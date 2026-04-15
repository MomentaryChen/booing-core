import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { format, parseISO } from 'date-fns'
import { Clock, Star, ArrowLeft, CheckCircle, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Calendar } from '@/components/ui/calendar'
import { Separator } from '@/components/ui/separator'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  fetchClientResourceDetail,
  isClientCatalogApiError,
} from '@/shared/lib/clientCatalogApi'
import { resolveDemoImageUrl } from '@/shared/lib/demoMedia'
import {
  createClientBooking,
  fetchResourceAvailability,
  isClientBookingApiError,
  type ClientResourceAvailabilitySlotDto,
} from '@/shared/lib/clientBookingApi'

type BookingMode = 'time' | 'resource_time' | 'session'

const CLIENT_RESOURCE_ID_RE =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

function isClientResourceIdParam(value: string | undefined): value is string {
  return !!value && CLIENT_RESOURCE_ID_RE.test(value)
}

export function BookingPage() {
  const { resourceId: resourceIdParam } = useParams()
  const navigate = useNavigate()
  const { t } = useTranslation(['client', 'common'])

  const [resource, setResource] = useState<{
    id: string
    name: string
    category: string
    price: number
    durationMinutes: number
    rating: number
    merchantName: string
    imageUrl: string | null
  } | null>(null)
  const [merchantName, setMerchantName] = useState<string>('')
  const [headerError, setHeaderError] = useState('')
  const [headerLoading, setHeaderLoading] = useState(true)

  const [selectedDate, setSelectedDate] = useState<Date | undefined>(undefined)
  const [slots, setSlots] = useState<ClientResourceAvailabilitySlotDto[]>([])
  const [slotsLoading, setSlotsLoading] = useState(false)
  const [slotsError, setSlotsError] = useState('')
  const [selectedStartAt, setSelectedStartAt] = useState<string | null>(null)
  const [bookingMode, setBookingMode] = useState<BookingMode>('time')
  const [guestEnabled, setGuestEnabled] = useState(false)
  const [guestName, setGuestName] = useState('')
  const [guestPhone, setGuestPhone] = useState('')
  const [guestEmail, setGuestEmail] = useState('')
  const [notes, setNotes] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')
  const [isBooked, setIsBooked] = useState(false)
  /** Bumped after slot conflict so availability is re-fetched (stale slot list). */
  const [availabilityRefreshKey, setAvailabilityRefreshKey] = useState(0)

  useEffect(() => {
    setIsBooked(false)
    setSelectedDate(undefined)
    setSlots([])
    setSelectedStartAt(null)
    setSlotsError('')
    setSubmitError('')
    setNotes('')
    setGuestEnabled(false)
    setGuestName('')
    setGuestPhone('')
    setGuestEmail('')
  }, [resourceIdParam])

  useEffect(() => {
    if (!isClientResourceIdParam(resourceIdParam)) {
      setHeaderError(t('booking.invalidResource'))
      setHeaderLoading(false)
      return
    }

    let cancelled = false
    const run = async () => {
      setHeaderLoading(true)
      setHeaderError('')
      try {
        const detail = await fetchClientResourceDetail(resourceIdParam)
        if (!cancelled) {
          setResource({
            id: detail.id,
            name: detail.name,
            category: detail.category,
            price: detail.price,
            durationMinutes: detail.durationMinutes,
            rating: detail.rating,
            merchantName: detail.merchant.name,
            imageUrl: detail.imageUrl,
          })
          setMerchantName(detail.merchant.name)
        }
      } catch (e) {
        if (!cancelled) {
          setHeaderError(isClientCatalogApiError(e) ? e.message : t('common:errors.generic'))
          setResource(null)
        }
      } finally {
        if (!cancelled) setHeaderLoading(false)
      }
    }
    void run()
    return () => {
      cancelled = true
    }
  }, [resourceIdParam, t])

  useEffect(() => {
    if (!selectedDate || !isClientResourceIdParam(resourceIdParam)) {
      setSlots([])
      setSelectedStartAt(null)
      return
    }
    const dateStr = format(selectedDate, 'yyyy-MM-dd')
    let cancelled = false
    const run = async () => {
      setSlotsLoading(true)
      setSlotsError('')
      setSelectedStartAt(null)
      try {
        const res = await fetchResourceAvailability(resourceIdParam, dateStr)
        if (!cancelled) {
          setSlots(res.slots.filter((s) => s.isAvailable))
        }
      } catch (e) {
        if (!cancelled) {
          setSlots([])
          setSlotsError(isClientBookingApiError(e) ? e.message : t('common:errors.generic'))
        }
      } finally {
        if (!cancelled) setSlotsLoading(false)
      }
    }
    void run()
    return () => {
      cancelled = true
    }
  }, [selectedDate, resourceIdParam, t, availabilityRefreshKey])

  const handleBooking = async () => {
    if (!resource || !selectedStartAt) return
    if (guestEnabled && (!guestName.trim() || !guestPhone.trim())) {
      setSubmitError(t('booking.guest.required'))
      return
    }
    setSubmitting(true)
    setSubmitError('')
    try {
      await createClientBooking({
        resourceId: resource.id,
        startAt: selectedStartAt,
        notes: notes.trim() || undefined,
        mode: bookingMode,
        guest: guestEnabled
          ? {
              name: guestName.trim(),
              phone: guestPhone.trim(),
              email: guestEmail.trim() || undefined,
            }
          : undefined,
      })
      setIsBooked(true)
    } catch (e) {
      let msg = t('common:errors.generic')
      if (isClientBookingApiError(e)) {
        const slotConflict =
          e.errorCode === 'BOOKING_SLOT_CONFLICT' ||
          (e.status === 409 && !e.errorCode)
        if (slotConflict) {
          msg = t('booking.conflict')
          setAvailabilityRefreshKey((k) => k + 1)
        } else {
          msg = e.message
        }
      }
      setSubmitError(msg)
    } finally {
      setSubmitting(false)
    }
  }

  if (headerLoading) {
    return (
      <div className="flex justify-center py-24">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (headerError || !resource) {
    return (
      <div className="container mx-auto px-4 py-8">
        <Button variant="ghost" className="mb-6 gap-2" onClick={() => navigate(-1)}>
          <ArrowLeft className="h-4 w-4" />
          {t('common:actions.back')}
        </Button>
        <p className="text-destructive">{headerError || t('booking.resourceNotFound')}</p>
      </div>
    )
  }

  const service = {
    name: resource.name,
    description: resource.category,
    category: resource.category,
    price: Number(resource.price),
    duration: resource.durationMinutes,
    rating: resource.rating,
    provider: merchantName || t('booking.unknownMerchant'),
  }

  if (isBooked) {
    return (
      <div className="container mx-auto px-4 py-8">
        <Card className="mx-auto max-w-md">
          <CardContent className="pt-6 text-center">
            <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-primary/10">
              <CheckCircle className="h-8 w-8 text-primary" />
            </div>
            <CardTitle className="mb-2 text-2xl">{t('booking.success')}</CardTitle>
            <CardDescription className="mb-6">
              {service.name}
              {selectedDate ? ` · ${format(selectedDate, 'MMMM d, yyyy')}` : ''}
            </CardDescription>
            <div className="flex justify-center gap-2">
              <Button variant="outline" onClick={() => navigate('/my-bookings')}>
                {t('common:nav.bookings')}
              </Button>
              <Button onClick={() => navigate('/search')}>{t('common:nav.search')}</Button>
            </div>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <Button variant="ghost" className="mb-6 gap-2" onClick={() => navigate(-1)}>
        <ArrowLeft className="h-4 w-4" />
        {t('common:actions.back')}
      </Button>

      <div className="grid gap-8 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <Card>
            <CardHeader className="p-0">
              <div className="relative aspect-video overflow-hidden rounded-t-lg bg-muted">
                <img
                  src={resolveDemoImageUrl(resource.imageUrl)}
                  alt=""
                  className="h-full w-full object-cover"
                />
              </div>
            </CardHeader>
            <CardContent className="p-6">
              <div className="mb-4 flex items-start justify-between">
                <div>
                  <Badge variant="secondary" className="mb-2">
                    {service.category}
                  </Badge>
                  <CardTitle className="text-2xl">{service.name}</CardTitle>
                  <p className="text-muted-foreground">{service.provider}</p>
                </div>
                <div className="text-right">
                  <div className="text-2xl font-bold">${service.price}</div>
                  <div className="flex items-center gap-1 text-sm text-muted-foreground">
                    <Star className="h-4 w-4 fill-primary text-primary" />
                    {service.rating}
                  </div>
                </div>
              </div>
              <div className="mb-4 flex items-center gap-2 text-muted-foreground">
                <Clock className="h-4 w-4" />
                <span>{t('booking.durationMinutes', { count: service.duration })}</span>
              </div>
              <Separator className="my-4" />
              <p className="text-muted-foreground">{service.description}</p>
            </CardContent>
          </Card>
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>{t('booking.selectDate')}</CardTitle>
            </CardHeader>
            <CardContent>
              <Calendar
                mode="single"
                selected={selectedDate}
                onSelect={setSelectedDate}
                disabled={(date) => date < new Date(new Date().setHours(0, 0, 0, 0))}
                className="rounded-md border"
              />
            </CardContent>
          </Card>

          {selectedDate && (
            <Card>
              <CardHeader>
                <CardTitle>{t('booking.selectTime')}</CardTitle>
              </CardHeader>
              <CardContent>
                {slotsLoading ? (
                  <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                ) : slotsError ? (
                  <p className="text-sm text-destructive">{slotsError}</p>
                ) : slots.length === 0 ? (
                  <p className="text-sm text-muted-foreground">{t('booking.noSlots')}</p>
                ) : (
                  <div className="grid grid-cols-3 gap-2">
                    {slots.map((slot) => {
                      const label = format(parseISO(slot.startAt), 'HH:mm')
                      const active = selectedStartAt === slot.startAt
                      return (
                        <Button
                          key={slot.startAt}
                          type="button"
                          variant={active ? 'default' : 'outline'}
                          onClick={() => setSelectedStartAt(slot.startAt)}
                          className="w-full"
                        >
                          {label}
                        </Button>
                      )
                    })}
                  </div>
                )}
              </CardContent>
            </Card>
          )}

          {selectedStartAt && (
            <Card>
              <CardHeader>
                <CardTitle>{t('booking.notes')}</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="booking-mode">{t('booking.mode.label')}</Label>
                  <select
                    id="booking-mode"
                    value={bookingMode}
                    onChange={(e) => setBookingMode(e.target.value as BookingMode)}
                    className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
                  >
                    <option value="time">{t('booking.mode.time')}</option>
                    <option value="resource_time">{t('booking.mode.resourceTime')}</option>
                    <option value="session">{t('booking.mode.session')}</option>
                  </select>
                  <p className="text-xs text-muted-foreground">{t(`booking.mode.hint.${bookingMode}`)}</p>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="notes">{t('booking.notes')}</Label>
                  <Input
                    id="notes"
                    placeholder={t('booking.notesPlaceholder')}
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                  />
                </div>
                <div className="space-y-3 rounded-md border p-3">
                  <label className="flex items-center gap-2 text-sm font-medium">
                    <input
                      type="checkbox"
                      checked={guestEnabled}
                      onChange={(e) => setGuestEnabled(e.target.checked)}
                    />
                    {t('booking.guest.enable')}
                  </label>
                  {guestEnabled ? (
                    <div className="grid gap-2">
                      <Input
                        value={guestName}
                        onChange={(e) => setGuestName(e.target.value)}
                        placeholder={t('booking.guest.name')}
                      />
                      <Input
                        value={guestPhone}
                        onChange={(e) => setGuestPhone(e.target.value)}
                        placeholder={t('booking.guest.phone')}
                      />
                      <Input
                        value={guestEmail}
                        onChange={(e) => setGuestEmail(e.target.value)}
                        placeholder={t('booking.guest.email')}
                      />
                    </div>
                  ) : null}
                </div>
                <Separator />
                <div className="flex items-center justify-between">
                  <span className="text-muted-foreground">{t('booking.total')}</span>
                  <span className="text-2xl font-bold">${service.price}</span>
                </div>
                {submitError ? <p className="text-sm text-destructive">{submitError}</p> : null}
                <Button className="w-full" size="lg" disabled={submitting} onClick={() => void handleBooking()}>
                  {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : t('booking.confirm')}
                </Button>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  )
}
