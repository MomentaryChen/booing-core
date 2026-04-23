import { useCallback, useEffect, useMemo, useState } from 'react'
import { motion } from 'framer-motion'
import { format, parseISO } from 'date-fns'
import { Link } from 'react-router-dom'
import { cn } from '@/shared/lib/utils'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Input } from '@/components/ui/input'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import {
  cancelClientBooking,
  fetchBookingRescheduleAvailability,
  fetchClientBookings,
  isClientBookingApiError,
  rescheduleClientBooking,
  type ClientBookingListItemDto,
  type ClientBookingRescheduleAvailabilitySlotDto,
  type ClientBookingTab,
} from '@/shared/lib/clientBookingApi'
import {
  Calendar,
  Clock,
  MapPin,
  User,
  ChevronLeft,
  ChevronRight,
  AlertCircle,
  CheckCircle2,
  XCircle,
} from 'lucide-react'

interface Booking {
  id: string
  title: string
  date: string
  time: string
  location: string
  host: string
  status: 'upcoming' | 'completed' | 'cancelled'
  price: number
  image?: string
}

interface Tab {
  id: string
  label: string
  count: number
}

const TAB_KEYS: ClientBookingTab[] = ['upcoming', 'past', 'cancelled']
type UiBookingTab = 'all' | 'upcoming' | 'completed' | 'cancelled'

function normalizeStatus(status: string): Booking['status'] {
  const lower = status.toLowerCase()
  if (lower === 'cancelled') return 'cancelled'
  if (lower === 'completed') return 'completed'
  return 'upcoming'
}

function mapDto(dto: ClientBookingListItemDto): Booking {
  return {
    id: String(dto.id),
    title: dto.serviceName,
    date: dto.date,
    time: dto.time,
    location: 'N/A',
    host: dto.providerName,
    status: normalizeStatus(dto.status),
    price: typeof dto.price === 'number' ? dto.price : Number(dto.price),
  }
}

function StatusBadge({ status }: { status: Booking['status'] }) {
  const variants = {
    upcoming: { icon: Clock, label: 'Upcoming', className: 'bg-blue-500/10 text-blue-500 border-blue-500/20' },
    completed: { icon: CheckCircle2, label: 'Completed', className: 'bg-green-500/10 text-green-500 border-green-500/20' },
    cancelled: { icon: XCircle, label: 'Cancelled', className: 'bg-red-500/10 text-red-500 border-red-500/20' },
  }

  const { icon: Icon, label, className } = variants[status]

  return (
    <Badge variant="outline" className={cn('gap-1', className)}>
      <Icon className="h-3 w-3" />
      {label}
    </Badge>
  )
}

function BookingCard({
  booking,
  isCancelling,
  onCancel,
  onReschedule,
}: {
  booking: Booking
  isCancelling: boolean
  onCancel: (bookingId: string) => void
  onReschedule: (booking: Booking) => void
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -20 }}
      transition={{ duration: 0.3 }}
    >
      <Card className="overflow-hidden transition-shadow duration-300 hover:shadow-lg">
        <div className="flex flex-col md:flex-row">
          <div className="relative h-48 w-full bg-muted md:h-auto md:w-48">
            {booking.image ? (
              <img src={booking.image} alt={booking.title} className="h-full w-full object-cover" />
            ) : (
              <div className="flex h-full w-full items-center justify-center">
                <MapPin className="h-12 w-12 text-muted-foreground" />
              </div>
            )}
            <div className="absolute right-3 top-3">
              <StatusBadge status={booking.status} />
            </div>
          </div>

          <div className="flex flex-1 flex-col">
            <CardHeader>
              <CardTitle className="text-xl">{booking.title}</CardTitle>
              <CardDescription className="flex items-center gap-1">
                <MapPin className="h-4 w-4" />
                {booking.location}
              </CardDescription>
            </CardHeader>

            <CardContent className="flex-1">
              <div className="space-y-2 text-sm">
                <div className="flex items-center gap-2 text-muted-foreground">
                  <Calendar className="h-4 w-4" />
                  <span>{new Date(booking.date).toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}</span>
                </div>
                <div className="flex items-center gap-2 text-muted-foreground">
                  <Clock className="h-4 w-4" />
                  <span>{booking.time}</span>
                </div>
                <div className="flex items-center gap-2 text-muted-foreground">
                  <User className="h-4 w-4" />
                  <span>Host: {booking.host}</span>
                </div>
                <div className="pt-2 text-lg font-semibold">${booking.price}</div>
              </div>
            </CardContent>

            <CardFooter className="flex-wrap gap-2">
              {booking.status === 'upcoming' && (
                <>
                  <Button variant="default" size="sm">View Details</Button>
                  <Button variant="outline" size="sm" onClick={() => onReschedule(booking)}>Modify</Button>
                  <Button
                    variant="destructive"
                    size="sm"
                    disabled={isCancelling}
                    onClick={() => onCancel(booking.id)}
                  >
                    {isCancelling ? 'Cancelling...' : 'Cancel'}
                  </Button>
                </>
              )}
              {booking.status === 'completed' && (
                <>
                  <Button variant="default" size="sm">View Details</Button>
                  <Button variant="outline" size="sm">Leave Review</Button>
                  <Button variant="outline" size="sm">Book Again</Button>
                </>
              )}
              {booking.status === 'cancelled' && (
                <>
                  <Button variant="default" size="sm">View Details</Button>
                  <Button variant="outline" size="sm">Book Again</Button>
                </>
              )}
            </CardFooter>
          </div>
        </div>
      </Card>
    </motion.div>
  )
}

function BookingCardSkeleton() {
  return (
    <Card className="overflow-hidden">
      <div className="flex flex-col md:flex-row">
        <Skeleton className="h-48 w-full md:h-auto md:w-48" />
        <div className="flex flex-1 flex-col">
          <CardHeader>
            <Skeleton className="h-6 w-3/4" />
            <Skeleton className="mt-2 h-4 w-1/2" />
          </CardHeader>
          <CardContent className="flex-1 space-y-2">
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-2/3" />
            <Skeleton className="h-4 w-1/2" />
          </CardContent>
          <CardFooter className="gap-2">
            <Skeleton className="h-9 w-24" />
            <Skeleton className="h-9 w-24" />
          </CardFooter>
        </div>
      </div>
    </Card>
  )
}

function EmptyState({ status }: { status: string }) {
  return (
    <div className="flex flex-col items-center justify-center px-4 py-16 text-center">
      <div className="mb-4 rounded-full bg-muted p-6">
        <AlertCircle className="h-12 w-12 text-muted-foreground" />
      </div>
      <h3 className="mb-2 text-xl font-semibold">No {status} bookings</h3>
      <p className="mb-6 max-w-md text-muted-foreground">
        {status === 'upcoming' && "You don't have any upcoming bookings. Start exploring and book your next adventure!"}
        {status === 'completed' && "You haven't completed any bookings yet."}
        {status === 'cancelled' && "You don't have any cancelled bookings."}
      </p>
      <Link to="/search">
        <Button>Explore Listings</Button>
      </Link>
    </div>
  )
}

function ErrorState({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center px-4 py-16 text-center">
      <div className="mb-4 rounded-full bg-destructive/10 p-6">
        <XCircle className="h-12 w-12 text-destructive" />
      </div>
      <h3 className="mb-2 text-xl font-semibold">Something went wrong</h3>
      <p className="mb-6 max-w-md text-muted-foreground">
        We couldn&apos;t load your bookings. Please try again later.
      </p>
      <Button variant="outline" onClick={onRetry}>Try Again</Button>
    </div>
  )
}

function PaginationControls({
  currentPage,
  totalPages,
  onPageChange,
}: {
  currentPage: number
  totalPages: number
  onPageChange: (page: number) => void
}) {
  return (
    <div className="mt-8 flex items-center justify-center gap-2">
      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 1}
      >
        <ChevronLeft className="h-4 w-4" />
        Previous
      </Button>

      <div className="flex items-center gap-1">
        {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
          <Button
            key={page}
            variant={page === currentPage ? 'default' : 'outline'}
            size="sm"
            onClick={() => onPageChange(page)}
            className="w-9"
          >
            {page}
          </Button>
        ))}
      </div>

      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage === totalPages}
      >
        Next
        <ChevronRight className="h-4 w-4" />
      </Button>
    </div>
  )
}

export function MyBookingsPage() {
  const [activeTab, setActiveTab] = useState<UiBookingTab>('all')
  const [lists, setLists] = useState<Record<ClientBookingTab, Booking[]>>({ upcoming: [], past: [], cancelled: [] })
  const [counts, setCounts] = useState<Record<ClientBookingTab, number>>({ upcoming: 0, past: 0, cancelled: 0 })
  const [loadingByTab, setLoadingByTab] = useState<Record<ClientBookingTab, boolean>>({
    upcoming: true,
    past: true,
    cancelled: true,
  })
  const [error, setError] = useState<string | null>(null)
  const [currentPage, setCurrentPage] = useState(1)
  const itemsPerPage = 4
  const [cancellingBookingId, setCancellingBookingId] = useState<string | null>(null)
  const [cancelTargetBookingId, setCancelTargetBookingId] = useState<string | null>(null)
  const [rescheduleTargetBooking, setRescheduleTargetBooking] = useState<Booking | null>(null)
  const [rescheduleDate, setRescheduleDate] = useState('')
  const [rescheduleSlots, setRescheduleSlots] = useState<ClientBookingRescheduleAvailabilitySlotDto[]>([])
  const [rescheduleSlotsLoading, setRescheduleSlotsLoading] = useState(false)
  const [rescheduleSlotsError, setRescheduleSlotsError] = useState('')
  const [rescheduleStartAt, setRescheduleStartAt] = useState<string | null>(null)

  const loadTab = useCallback(async (targetTab: ClientBookingTab, targetPage: number) => {
    setLoadingByTab((prev) => ({ ...prev, [targetTab]: true }))
    try {
      const response = await fetchClientBookings(targetTab, targetPage, itemsPerPage)
      setLists((prev) => ({ ...prev, [targetTab]: response.items.map(mapDto) }))
      setCounts((prev) => ({ ...prev, [targetTab]: response.total }))
      setError(null)
    } catch (e) {
      setError(isClientBookingApiError(e) ? e.message : 'Failed to load bookings')
    } finally {
      setLoadingByTab((prev) => ({ ...prev, [targetTab]: false }))
    }
  }, [])

  const loadAllFirstPages = useCallback(async () => {
    await Promise.all(TAB_KEYS.map((tab) => loadTab(tab, 0)))
  }, [loadTab])

  useEffect(() => {
    void loadAllFirstPages()
  }, [loadAllFirstPages])

  useEffect(() => {
    if (!rescheduleTargetBooking || !rescheduleDate) {
      setRescheduleSlots([])
      setRescheduleSlotsError('')
      setRescheduleStartAt(null)
      return
    }
    let cancelled = false
    const run = async () => {
      setRescheduleSlotsLoading(true)
      setRescheduleSlotsError('')
      setRescheduleStartAt(null)
      try {
        const response = await fetchBookingRescheduleAvailability(rescheduleTargetBooking.id, rescheduleDate)
        if (cancelled) return
        setRescheduleSlots(response.slots.filter((slot) => slot.available))
      } catch (e) {
        if (cancelled) return
        setRescheduleSlots([])
        setRescheduleSlotsError(isClientBookingApiError(e) ? e.message : 'Failed to load available slots')
      } finally {
        if (!cancelled) setRescheduleSlotsLoading(false)
      }
    }
    void run()
    return () => {
      cancelled = true
    }
  }, [rescheduleDate, rescheduleTargetBooking])

  const handleCancel = useCallback(async (bookingId: string) => {
    setCancellingBookingId(bookingId)
    try {
      await cancelClientBooking(bookingId, 'client-request')
      await loadAllFirstPages()
      setError(null)
    } catch (e) {
      setError(isClientBookingApiError(e) ? e.message : 'Failed to cancel booking')
    } finally {
      setCancellingBookingId(null)
      setCancelTargetBookingId(null)
    }
  }, [loadAllFirstPages])

  const handleReschedule = useCallback(async (bookingId: string) => {
    if (!rescheduleStartAt) return
    try {
      await rescheduleClientBooking(bookingId, rescheduleStartAt, 'client-reschedule')
      await loadAllFirstPages()
      setRescheduleTargetBooking(null)
      setRescheduleStartAt(null)
      setRescheduleSlots([])
      setRescheduleSlotsError('')
      setError(null)
    } catch (e) {
      setError(isClientBookingApiError(e) ? e.message : 'Failed to reschedule booking')
    }
  }, [loadAllFirstPages, rescheduleStartAt])

  const allBookings = useMemo(
    () => [...lists.upcoming, ...lists.past, ...lists.cancelled],
    [lists.cancelled, lists.past, lists.upcoming],
  )
  const completedBookings = useMemo(
    () => lists.past.filter((booking) => booking.status === 'completed'),
    [lists.past],
  )

  const tabs: Tab[] = [
    { id: 'all', label: 'All Bookings', count: counts.upcoming + counts.past + counts.cancelled },
    { id: 'upcoming', label: 'Upcoming', count: counts.upcoming },
    { id: 'completed', label: 'Completed', count: counts.past },
    { id: 'cancelled', label: 'Cancelled', count: counts.cancelled },
  ]

  const activeTabBookings = activeTab === 'all'
    ? allBookings
    : activeTab === 'upcoming'
      ? lists.upcoming
      : activeTab === 'completed'
        ? completedBookings
        : lists.cancelled

  const isLoading = activeTab === 'all'
    ? TAB_KEYS.some((tab) => loadingByTab[tab])
    : loadingByTab[activeTab === 'completed' ? 'past' : (activeTab as ClientBookingTab)]

  const hasError = !!error
  const totalPages = activeTab === 'all'
    ? Math.max(1, Math.ceil(activeTabBookings.length / itemsPerPage))
    : Math.max(
      1,
      Math.ceil(
        (activeTab === 'upcoming'
          ? counts.upcoming
          : activeTab === 'completed'
            ? counts.past
            : counts.cancelled) / itemsPerPage,
      ),
    )

  const paginatedBookings = activeTab === 'all'
    ? activeTabBookings.slice((currentPage - 1) * itemsPerPage, currentPage * itemsPerPage)
    : activeTabBookings

  const handlePageChange = async (page: number) => {
    const safePage = Math.min(Math.max(page, 1), totalPages)
    setCurrentPage(safePage)
    if (activeTab === 'all') return
    const targetApiTab: ClientBookingTab = activeTab === 'completed' ? 'past' : (activeTab as ClientBookingTab)
    await loadTab(targetApiTab, safePage - 1)
  }

  const handleTabChange = (tabId: UiBookingTab) => {
    setActiveTab(tabId)
    setCurrentPage(1)
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto max-w-7xl px-4 py-6 md:py-8">
        <div className="mb-8">
          <h1 className="mb-2 text-4xl font-bold">My Bookings</h1>
          <p className="text-muted-foreground">Manage and track all your reservations</p>
        </div>

        <div className="mb-8">
          <div className="inline-flex flex-wrap gap-2 rounded-lg bg-muted/50 p-1">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => handleTabChange(tab.id as UiBookingTab)}
                className={cn(
                  'relative rounded-md px-4 py-2 text-sm font-medium transition-colors',
                  'hover:bg-background/80',
                  activeTab === tab.id && 'bg-background shadow-sm',
                )}
              >
                {activeTab === tab.id && (
                  <motion.div
                    layoutId="active-tab"
                    className="absolute inset-0 rounded-md bg-background shadow-sm"
                    transition={{ type: 'spring', duration: 0.5 }}
                  />
                )}
                <span className="relative z-10 flex items-center gap-2">
                  {tab.label}
                  <Badge variant="secondary" className="ml-1">
                    {tab.count}
                  </Badge>
                </span>
              </button>
            ))}
          </div>
        </div>
        {error && (
          <p className="mb-4 rounded-md border border-destructive/50 bg-destructive/10 px-4 py-3 text-sm text-destructive">
            {error}
          </p>
        )}

        <div>
          {isLoading ? (
            <div className="space-y-4">
              {Array.from({ length: 3 }).map((_, i) => (
                <BookingCardSkeleton key={i} />
              ))}
            </div>
          ) : hasError ? (
            <ErrorState onRetry={() => void loadAllFirstPages()} />
          ) : paginatedBookings.length === 0 ? (
            <EmptyState status={activeTab === 'all' ? 'bookings' : activeTab} />
          ) : (
            <>
              <div className="space-y-4">
                {paginatedBookings.map((booking) => (
                  <BookingCard
                    key={booking.id}
                    booking={booking}
                    isCancelling={cancellingBookingId === booking.id}
                    onCancel={setCancelTargetBookingId}
                    onReschedule={(targetBooking) => {
                      setRescheduleTargetBooking(targetBooking)
                      setRescheduleDate(targetBooking.date)
                      setRescheduleStartAt(null)
                      setRescheduleSlots([])
                      setRescheduleSlotsError('')
                    }}
                  />
                ))}
              </div>

              {totalPages > 1 && (
                <PaginationControls
                  currentPage={currentPage}
                  totalPages={totalPages}
                  onPageChange={(page) => void handlePageChange(page)}
                />
              )}
            </>
          )}
        </div>
      </div>
      <AlertDialog open={!!cancelTargetBookingId} onOpenChange={(open) => !open && setCancelTargetBookingId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Cancel booking</AlertDialogTitle>
            <AlertDialogDescription>Are you sure you want to cancel this booking?</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Back</AlertDialogCancel>
            <AlertDialogAction
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              onClick={() => cancelTargetBookingId && void handleCancel(cancelTargetBookingId)}
            >
              Confirm cancel
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
      <AlertDialog
        open={!!rescheduleTargetBooking}
        onOpenChange={(open) => {
          if (!open) {
            setRescheduleTargetBooking(null)
            setRescheduleStartAt(null)
            setRescheduleSlots([])
            setRescheduleSlotsError('')
          }
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Reschedule booking</AlertDialogTitle>
            <AlertDialogDescription>
              {rescheduleTargetBooking ? `${rescheduleTargetBooking.title} · ${rescheduleTargetBooking.host}` : ''}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <div className="space-y-3">
            <Input type="date" value={rescheduleDate} onChange={(e) => setRescheduleDate(e.target.value)} />
            {rescheduleSlotsLoading ? (
              <p className="text-sm text-muted-foreground">Loading slots...</p>
            ) : rescheduleSlotsError ? (
              <p className="text-sm text-destructive">{rescheduleSlotsError}</p>
            ) : rescheduleDate && rescheduleSlots.length === 0 ? (
              <p className="text-sm text-muted-foreground">No available slots for selected date.</p>
            ) : (
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
                {rescheduleSlots.map((slot) => {
                  const label = format(parseISO(slot.startAt), 'HH:mm')
                  const active = rescheduleStartAt === slot.startAt
                  return (
                    <Button
                      key={slot.startAt}
                      type="button"
                      variant={active ? 'default' : 'outline'}
                      onClick={() => setRescheduleStartAt(slot.startAt)}
                    >
                      {label}
                    </Button>
                  )
                })}
              </div>
            )}
          </div>
          <AlertDialogFooter>
            <AlertDialogCancel>Back</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => rescheduleTargetBooking && void handleReschedule(rescheduleTargetBooking.id)}
              disabled={!rescheduleStartAt}
            >
              Confirm reschedule
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
