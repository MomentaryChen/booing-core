import { useAuthStore } from '@/shared/stores/authStore'

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
      // keep fallback
    }
    throw new ApiError(response.status, message)
  }

  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

export function isMerchantPortalApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}

export interface MerchantResourceDto {
  id: number
  name: string
  type: string
  category: string
  capacity: number
  active: boolean
  price: number
  assignedStaffIds: number[]
  serviceItemsJson: string
  /** Derived cover image from linked services, when available. */
  imageUrl: string | null
  status: 'ACTIVE' | 'MAINTENANCE' | 'FULLY_BOOKED'
  businessHoursJson: string
}

export interface MerchantBusinessHourDto {
  id: number
  dayOfWeek:
    | 'MONDAY'
    | 'TUESDAY'
    | 'WEDNESDAY'
    | 'THURSDAY'
    | 'FRIDAY'
    | 'SATURDAY'
    | 'SUNDAY'
  startTime: string
  endTime: string
}

export interface MerchantAvailabilityExceptionDto {
  id: number
  type: string
  startAt: string
  endAt: string
  reason: string | null
}

export interface MerchantProfileDto {
  description: string | null
  logoUrl: string | null
  address: string | null
  phone: string | null
  email: string | null
  website: string | null
  /** Present when backend supports merchant profile V21+. */
  storeCategory?: string | null
  lineContactUrl?: string | null
}

export interface MerchantCustomizationDto {
  themePreset: string | null
  themeColor: string | null
  heroTitle: string | null
  bookingFlowText: string | null
  inviteCode: string | null
  termsText: string | null
  announcementText: string | null
  faqJson: string | null
  bufferMinutes: number | null
  homepageSectionsJson: string | null
  categoryOrderJson: string | null
  notificationNewBooking?: boolean | null
  notificationCancellation?: boolean | null
  notificationDailySummary?: boolean | null
}

export interface MerchantServiceDto {
  id: number
  name: string
  durationMinutes: number
  price: number
  category: string
  imageUrl: string | null
  active: boolean
  resourceCount: number
}

export interface MerchantResourceListDto {
  items: MerchantResourceDto[]
  page: number
  size: number
  total: number
}

export function fetchMerchantResources(
  merchantId: number,
  query?: { page?: number; size?: number; status?: 'ACTIVE' | 'MAINTENANCE' | 'FULLY_BOOKED' },
): Promise<MerchantResourceListDto> {
  const params = new URLSearchParams()
  if (query?.page != null) params.set('page', String(query.page))
  if (query?.size != null) params.set('size', String(query.size))
  if (query?.status) params.set('status', query.status)
  const suffix = params.toString() ? `?${params.toString()}` : ''
  return requestJson(`/api/merchant/${merchantId}/resources${suffix}`)
}

export function fetchMerchantServices(merchantId: number): Promise<MerchantServiceDto[]> {
  return requestJson(`/api/merchant/${merchantId}/services`)
}

export function createMerchantService(
  merchantId: number,
  payload: {
    name: string
    durationMinutes: number
    price: number
    category: string
    imageUrl?: string | null
  },
): Promise<MerchantServiceDto> {
  return requestJson(`/api/merchant/${merchantId}/services`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function deleteMerchantService(merchantId: number, serviceId: number): Promise<void> {
  return requestJson(`/api/merchant/${merchantId}/services/${serviceId}`, {
    method: 'DELETE',
  })
}

export function updateMerchantService(
  merchantId: number,
  serviceId: number,
  payload: {
    name: string
    durationMinutes: number
    price: number
    category: string
    imageUrl?: string | null
  },
): Promise<MerchantServiceDto> {
  return requestJson(`/api/merchant/${merchantId}/services/${serviceId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function toggleMerchantServiceActive(
  merchantId: number,
  serviceId: number,
  active: boolean,
): Promise<MerchantServiceDto> {
  const params = new URLSearchParams({ active: String(active) })
  return requestJson(`/api/merchant/${merchantId}/services/${serviceId}/active?${params.toString()}`, {
    method: 'PATCH',
  })
}

export function cloneMerchantService(
  merchantId: number,
  serviceId: number,
  payload?: { nameSuffix?: string },
): Promise<MerchantServiceDto> {
  return requestJson(`/api/merchant/${merchantId}/services/${serviceId}/clone`, {
    method: 'POST',
    body: JSON.stringify(payload ?? {}),
  })
}

export function createMerchantResource(
  merchantId: number,
  payload: {
    name: string
    type: string
    category: string
    capacity: number
    active: boolean
    serviceItemsJson: string
    assignedStaffIds?: number[]
    price: number
  },
): Promise<MerchantResourceDto> {
  return requestJson(`/api/merchant/${merchantId}/resources`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function deleteMerchantResource(merchantId: number, resourceId: number): Promise<void> {
  return requestJson(`/api/merchant/${merchantId}/resources/${resourceId}`, {
    method: 'DELETE',
  })
}

export function updateMerchantResource(
  merchantId: number,
  resourceId: number,
  payload: Partial<{
    name: string
    type: string
    category: string
    capacity: number
    active: boolean
    serviceItemsJson: string
    assignedStaffIds: number[]
    price: number
  }>,
): Promise<MerchantResourceDto> {
  return requestJson(`/api/merchant/${merchantId}/resources/${resourceId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function batchUpdateMerchantResourcePrice(
  merchantId: number,
  payload: { resourceIds: number[]; price: number },
): Promise<MerchantResourceListDto> {
  return requestJson(`/api/merchant/${merchantId}/resources/batch/price`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function batchUpdateMerchantResourceStatus(
  merchantId: number,
  payload: { resourceIds: number[]; status: 'ACTIVE' | 'MAINTENANCE' | 'FULLY_BOOKED' },
): Promise<MerchantResourceListDto> {
  return requestJson(`/api/merchant/${merchantId}/resources/batch/status`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function batchUpdateMerchantResourceBusinessHours(
  merchantId: number,
  payload: { resourceIds: number[]; businessHoursJson: string },
): Promise<MerchantResourceListDto> {
  return requestJson(`/api/merchant/${merchantId}/resources/batch/business-hours`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function fetchMerchantBusinessHours(merchantId: number): Promise<MerchantBusinessHourDto[]> {
  return requestJson(`/api/merchant/${merchantId}/business-hours`)
}

export function putMerchantBusinessHours(
  merchantId: number,
  payload: Array<{ dayOfWeek: string; startTime: string; endTime: string }>,
): Promise<MerchantBusinessHourDto[]> {
  return requestJson(`/api/merchant/${merchantId}/business-hours`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function fetchMerchantAvailabilityExceptions(
  merchantId: number,
): Promise<MerchantAvailabilityExceptionDto[]> {
  return requestJson(`/api/merchant/${merchantId}/availability-exceptions`)
}

export function createMerchantAvailabilityException(
  merchantId: number,
  payload: { type: string; startAt: string; endAt: string; reason?: string },
): Promise<MerchantAvailabilityExceptionDto> {
  return requestJson(`/api/merchant/${merchantId}/availability-exceptions`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function deleteMerchantAvailabilityException(
  merchantId: number,
  exceptionId: number,
): Promise<void> {
  return requestJson(`/api/merchant/${merchantId}/availability-exceptions/${exceptionId}`, {
    method: 'DELETE',
  })
}

export function fetchMerchantProfile(merchantId: number): Promise<MerchantProfileDto> {
  return requestJson(`/api/merchant/${merchantId}/profile`)
}

export function putMerchantProfile(
  merchantId: number,
  payload: MerchantProfileDto,
): Promise<MerchantProfileDto> {
  return requestJson(`/api/merchant/${merchantId}/profile`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function fetchMerchantCustomization(merchantId: number): Promise<MerchantCustomizationDto> {
  return requestJson(`/api/merchant/${merchantId}/customization`)
}

export function putMerchantCustomization(
  merchantId: number,
  payload: MerchantCustomizationDto,
): Promise<MerchantCustomizationDto> {
  return requestJson(`/api/merchant/${merchantId}/customization`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}
