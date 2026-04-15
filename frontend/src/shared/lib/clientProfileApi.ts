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
  language: string | null
  timezone: string | null
  currency: string | null
  emailNotifications: boolean | null
  smsNotifications: boolean | null
}

interface ClientProfilePreferencesResponseRaw {
  language?: string | null
  timezone?: string | null
  currency?: string | null
  notificationPrefs?: {
    email?: boolean | null
    sms?: boolean | null
  } | null
  emailNotifications?: boolean | null
  smsNotifications?: boolean | null
}

export interface ClientProfilePreferencesDto {
  language: string | null
  timezone: string | null
  currency: string | null
  notificationPrefs: {
    email: boolean | null
    sms: boolean | null
  }
}

export interface ClientPasswordUpdateResponse {
  updatedAt: string
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
  language?: string
  timezone?: string
  currency?: string
  emailNotifications?: boolean
  smsNotifications?: boolean
}): Promise<ClientProfileDto> {
  return requestJsonWithInit<ClientProfileDto>('/api/client/profile', {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function patchClientProfilePassword(payload: {
  currentPassword: string
  newPassword: string
}): Promise<ClientPasswordUpdateResponse> {
  return requestJsonWithInit<ClientPasswordUpdateResponse>('/api/client/profile/password', {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

function normalizePreferences(
  payload: ClientProfilePreferencesResponseRaw,
): ClientProfilePreferencesDto {
  return {
    language: payload.language ?? null,
    timezone: payload.timezone ?? null,
    currency: payload.currency ?? null,
    notificationPrefs: {
      email: payload.notificationPrefs?.email ?? payload.emailNotifications ?? null,
      sms: payload.notificationPrefs?.sms ?? payload.smsNotifications ?? null,
    },
  }
}

export async function fetchClientProfilePreferences(): Promise<ClientProfilePreferencesDto> {
  const response = await requestJson<ClientProfilePreferencesResponseRaw>(
    '/api/client/profile/preferences',
  )
  return normalizePreferences(response)
}

export async function putClientProfilePreferences(payload: {
  language?: string | null
  timezone?: string | null
  currency?: string | null
  notificationPrefs?: {
    email?: boolean | null
    sms?: boolean | null
  }
}): Promise<ClientProfilePreferencesDto> {
  const response = await requestJsonWithInit<ClientProfilePreferencesResponseRaw>(
    '/api/client/profile/preferences',
    {
      method: 'PUT',
      body: JSON.stringify(payload),
    },
  )
  return normalizePreferences(response)
}
