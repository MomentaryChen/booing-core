import { useAuthStore } from '@/shared/stores/authStore'

export interface ClientMerchantCardDto {
  merchantId: string
  name: string
  slug: string
  visibility: string
  joinState: string
}

export interface ServiceItemSummaryDto {
  id: string
  name: string
  durationMinutes: number
  price: number
  category: string
  imageUrl: string | null
}

export interface ResourceItemSummaryDto {
  id: string
  name: string
  type: string
  category: string
  capacity: number
  active: boolean
  price: number
  /** Derived cover image from linked services, when available. */
  imageUrl: string | null
}

export interface ClientCatalogResourceDto {
  id: string
  name: string
  category: string
  price: number
  durationMinutes: number
  rating: number
  imageUrl: string | null
  merchantName: string
  resourceType?: string | null
  availabilityLabel?: 'available' | 'limited' | 'unavailable' | 'stale' | null
  remainingUnits?: number | null
  seatsLeft?: number | null
  nextAvailableAt?: string | null
}

export interface ClientCategoryDto {
  key: string
  label: string
  count: number
}

export interface ClientResourcesResponseDto {
  items: ClientCatalogResourceDto[]
  page: number
  size: number
  total: number
}

export interface ClientResourceDetailDto {
  id: string
  name: string
  description: string
  category: string
  price: number
  durationMinutes: number
  rating: number
  merchant: MerchantSummaryDto
  imageUrl: string | null
}

export interface MerchantSummaryDto {
  id: string
  name: string
  slug: string
  active: boolean
}

/** Mirrors `ClientMerchantResponse` JSON from GET /api/client/merchant/{slug}. */
export interface ClientMerchantStorefrontDto {
  merchant: MerchantSummaryDto
  profile: { description: string | null; logoUrl: string | null }
  customization: Record<string, unknown>
  services: ServiceItemSummaryDto[]
  resources: ResourceItemSummaryDto[]
  dynamicFields: unknown[]
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

export function isClientCatalogApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}

/** Anonymous-friendly: lists active merchants (join state depends on optional auth). */
export async function fetchVisibleMerchants(): Promise<ClientMerchantCardDto[]> {
  return requestJson<ClientMerchantCardDto[]>('/api/client/merchants')
}

export async function fetchMerchantStorefront(slug: string): Promise<ClientMerchantStorefrontDto> {
  return requestJson<ClientMerchantStorefrontDto>(`/api/client/merchant/${encodeURIComponent(slug)}`)
}

export async function fetchFeaturedResources(limit = 6): Promise<ClientCatalogResourceDto[]> {
  const q = new URLSearchParams({ limit: String(limit) })
  return requestJson<ClientCatalogResourceDto[]>(`/api/client/resources/featured?${q.toString()}`)
}

export async function fetchClientCategories(): Promise<ClientCategoryDto[]> {
  return requestJson<ClientCategoryDto[]>('/api/client/categories')
}

export async function fetchClientResources(params: {
  q?: string
  category?: string
  resourceType?: string
  sort?: 'relevance' | 'priceAsc' | 'priceDesc' | 'rating'
  page?: number
  size?: number
}): Promise<ClientResourcesResponseDto> {
  const q = new URLSearchParams()
  if (params.q) q.set('q', params.q)
  if (params.category) q.set('category', params.category)
  if (params.resourceType && params.resourceType !== 'all') q.set('resourceType', params.resourceType)
  if (params.sort) q.set('sort', params.sort)
  q.set('page', String(params.page ?? 0))
  q.set('size', String(params.size ?? 20))
  return requestJson<ClientResourcesResponseDto>(`/api/client/resources?${q.toString()}`)
}

export async function fetchClientResourceDetail(resourceId: string): Promise<ClientResourceDetailDto> {
  return requestJson<ClientResourceDetailDto>(`/api/client/resources/${encodeURIComponent(resourceId)}`)
}
