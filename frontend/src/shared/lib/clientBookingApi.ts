import { useAuthStore } from '@/shared/stores/authStore'

export type ClientBookingTab = 'upcoming' | 'past' | 'cancelled'

export interface ClientBookingListItemDto {
  id: string
  bookingNo: string
  serviceName: string
  providerName: string
  date: string
  time: string
  durationMinutes: number
  status: string
  price: number
}

export interface ClientBookingListResponseDto {
  items: ClientBookingListItemDto[]
  page: number
  size: number
  total: number
}

export interface ClientBookingStatusResponseDto {
  id: string
  status: string
  updatedAt: string
}

class ApiError extends Error {
  readonly status: number
  readonly errorCode?: string

  constructor(status: number, message: string, errorCode?: string) {
    super(message)
    this.status = status
    this.errorCode = errorCode
  }
}

interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const token = useAuthStore.getState().token
  const response = await fetch(path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init?.headers ?? {}),
    },
  })

  if (!response.ok) {
    let message = `Request failed: ${response.status}`
    let errorCode: string | undefined
    try {
      const payload = (await response.json()) as unknown
      if (payload && typeof payload === 'object') {
        const obj = payload as Record<string, unknown>
        if (
          'code' in obj &&
          'message' in obj &&
          'data' in obj &&
          obj.data != null &&
          typeof obj.data === 'object'
        ) {
          const data = obj.data as Record<string, unknown>
          if (typeof data.errorCode === 'string') {
            errorCode = data.errorCode
          }
        }
        const topMessage = typeof obj.message === 'string' ? obj.message : undefined
        const fallbackError = typeof obj.error === 'string' ? obj.error : undefined
        message = topMessage ?? fallbackError ?? message
      }
    } catch {
      // ignore
    }
    throw new ApiError(response.status, message, errorCode)
  }

  const payload = (await response.json()) as T | ApiEnvelope<T>
  if (
    payload &&
    typeof payload === 'object' &&
    'code' in payload &&
    'message' in payload &&
    'data' in payload
  ) {
    return (payload as ApiEnvelope<T>).data
  }
  return payload as T
}

export function isClientBookingApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}

export async function fetchClientBookings(
  tab: ClientBookingTab,
  page = 0,
  size = 20,
): Promise<ClientBookingListResponseDto> {
  const params = new URLSearchParams({
    tab,
    page: String(page),
    size: String(size),
  })
  return requestJson<ClientBookingListResponseDto>(`/api/client/bookings?${params.toString()}`)
}

export async function cancelClientBooking(
  bookingId: string,
  reason?: string,
): Promise<ClientBookingStatusResponseDto> {
  return requestJson<ClientBookingStatusResponseDto>(
    `/api/client/bookings/${encodeURIComponent(bookingId)}/cancel`,
    {
      method: 'PATCH',
      body: JSON.stringify({ reason }),
    },
  )
}

export async function rescheduleClientBooking(
  bookingId: string,
  newStartAt: string,
  reason?: string,
): Promise<ClientBookingStatusResponseDto> {
  return requestJson<ClientBookingStatusResponseDto>(
    `/api/client/bookings/${encodeURIComponent(bookingId)}/reschedule`,
    {
      method: 'PATCH',
      body: JSON.stringify({ newStartAt, reason }),
    },
  )
}

export interface ClientResourceAvailabilitySlotDto {
  startAt: string
  endAt: string
  isAvailable: boolean
  capacityRemaining: number | null
}

export interface ClientResourceAvailabilityResponseDto {
  date: string
  slots: ClientResourceAvailabilitySlotDto[]
}

export async function fetchResourceAvailability(
  resourceId: string,
  dateYmd: string,
): Promise<ClientResourceAvailabilityResponseDto> {
  const q = new URLSearchParams({ date: dateYmd })
  return requestJson<ClientResourceAvailabilityResponseDto>(
    `/api/client/resources/${encodeURIComponent(resourceId)}/availability?${q.toString()}`,
  )
}

export interface ClientBookingCreateBody {
  resourceId: string
  startAt: string
  notes?: string
  mode?: 'time' | 'resource_time' | 'session'
  guest?: {
    name: string
    phone: string
    email?: string
  }
}

export interface ClientBookingCreateResponseDto {
  id: string
  bookingNo: string
  status: string
  resourceId: string
  startAt: string
  endAt: string
  tenantId: string
  createdAt: string
}

export async function createClientBooking(
  body: ClientBookingCreateBody,
): Promise<ClientBookingCreateResponseDto> {
  // Backend accepts only resourceId, startAt, notes (see ClientBookingCreateRequest).
  // mode / guest are UI-only until API supports them.
  const payload: { resourceId: string; startAt: string; notes?: string } = {
    resourceId: body.resourceId,
    startAt: body.startAt,
  }
  if (body.notes != null && body.notes.trim() !== '') {
    payload.notes = body.notes.trim()
  }
  return requestJson<ClientBookingCreateResponseDto>('/api/client/bookings', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
