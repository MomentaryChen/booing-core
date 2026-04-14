import { useAuthStore } from '@/shared/stores/authStore'

export type ClientBookingTab = 'upcoming' | 'past' | 'cancelled'

export interface ClientBookingListItemDto {
  id: number
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

class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
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
    try {
      const payload = (await response.json()) as { message?: string; error?: string }
      message = payload.message ?? payload.error ?? message
    } catch {
      // ignore
    }
    throw new ApiError(response.status, message)
  }

  return (await response.json()) as T
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
  resourceId: number,
  dateYmd: string,
): Promise<ClientResourceAvailabilityResponseDto> {
  const q = new URLSearchParams({ date: dateYmd })
  return requestJson<ClientResourceAvailabilityResponseDto>(
    `/api/client/resources/${resourceId}/availability?${q.toString()}`,
  )
}

export interface ClientBookingCreateBody {
  resourceId: number
  startAt: string
  notes?: string
}

export interface ClientBookingCreateResponseDto {
  id: number
  bookingNo: string
  status: string
  resourceId: number
  startAt: string
  endAt: string
  tenantId: number
  createdAt: string
}

export async function createClientBooking(
  body: ClientBookingCreateBody,
): Promise<ClientBookingCreateResponseDto> {
  const response = await fetch('/api/client/bookings', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(useAuthStore.getState().token
        ? { Authorization: `Bearer ${useAuthStore.getState().token}` }
        : {}),
    },
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    let message = `Request failed: ${response.status}`
    try {
      const payload = (await response.json()) as { message?: string }
      message = payload.message ?? message
    } catch {
      // ignore
    }
    throw new ApiError(response.status, message)
  }
  return (await response.json()) as ClientBookingCreateResponseDto
}
