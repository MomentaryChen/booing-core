import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Calendar, Clock, Search } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  fetchClientBookings,
  isClientBookingApiError,
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

function BookingCard({ booking, showActions = false }: { booking: BookingCardModel; showActions?: boolean }) {
  const { t } = useTranslation(['client', 'common'])

  return (
    <Card>
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
          {showActions && (
            <div className="flex gap-2">
              <Button variant="outline" size="sm">
                {t('myBookings.actions.reschedule')}
              </Button>
              <Button variant="outline" size="sm" className="text-destructive">
                {t('myBookings.actions.cancel')}
              </Button>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  )
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
  const [lists, setLists] = useState<Record<ClientBookingTab, BookingCardModel[]>>({
    upcoming: [],
    past: [],
    cancelled: [],
  })
  const [counts, setCounts] = useState<Record<ClientBookingTab, number>>({
    upcoming: 0,
    past: 0,
    cancelled: 0,
  })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    async function loadAll() {
      setLoading(true)
      setError(null)
      try {
        const [u, p, c] = await Promise.all([
          fetchClientBookings('upcoming', 0, 50),
          fetchClientBookings('past', 0, 50),
          fetchClientBookings('cancelled', 0, 50),
        ])
        if (cancelled) {
          return
        }
        setLists({
          upcoming: u.items.map(mapDto),
          past: p.items.map(mapDto),
          cancelled: c.items.map(mapDto),
        })
        setCounts({
          upcoming: u.total,
          past: p.total,
          cancelled: c.total,
        })
      } catch (e) {
        if (cancelled) {
          return
        }
        if (isClientBookingApiError(e) && e.status === 401) {
          setError(t('myBookings.errors.signInRequired'))
        } else if (isClientBookingApiError(e) && e.status === 403) {
          setError(t('myBookings.errors.forbidden'))
        } else {
          setError(t('myBookings.errors.loadFailed'))
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }
    void loadAll()
    return () => {
      cancelled = true
    }
  }, [t])

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
            {loading ? (
              <p className="text-center text-muted-foreground">{t('common:status.loading')}</p>
            ) : lists[key].length > 0 ? (
              <div className="space-y-4">
                {lists[key].map((booking) => (
                  <BookingCard key={booking.id} booking={booking} showActions={key === 'upcoming'} />
                ))}
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
