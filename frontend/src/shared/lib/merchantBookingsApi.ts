import { useAuthStore } from '@/shared/stores/authStore'

export interface MerchantBookingRowDto {
  id: number
  serviceItemId: number
  startAt: string
  endAt: string
  customerName: string
  customerContact: string
  status: string
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
      const payload = (await response.json()) as { message?: string }
      message = payload.message ?? message
    } catch {
      // ignore
    }
    throw new ApiError(response.status, message)
  }

  return (await response.json()) as T
}

export function isMerchantBookingsApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}

export async function fetchMerchantBookings(
  merchantId: number,
  status?: string,
): Promise<MerchantBookingRowDto[]> {
  const q = status ? `?status=${encodeURIComponent(status)}` : ''
  return requestJson<MerchantBookingRowDto[]>(`/api/merchant/${merchantId}/bookings${q}`)
}

export async function putMerchantBookingStatus(
  merchantId: number,
  bookingId: number,
  status: string,
  reason?: string,
): Promise<MerchantBookingRowDto> {
  return requestJson<MerchantBookingRowDto>(`/api/merchant/${merchantId}/bookings/${bookingId}/status`, {
    method: 'PUT',
    body: JSON.stringify({ status, reason: reason ?? null }),
  })
}
