import { useCallback, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Search, MoreHorizontal, Check, X, Phone, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { useAuthStore } from '@/shared/stores/authStore'
import {
  fetchMerchantBookings,
  isMerchantBookingsApiError,
  putMerchantBookingStatus,
  type MerchantBookingRowDto,
} from '@/shared/lib/merchantBookingsApi'

const statusColors: Record<string, string> = {
  pending: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200',
  confirmed: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
  completed: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200',
  cancelled: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200',
  checked_in: 'bg-slate-100 text-slate-800 dark:bg-slate-900 dark:text-slate-200',
  in_service: 'bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200',
}

function normalizeStatus(s: string): string {
  return s.toLowerCase()
}

function formatDateTime(iso: string): { date: string; time: string } {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return { date: iso, time: '' }
  return {
    date: d.toLocaleDateString(),
    time: d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' }),
  }
}

export function BookingsPage() {
  const { t } = useTranslation(['merchant', 'common'])
  const merchantId = Number(useAuthStore((s) => s.user?.tenantId))
  const [bookings, setBookings] = useState<MerchantBookingRowDto[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [activeTab, setActiveTab] = useState('all')
  const [actionError, setActionError] = useState('')

  const reload = useCallback(async () => {
    if (!merchantId) return
    setLoading(true)
    setLoadError('')
    try {
      const list = await fetchMerchantBookings(merchantId)
      setBookings(list)
    } catch (e) {
      setLoadError(isMerchantBookingsApiError(e) ? e.message : t('common:errors.generic'))
    } finally {
      setLoading(false)
    }
  }, [merchantId, t])

  useEffect(() => {
    void reload()
  }, [reload])

  const filteredBookings = useMemo(() => {
    return bookings.filter((booking) => {
      const st = normalizeStatus(booking.status)
      const matchesSearch =
        booking.customerName.toLowerCase().includes(searchQuery.toLowerCase()) ||
        booking.customerContact.toLowerCase().includes(searchQuery.toLowerCase())
      const matchesTab =
        activeTab === 'all' ||
        (activeTab === 'pending' && st === 'pending') ||
        (activeTab === 'confirmed' && st === 'confirmed') ||
        (activeTab === 'completed' && st === 'completed') ||
        (activeTab === 'cancelled' && st === 'cancelled')
      return matchesSearch && matchesTab
    })
  }, [bookings, searchQuery, activeTab])

  const getTabCount = (tab: string) => {
    if (tab === 'all') return bookings.length
    return bookings.filter((b) => normalizeStatus(b.status) === tab).length
  }

  const runTransition = async (bookingId: number, status: string) => {
    if (!merchantId) return
    setActionError('')
    try {
      await putMerchantBookingStatus(merchantId, bookingId, status)
      await reload()
    } catch (e) {
      setActionError(isMerchantBookingsApiError(e) ? e.message : t('common:errors.generic'))
    }
  }

  if (!merchantId) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold">{t('bookings.title')}</h1>
        <p className="text-sm text-muted-foreground">{t('bookings.noMerchantContext')}</p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">{t('bookings.title')}</h1>

      {loadError ? <p className="text-sm text-destructive">{loadError}</p> : null}
      {actionError ? <p className="text-sm text-destructive">{actionError}</p> : null}

      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList>
          <TabsTrigger value="all">
            {t('bookings.tabs.all')} ({getTabCount('all')})
          </TabsTrigger>
          <TabsTrigger value="pending">
            {t('bookings.tabs.pending')} ({getTabCount('pending')})
          </TabsTrigger>
          <TabsTrigger value="confirmed">
            {t('bookings.tabs.confirmed')} ({getTabCount('confirmed')})
          </TabsTrigger>
          <TabsTrigger value="completed">
            {t('bookings.tabs.completed')} ({getTabCount('completed')})
          </TabsTrigger>
          <TabsTrigger value="cancelled">
            {t('bookings.tabs.cancelled')} ({getTabCount('cancelled')})
          </TabsTrigger>
        </TabsList>

        <Card className="mt-4">
          <CardHeader>
            <div className="relative max-w-sm">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder={t('bookings.searchPlaceholder')}
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9"
              />
            </div>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="flex justify-center py-12">
                <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>{t('bookings.details.customer')}</TableHead>
                    <TableHead>{t('bookings.details.resource')}</TableHead>
                    <TableHead>{t('bookings.details.datetime')}</TableHead>
                    <TableHead>{t('bookings.details.price')}</TableHead>
                    <TableHead>{t('bookings.details.status')}</TableHead>
                    <TableHead className="w-[50px]">{t('bookings.details.actions')}</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredBookings.map((booking) => {
                    const st = normalizeStatus(booking.status)
                    const { date, time } = formatDateTime(booking.startAt)
                    return (
                      <TableRow key={booking.id}>
                        <TableCell>
                          <div>
                            <p className="font-medium">{booking.customerName}</p>
                            <p className="text-sm text-muted-foreground">{booking.customerContact}</p>
                          </div>
                        </TableCell>
                        <TableCell>
                          {t('bookings.serviceItem', { id: booking.serviceItemId })}
                        </TableCell>
                        <TableCell>
                          <div>
                            <p>{date}</p>
                            <p className="text-sm text-muted-foreground">{time}</p>
                          </div>
                        </TableCell>
                        <TableCell>—</TableCell>
                        <TableCell>
                          <Badge className={statusColors[st] ?? statusColors.pending}>
                            {t(`common:status.${st}`, { defaultValue: booking.status })}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button variant="ghost" size="icon">
                                <MoreHorizontal className="h-4 w-4" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                              {st === 'pending' && (
                                <DropdownMenuItem
                                  className="gap-2 text-destructive"
                                  onClick={() => void runTransition(booking.id, 'CANCELLED')}
                                >
                                  <X className="h-4 w-4" />
                                  {t('bookings.actions.reject')}
                                </DropdownMenuItem>
                              )}
                              {st === 'confirmed' && (
                                <>
                                  <DropdownMenuItem
                                    className="gap-2"
                                    onClick={() => void runTransition(booking.id, 'CHECKED_IN')}
                                  >
                                    <Check className="h-4 w-4" />
                                    {t('bookings.actions.checkIn')}
                                  </DropdownMenuItem>
                                  <DropdownMenuItem
                                    className="gap-2 text-destructive"
                                    onClick={() => void runTransition(booking.id, 'CANCELLED')}
                                  >
                                    <X className="h-4 w-4" />
                                    {t('bookings.actions.reject')}
                                  </DropdownMenuItem>
                                </>
                              )}
                              {(st === 'checked_in' || st === 'in_service') && (
                                <DropdownMenuItem
                                  className="gap-2"
                                  onClick={() => void runTransition(booking.id, 'COMPLETED')}
                                >
                                  <Check className="h-4 w-4" />
                                  {t('bookings.actions.complete')}
                                </DropdownMenuItem>
                              )}
                              <DropdownMenuItem className="gap-2">
                                <Phone className="h-4 w-4" />
                                {t('bookings.actions.contact')}
                              </DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </TableCell>
                      </TableRow>
                    )
                  })}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </Tabs>
    </div>
  )
}
