import { useAuthStore } from '@/shared/stores/authStore'

class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

export interface ClientProfileDto {
  authenticated: boolean
  role: string | null
  suggestedName: string | null
  suggestedContact: string | null
}

async function requestJson<T>(path: string): Promise<T> {
  return requestJsonWithInit<T>(path, undefined)
}

async function requestJsonWithInit<T>(path: string, init?: RequestInit): Promise<T> {
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

export function isClientProfileApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}

export function fetchClientProfile(): Promise<ClientProfileDto> {
  return requestJson<ClientProfileDto>('/api/client/profile')
}

export function putClientProfile(payload: {
  suggestedName?: string
  suggestedContact?: string
}): Promise<ClientProfileDto> {
  return requestJsonWithInit<ClientProfileDto>('/api/client/profile', {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}
