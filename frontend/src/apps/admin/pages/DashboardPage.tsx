import { useTranslation } from 'react-i18next'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Users, Building2, Calendar, Activity, TrendingUp } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import {
  fetchSystemCommandCenter,
  fetchSystemOverview,
  fetchSystemUsers,
  isSystemAdminApiError,
} from '@/shared/lib/systemAdminApi'

export function DashboardPage() {
  const { t } = useTranslation(['admin', 'common'])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [overview, setOverview] = useState({
    totalMerchants: 0,
    activeMerchants: 0,
    totalBookings: 0,
    domainTemplates: 0,
  })
  const [commandCenter, setCommandCenter] = useState<{
    pendingActions?: number
    occupancyRate?: number
    liveFeed?: Array<{
      bookingId: number
      merchantName: string
      customerName: string
      status: string
      startAt: string
    }>
  }>({})
  const [usersCount, setUsersCount] = useState(0)

  useEffect(() => {
    let mounted = true
    async function loadData() {
      setLoading(true)
      setError(null)
      try {
        const [overviewData, commandCenterData, usersData] = await Promise.all([
          fetchSystemOverview(),
          fetchSystemCommandCenter(),
          fetchSystemUsers(),
        ])
        if (!mounted) {
          return
        }
        setOverview(overviewData)
        setCommandCenter(commandCenterData)
        setUsersCount(usersData.length)
      } catch (e) {
        if (!mounted) {
          return
        }
        if (isSystemAdminApiError(e)) {
          setError(e.message)
        } else {
          setError(t('common:errors.generic'))
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
  }, [t])

  const stats = useMemo(
    () => [
      {
        key: 'totalUsers',
        value: String(usersCount),
        icon: Users,
        change: '+0%',
      },
      {
        key: 'totalTenants',
        value: String(overview.totalMerchants),
        icon: Building2,
        change: '+0%',
      },
      {
        key: 'totalBookings',
        value: String(overview.totalBookings),
        icon: Calendar,
        change: '+0%',
      },
      {
        key: 'systemHealth',
        value: `${commandCenter.occupancyRate ?? 0}%`,
        icon: Activity,
        change: 'healthy',
      },
    ],
    [commandCenter.occupancyRate, overview.totalBookings, overview.totalMerchants, usersCount],
  )

  const recentActivity = useMemo(
    () =>
      (commandCenter.liveFeed ?? []).slice(0, 5).map((item) => ({
        id: String(item.bookingId),
        actionKey: 'bookingCompleted',
        user: item.customerName || item.merchantName || 'system',
        timeKey: 'minutesAgo',
        timeValue: 5,
      })),
    [commandCenter.liveFeed],
  )

  return (
    <div className="space-y-8">
      <h1 className="text-3xl font-bold">{t('dashboard.title')}</h1>
      {loading && <p className="text-sm text-muted-foreground">{t('common:status.loading')}</p>}
      {error && <p className="text-sm text-destructive">{error}</p>}

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
                {change.includes('%') && <TrendingUp className="h-3 w-3 text-green-500" />}
                <span className={change.includes('%') ? 'text-green-500' : ''}>
                  {change.includes('%') ? change : t(`dashboard.systemHealth.${change}`)}
                </span>
                {change.includes('%') && <span>{t('dashboard.vsLastMonth')}</span>}
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Charts */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <Card className="col-span-2">
          <CardHeader>
            <CardTitle>{t('dashboard.charts.userGrowth')}</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-[300px] flex items-center justify-center text-muted-foreground border rounded-lg">
              {t('dashboard.charts.userGrowthPlaceholder')}
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>{t('dashboard.charts.bookingTrends')}</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-[300px] flex items-center justify-center text-muted-foreground border rounded-lg">
              {t('dashboard.charts.bookingTrendsPlaceholder')}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Recent Activity */}
      <Card>
        <CardHeader>
          <CardTitle>{t('dashboard.recentActivity.title')}</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {recentActivity.map((activity) => (
              <div
                key={activity.id}
                className="flex items-center justify-between border-b pb-4 last:border-0 last:pb-0"
              >
                <div>
                  <p className="font-medium">{t(`dashboard.recentActivity.actions.${activity.actionKey}`)}</p>
                  <p className="text-sm text-muted-foreground">{t('dashboard.recentActivity.byUser', { user: activity.user })}</p>
                </div>
                <span className="text-sm text-muted-foreground">{t(`dashboard.recentActivity.${activity.timeKey}`, { count: activity.timeValue })}</span>
              </div>
            ))}
            {recentActivity.length === 0 && !loading && (
              <p className="text-sm text-muted-foreground">{t('admin:auditLogs.totalLogs', { count: 0 })}</p>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
