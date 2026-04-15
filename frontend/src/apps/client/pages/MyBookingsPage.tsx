import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Calendar, Clock, Search } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  cancelClientBooking,
  fetchClientBookings,
  isClientBookingApiError,
  rescheduleClientBooking,
  type ClientBookingListItemDto,
  type ClientBookingTab,
} from '@/shared/lib/clientBookingApi'

const statusColors: Record<string, string> = {
  confirmed: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
  pending: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200',
  completed: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200',
  cancelled: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200',
}

interface BookingCardModel {
  id: string
  serviceName: string
  provider: string
  date: string
  time: string
  duration: number
  status: string
  price: number
}

function mapDto(dto: ClientBookingListItemDto): BookingCardModel {
  return {
    id: String(dto.id),
    serviceName: dto.serviceName,
    provider: dto.providerName,
    date: dto.date,
    time: dto.time,
    duration: dto.durationMinutes,
    status: dto.status.toLowerCase(),
    price: typeof dto.price === 'number' ? dto.price : Number(dto.price),
  }
}

function EmptyState() {
  const { t } = useTranslation(['client', 'common'])

  return (
    <div className="py-12 text-center">
      <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-muted">
        <Calendar className="h-8 w-8 text-muted-foreground" />
      </div>
      <h3 className="mb-2 text-lg font-semibold">{t('myBookings.empty.title')}</h3>
      <p className="mb-6 text-muted-foreground">{t('myBookings.empty.subtitle')}</p>
      <Link to="/search">
        <Button className="gap-2">
          <Search className="h-4 w-4" />
          {t('myBookings.empty.cta')}
        </Button>
      </Link>
    </div>
  )
}

export function MyBookingsPage() {
  const { t } = useTranslation(['client', 'common'])
  const [tab, setTab] = useState<ClientBookingTab>('upcoming')
  const [lists, setLists] = useState<Record<ClientBookingTab, BookingCardModel[]>>({ upcoming: [], past: [], cancelled: [] })
  const [counts, setCounts] = useState<Record<ClientBookingTab, number>>({ upcoming: 0, past: 0, cancelled: 0 })
  const [pages, setPages] = useState<Record<ClientBookingTab, number>>({ upcoming: 0, past: 0, cancelled: 0 })
  const [loadingByTab, setLoadingByTab] = useState<Record<ClientBookingTab, boolean>>({
    upcoming: true,
    past: true,
    cancelled: true,
  })
  const [jumpPageByTab, setJumpPageByTab] = useState<Record<ClientBookingTab, string>>({
    upcoming: '1',
    past: '1',
    cancelled: '1',
  })
  const [error, setError] = useState<string | null>(null)
  const [pageSize, setPageSize] = useState(10)
  const [cancellingBookingId, setCancellingBookingId] = useState<string | null>(null)

  const loadTab = useCallback(async (targetTab: ClientBookingTab, targetPage: number) => {
    setLoadingByTab((prev) => ({ ...prev, [targetTab]: true }))
    try {
      const response = await fetchClientBookings(targetTab, targetPage, pageSize)
      setLists((prev) => ({ ...prev, [targetTab]: response.items.map(mapDto) }))
      setCounts((prev) => ({ ...prev, [targetTab]: response.total }))
      setPages((prev) => ({ ...prev, [targetTab]: response.page }))
      setJumpPageByTab((prev) => ({ ...prev, [targetTab]: String(response.page + 1) }))
    } catch (e) {
      if (isClientBookingApiError(e) && e.status === 401) {
        setError(t('myBookings.errors.signInRequired'))
      } else if (isClientBookingApiError(e) && e.status === 403) {
        setError(t('myBookings.errors.forbidden'))
      } else {
        setError(t('myBookings.errors.loadFailed'))
      }
    } finally {
      setLoadingByTab((prev) => ({ ...prev, [targetTab]: false }))
    }
  }, [pageSize, t])

  const loadAllFirstPages = useCallback(async () => {
    setError(null)
    setPages({ upcoming: 0, past: 0, cancelled: 0 })
    setJumpPageByTab({ upcoming: '1', past: '1', cancelled: '1' })
    await Promise.all([
      loadTab('upcoming', 0),
      loadTab('past', 0),
      loadTab('cancelled', 0),
    ])
  }, [loadTab])

  useEffect(() => {
    void loadAllFirstPages()
  }, [loadAllFirstPages, pageSize])

  const handleCancel = useCallback(
    async (bookingId: string) => {
      const confirmed = window.confirm(t('myBookings.actions.cancel'))
      if (!confirmed) return
      setError(null)
      setCancellingBookingId(bookingId)
      try {
        await cancelClientBooking(bookingId, 'client-request')
        await loadAllFirstPages()
      } catch (e) {
        if (isClientBookingApiError(e) && e.status === 409) {
          setError(t('myBookings.errors.cancelConflict'))
        } else {
          setError(t('myBookings.errors.cancelFailed'))
        }
      } finally {
        setCancellingBookingId(null)
      }
    },
    [loadAllFirstPages, t],
  )

  const handleReschedule = useCallback(
    async (bookingId: string) => {
      const next = window.prompt('New start time (ISO), e.g. 2026-04-20T10:00:00')
      if (!next) return
      setError(null)
      try {
        await rescheduleClientBooking(bookingId, next, 'client-reschedule')
        await loadTab(tab, pages[tab])
      } catch (e) {
        setError(isClientBookingApiError(e) ? e.message : t('myBookings.errors.loadFailed'))
      }
    },
    [loadTab, pages, tab, t],
  )

  const handleJumpPage = useCallback(
    async (key: ClientBookingTab) => {
      const totalPages = Math.max(1, Math.ceil(counts[key] / pageSize))
      const parsed = Number(jumpPageByTab[key])
      if (!Number.isInteger(parsed)) {
        setJumpPageByTab((prev) => ({ ...prev, [key]: String(pages[key] + 1) }))
        return
      }
      const clamped = Math.min(Math.max(parsed, 1), totalPages)
      setJumpPageByTab((prev) => ({ ...prev, [key]: String(clamped) }))
      await loadTab(key, clamped - 1)
    },
    [counts, jumpPageByTab, loadTab, pageSize, pages],
  )

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="mb-8 text-3xl font-bold">{t('myBookings.title')}</h1>

      {error && (
        <p className="mb-4 rounded-md border border-destructive/50 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {error}
        </p>
      )}

      <Tabs value={tab} onValueChange={(v) => setTab(v as ClientBookingTab)} className="w-full">
        <TabsList className="mb-6">
          <TabsTrigger value="upcoming">
            {t('myBookings.tabs.upcoming')} ({counts.upcoming})
          </TabsTrigger>
          <TabsTrigger value="past">
            {t('myBookings.tabs.past')} ({counts.past})
          </TabsTrigger>
          <TabsTrigger value="cancelled">
            {t('myBookings.tabs.cancelled')} ({counts.cancelled})
          </TabsTrigger>
        </TabsList>

        {(['upcoming', 'past', 'cancelled'] as const).map((key) => (
          <TabsContent key={key} value={key}>
            {loadingByTab[key] ? (
              <p className="text-center text-muted-foreground">{t('common:status.loading')}</p>
            ) : counts[key] > 0 ? (
              <div className="space-y-4">
                {lists[key].length === 0 ? (
                  <p className="py-6 text-center text-sm text-muted-foreground">
                    {t('myBookings.emptyPage')}
                  </p>
                ) : null}
                {lists[key].map((booking) => (
                  <Card key={booking.id}>
                    <CardContent className="p-4">
                      <div className="flex items-start justify-between">
                        <div className="space-y-1">
                          <h3 className="font-semibold">{booking.serviceName}</h3>
                          <p className="text-sm text-muted-foreground">{booking.provider}</p>
                        </div>
                        <Badge className={statusColors[booking.status] ?? statusColors.pending}>
                          {t(`common:status.${booking.status}`, { defaultValue: booking.status })}
                        </Badge>
                      </div>
                      <div className="mt-4 flex flex-wrap gap-4 text-sm text-muted-foreground">
                        <span className="flex items-center gap-1">
                          <Calendar className="h-4 w-4" />
                          {booking.date}
                        </span>
                        <span className="flex items-center gap-1">
                          <Clock className="h-4 w-4" />
                          {booking.time} ({booking.duration} min)
                        </span>
                      </div>
                      <div className="mt-4 flex items-center justify-between">
                        <span className="font-semibold">${booking.price}</span>
                        {key === 'upcoming' ? (
                          <div className="flex gap-2">
                            <Button variant="outline" size="sm" onClick={() => void handleReschedule(booking.id)}>
                              {t('myBookings.actions.reschedule')}
                            </Button>
                            <Button
                              variant="outline"
                              size="sm"
                              className="text-destructive"
                              disabled={cancellingBookingId === booking.id}
                              onClick={() => void handleCancel(booking.id)}
                            >
                              {t('myBookings.actions.cancel')}
                            </Button>
                          </div>
                        ) : null}
                      </div>
                    </CardContent>
                  </Card>
                ))}
                <div className="flex items-center justify-end gap-2 pt-2">
                  <select
                    className="h-8 rounded-md border bg-background px-2 text-sm"
                    value={String(pageSize)}
                    onChange={(e) => setPageSize(Number(e.target.value))}
                  >
                    <option value="10">10 / page</option>
                    <option value="20">20 / page</option>
                    <option value="50">50 / page</option>
                  </select>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={pages[key] <= 0}
                    onClick={() => void loadTab(key, Math.max(0, pages[key] - 1))}
                  >
                    {t('common:actions.previous', { defaultValue: 'Previous' })}
                  </Button>
                  <span className="text-sm text-muted-foreground">
                    {pages[key] + 1} / {Math.max(1, Math.ceil(counts[key] / pageSize))}
                  </span>
                  <input
                    className="h-8 w-14 rounded-md border bg-background px-2 text-sm"
                    value={jumpPageByTab[key]}
                    onChange={(e) =>
                      setJumpPageByTab((prev) => ({ ...prev, [key]: e.target.value }))
                    }
                  />
                  <Button variant="outline" size="sm" onClick={() => void handleJumpPage(key)}>
                    {t('common:actions.go', { defaultValue: 'Go' })}
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={pages[key] + 1 >= Math.max(1, Math.ceil(counts[key] / pageSize))}
                    onClick={() => void loadTab(key, pages[key] + 1)}
                  >
                    {t('common:actions.next', { defaultValue: 'Next' })}
                  </Button>
                </div>
              </div>
            ) : (
              <EmptyState />
            )}
          </TabsContent>
        ))}
      </Tabs>
    </div>
  )
}
