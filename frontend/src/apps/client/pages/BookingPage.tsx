import { useEffect, useMemo, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { format, parseISO } from 'date-fns'
import { AnimatePresence, motion } from 'framer-motion'
import {
  ArrowLeft,
  Calendar as CalendarIcon,
  Check,
  CheckCircle,
  ChevronRight,
  Clock,
  Loader2,
  Mail,
  Phone,
  RefreshCw,
  Star,
  User,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Calendar } from '@/components/ui/calendar'
import { Separator } from '@/components/ui/separator'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Checkbox } from '@/components/ui/checkbox'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { cn } from '@/shared/lib/utils'
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
import { AsyncStateBlock } from '@/apps/client/components/AsyncStateBlock'
import { PageHeader } from '@/apps/client/components/PageHeader'

type BookingMode = 'time' | 'resource_time' | 'session'
type BookingStep = 'resource' | 'datetime' | 'details'

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
  const [step, setStep] = useState<BookingStep>('resource')
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
    setStep('resource')
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

  const canProceedToDateTime = !!resource
  const canProceedToDetails = canProceedToDateTime && !!selectedDate && !!selectedStartAt
  const canSubmit =
    canProceedToDetails &&
    !submitting &&
    (!guestEnabled || (!!guestName.trim() && !!guestPhone.trim()))

  const slotSummary = useMemo(() => {
    if (!selectedStartAt) return null
    return format(parseISO(selectedStartAt), 'HH:mm')
  }, [selectedStartAt])

  const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: { staggerChildren: 0.08 },
    },
  }

  const itemVariants = {
    hidden: { opacity: 0, y: 12 },
    visible: { opacity: 1, y: 0 },
  }

  if (headerLoading) {
    return <AsyncStateBlock type="loading" description={t('common:status.loading')} />
  }

  if (headerError || !resource) {
    return (
      <div className="mx-auto w-full max-w-7xl space-y-6 px-4 py-6 md:py-8">
        <Button variant="ghost" className="mb-6 gap-2" onClick={() => navigate(-1)}>
          <ArrowLeft className="h-4 w-4" />
          {t('common:actions.back')}
        </Button>
        <AsyncStateBlock
          type="error"
          title={t('booking.resourceNotFound')}
          description={headerError || t('booking.resourceNotFound')}
        />
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
      <div className="mx-auto w-full max-w-7xl space-y-6 px-4 py-6 md:py-8">
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
    <div className="mx-auto w-full max-w-7xl space-y-6 px-4 py-6 md:py-8">
      <PageHeader
        title={resource.name}
        subtitle={service.provider}
        actions={
          <Button variant="ghost" className="gap-2" onClick={() => navigate(-1)}>
            <ArrowLeft className="h-4 w-4" />
            {t('common:actions.back')}
          </Button>
        }
      />

      <div className="grid gap-8 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <Card>
            <CardHeader>
              <div className="flex flex-wrap items-center gap-2">
                <Button
                  variant={step === 'resource' ? 'default' : 'outline'}
                  size="sm"
                  onClick={() => setStep('resource')}
                >
                  1. Resource
                </Button>
                <Button
                  variant={step === 'datetime' ? 'default' : 'outline'}
                  size="sm"
                  onClick={() => canProceedToDateTime && setStep('datetime')}
                  disabled={!canProceedToDateTime}
                >
                  2. Date & Time
                </Button>
                <Button
                  variant={step === 'details' ? 'default' : 'outline'}
                  size="sm"
                  onClick={() => canProceedToDetails && setStep('details')}
                  disabled={!canProceedToDetails}
                >
                  3. Details
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <AnimatePresence mode="wait">
                {step === 'resource' ? (
                  <motion.div
                    key="resource"
                    variants={containerVariants}
                    initial="hidden"
                    animate="visible"
                    exit={{ opacity: 0, x: -20 }}
                    className="space-y-4"
                  >
                    <motion.h2 variants={itemVariants} className="text-xl font-semibold">
                      {t('booking.selectResource')}
                    </motion.h2>
                    <motion.div variants={itemVariants}>
                      <div className="relative aspect-video overflow-hidden rounded-lg bg-muted">
                        <img
                          src={resolveDemoImageUrl(resource.imageUrl)}
                          alt={resource.name}
                          className="h-full w-full object-cover"
                        />
                      </div>
                    </motion.div>
                    <motion.div variants={itemVariants} className="rounded-lg border bg-card p-4">
                      <div className="mb-3 flex items-start justify-between">
                        <div>
                          <Badge variant="secondary" className="mb-2">
                            {service.category}
                          </Badge>
                          <h3 className="text-xl font-semibold">{service.name}</h3>
                          <p className="text-sm text-muted-foreground">{service.provider}</p>
                        </div>
                        <div className="text-right">
                          <div className="text-2xl font-bold">${service.price}</div>
                          <div className="flex items-center justify-end gap-1 text-sm text-muted-foreground">
                            <Star className="h-4 w-4 fill-primary text-primary" />
                            {service.rating}
                          </div>
                        </div>
                      </div>
                      <div className="mb-3 flex items-center gap-2 text-sm text-muted-foreground">
                        <Clock className="h-4 w-4" />
                        <span>{t('booking.durationMinutes', { count: service.duration })}</span>
                      </div>
                      <p className="text-sm text-muted-foreground">{service.description}</p>
                    </motion.div>
                    <motion.div variants={itemVariants}>
                      <Button onClick={() => setStep('datetime')} className="w-full sm:w-auto">
                        {t('booking.continueDateTime')}
                        <ChevronRight className="h-4 w-4" />
                      </Button>
                    </motion.div>
                  </motion.div>
                ) : null}

                {step === 'datetime' ? (
                  <motion.div
                    key="datetime"
                    variants={containerVariants}
                    initial="hidden"
                    animate="visible"
                    exit={{ opacity: 0, x: -20 }}
                    className="space-y-6"
                  >
                    <motion.div variants={itemVariants}>
                      <h2 className="mb-3 flex items-center gap-2 text-xl font-semibold">
                        <CalendarIcon className="h-5 w-5" />
                        {t('booking.selectDate')}
                      </h2>
                      <Calendar
                        mode="single"
                        selected={selectedDate}
                        onSelect={setSelectedDate}
                        disabled={(date) => date < new Date(new Date().setHours(0, 0, 0, 0))}
                        className="rounded-md border"
                      />
                    </motion.div>
                    <Separator />
                    <motion.div variants={itemVariants}>
                      <h2 className="mb-3 flex items-center gap-2 text-xl font-semibold">
                        <Clock className="h-5 w-5" />
                        {t('booking.selectTime')}
                      </h2>
                      {!selectedDate ? (
                        <p className="text-sm text-muted-foreground">{t('booking.selectDateFirst')}</p>
                      ) : slotsLoading ? (
                        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                      ) : slotsError ? (
                        <Alert variant="destructive">
                          <AlertDescription className="space-y-3">
                            <p>{slotsError}</p>
                            <Button
                              type="button"
                              variant="outline"
                              size="sm"
                              className="gap-2"
                              onClick={() => setAvailabilityRefreshKey((k) => k + 1)}
                            >
                              <RefreshCw className="h-3 w-3" />
                              {t('common:actions.retry')}
                            </Button>
                          </AlertDescription>
                        </Alert>
                      ) : slots.length === 0 ? (
                        <p className="text-sm text-muted-foreground">{t('booking.noSlots')}</p>
                      ) : (
                        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4">
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
                    </motion.div>
                    <motion.div variants={itemVariants}>
                      <Button onClick={() => setStep('details')} disabled={!canProceedToDetails} className="w-full">
                        {t('booking.continueDetails')}
                      </Button>
                    </motion.div>
                  </motion.div>
                ) : null}

                {step === 'details' ? (
                  <motion.div
                    key="details"
                    variants={containerVariants}
                    initial="hidden"
                    animate="visible"
                    exit={{ opacity: 0, x: -20 }}
                    className="space-y-4"
                  >
                    <motion.h2 variants={itemVariants} className="text-xl font-semibold">
                      {t('booking.detailsTitle')}
                    </motion.h2>
                    <motion.div variants={itemVariants} className="space-y-2">
                      <Label htmlFor="booking-mode">{t('booking.mode.label')}</Label>
                      <Select value={bookingMode} onValueChange={(value) => setBookingMode(value as BookingMode)}>
                        <SelectTrigger id="booking-mode">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="time">{t('booking.mode.time')}</SelectItem>
                          <SelectItem value="resource_time">{t('booking.mode.resourceTime')}</SelectItem>
                          <SelectItem value="session">{t('booking.mode.session')}</SelectItem>
                        </SelectContent>
                      </Select>
                      <p className="text-xs text-muted-foreground">{t(`booking.mode.hint.${bookingMode}`)}</p>
                    </motion.div>
                    <motion.div variants={itemVariants} className="space-y-2 rounded-md border p-3">
                      <label htmlFor="guest-enabled" className="flex items-center gap-2 text-sm font-medium">
                        <Checkbox
                          id="guest-enabled"
                          checked={guestEnabled}
                          onCheckedChange={(checked) => setGuestEnabled(checked === true)}
                        />
                        {t('booking.guest.enable')}
                      </label>
                      {guestEnabled ? (
                        <div className="grid gap-2">
                          <div className="relative">
                            <User className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                            <Input
                              value={guestName}
                              onChange={(e) => setGuestName(e.target.value)}
                              placeholder={t('booking.guest.name')}
                              className="pl-10"
                            />
                          </div>
                          <div className="relative">
                            <Phone className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                            <Input
                              value={guestPhone}
                              onChange={(e) => setGuestPhone(e.target.value)}
                              placeholder={t('booking.guest.phone')}
                              className="pl-10"
                            />
                          </div>
                          <div className="relative">
                            <Mail className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                            <Input
                              value={guestEmail}
                              onChange={(e) => setGuestEmail(e.target.value)}
                              placeholder={t('booking.guest.email')}
                              className="pl-10"
                            />
                          </div>
                        </div>
                      ) : null}
                    </motion.div>
                    <motion.div variants={itemVariants} className="space-y-2">
                      <Label htmlFor="notes">{t('booking.notes')}</Label>
                      <Textarea
                        id="notes"
                        placeholder={t('booking.notesPlaceholder')}
                        value={notes}
                        onChange={(e) => setNotes(e.target.value)}
                      />
                    </motion.div>
                    {submitError ? (
                      <motion.div variants={itemVariants}>
                        <Alert variant="destructive">
                          <AlertDescription>{submitError}</AlertDescription>
                        </Alert>
                      </motion.div>
                    ) : null}
                    <motion.div variants={itemVariants}>
                      <Button className="w-full" size="lg" disabled={!canSubmit} onClick={() => void handleBooking()}>
                        {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : t('booking.confirm')}
                      </Button>
                    </motion.div>
                  </motion.div>
                ) : null}
              </AnimatePresence>
            </CardContent>
          </Card>
        </div>

        <div className="lg:col-span-1">
          <motion.div initial={{ opacity: 0, x: 16 }} animate={{ opacity: 1, x: 0 }} className="sticky top-24">
            <Card className="shadow-xl">
              <CardHeader>
                <CardTitle>{t('booking.summary')}</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <div className="relative h-32 w-full overflow-hidden rounded-lg">
                    <img
                      src={resolveDemoImageUrl(resource.imageUrl)}
                      alt={resource.name}
                      className="h-full w-full object-cover"
                    />
                  </div>
                  <h3 className="font-semibold">{service.name}</h3>
                  <p className="text-sm text-muted-foreground">{service.description}</p>
                </div>
                <Separator />
                <div className="space-y-3 text-sm">
                  {selectedDate ? (
                    <div className="flex items-center gap-2">
                      <CalendarIcon className="h-4 w-4 text-muted-foreground" />
                      <span>{format(selectedDate, 'yyyy/MM/dd')}</span>
                    </div>
                  ) : null}
                  {slotSummary ? (
                    <div className="flex items-center gap-2">
                      <Clock className="h-4 w-4 text-muted-foreground" />
                      <span>{slotSummary}</span>
                    </div>
                  ) : null}
                  <div className="flex items-center gap-2">
                    <Check
                      className={cn(
                        'h-4 w-4',
                        canProceedToDetails ? 'text-primary' : 'text-muted-foreground',
                      )}
                    />
                    <span className="text-muted-foreground">
                      {canProceedToDetails
                        ? t('booking.readyForSubmit')
                        : t('booking.selectDateTimeFirst')}
                    </span>
                  </div>
                </div>
                <Separator />
                <div className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">{t('booking.total')}</span>
                    <span className="font-semibold">${service.price}</span>
                  </div>
                  <div className="text-xs text-muted-foreground">
                    {t('booking.durationMinutes', { count: service.duration })}
                  </div>
                </div>
              </CardContent>
            </Card>
          </motion.div>
        </div>
      </div>
    </div>
  )
}
