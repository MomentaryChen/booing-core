import { useTranslation } from 'react-i18next'
import { useAuth } from '@/shared/hooks/useAuth'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Calendar, DollarSign, Star, TrendingUp, Clock } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useAuthStore } from '@/shared/stores/authStore'
import {
  fetchMerchantBookings,
  isMerchantBookingsApiError,
  type MerchantBookingRowDto,
} from '@/shared/lib/merchantBookingsApi'

const statusColors: Record<string, string> = {
  confirmed: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
  pending: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200',
}

export function DashboardPage() {
  const { t } = useTranslation(['merchant', 'common'])
  const { user } = useAuth()
  const merchantId = Number(useAuthStore((s) => s.user?.tenantId))
  const [bookings, setBookings] = useState<MerchantBookingRowDto[]>([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let mounted = true
    async function loadData() {
      if (!merchantId) {
        setLoading(false)
        return
      }
      setLoading(true)
      setError('')
      try {
        const data = await fetchMerchantBookings(merchantId)
        if (mounted) {
          setBookings(data)
        }
      } catch (e) {
        if (mounted) {
          setError(isMerchantBookingsApiError(e) ? e.message : t('common:errors.generic'))
        }
      } finally {
        if (mounted) {
          setLoading(false)
        }
      }
    }
    void loadData()
    return () => {
      mounted = false
    }
  }, [merchantId, t])

  const stats = useMemo(() => {
    const today = new Date().toDateString()
    const todayCount = bookings.filter((b) => new Date(b.startAt).toDateString() === today).length
    const confirmedCount = bookings.filter((b) => b.status.toLowerCase() === 'confirmed').length
    return [
      { key: 'totalBookings', value: String(bookings.length), icon: Calendar, change: '+0%' },
      { key: 'todayBookings', value: String(todayCount), icon: Clock, change: '+0' },
      { key: 'revenue', value: '$0', icon: DollarSign, change: '+0%' },
      { key: 'avgRating', value: String(confirmedCount), icon: Star, change: '+0.0' },
    ]
  }, [bookings])

  const upcomingBookings = useMemo(
    () =>
      bookings.slice(0, 6).map((b) => ({
        id: String(b.id),
        customer: b.customerName,
        service: t('bookings.serviceItem', { id: b.serviceItemId }),
        time: new Date(b.startAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        status: b.status.toLowerCase(),
      })),
    [bookings, t],
  )

  if (!merchantId) {
    return <p className="text-sm text-muted-foreground">{t('bookings.noMerchantContext')}</p>
  }

  return (
    <div className="space-y-8">
      {/* Welcome */}
      <div>
        <h1 className="text-3xl font-bold">{t('dashboard.title')}</h1>
        <p className="text-muted-foreground">
          {t('dashboard.welcome', { name: user?.name })}
        </p>
      </div>
      {loading ? <p className="text-sm text-muted-foreground">{t('common:status.loading')}</p> : null}
      {error ? <p className="text-sm text-destructive">{error}</p> : null}

      {/* Stats Grid */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {stats.map(({ key, value, icon: Icon, change }) => (
          <Card key={key}>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">
                {t(`dashboard.stats.${key}`)}
              </CardTitle>
              <Icon className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{value}</div>
              <div className="flex items-center gap-1 text-xs text-muted-foreground">
                <TrendingUp className="h-3 w-3 text-green-500" />
                <span className="text-green-500">{change}</span>
                <span>{t('dashboard.vsLastMonth')}</span>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Upcoming Bookings */}
      <Card>
        <CardHeader>
          <CardTitle>{t('dashboard.upcoming.title')}</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {upcomingBookings.map((booking) => (
              <div
                key={booking.id}
                className="flex items-center justify-between p-4 rounded-lg border"
              >
                <div className="space-y-1">
                  <p className="font-medium">{booking.customer}</p>
                  <p className="text-sm text-muted-foreground">{booking.service}</p>
                </div>
                <div className="flex items-center gap-4">
                  <span className="text-sm text-muted-foreground">{booking.time}</span>
                  <Badge className={statusColors[booking.status]}>
                    {t(`common:status.${booking.status}`)}
                  </Badge>
                </div>
              </div>
            ))}
            {!loading && upcomingBookings.length === 0 ? (
              <p className="text-sm text-muted-foreground">{t('bookings.tabs.all')} (0)</p>
            ) : null}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
