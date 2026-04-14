import { useAuthStore } from '@/shared/stores/authStore'

export interface ClientMerchantCardDto {
  merchantId: number
  name: string
  slug: string
  visibility: string
  joinState: string
}

export interface ServiceItemSummaryDto {
  id: number
  name: string
  durationMinutes: number
  price: number
  category: string
  imageUrl: string | null
}

export interface ResourceItemSummaryDto {
  id: number
  name: string
  type: string
  category: string
  capacity: number
  active: boolean
  price: number
  /** Derived cover image from linked services, when available. */
  imageUrl: string | null
}

export interface MerchantSummaryDto {
  id: number
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
