import type {
  AuthMeResponse,
  AuthContextOption,
  ContextSelectRequest,
  MerchantEnableRequest,
  MerchantEnableResponse,
  PublicRegisterRequestBody,
  PublicRegisterResponse,
  TokenResponse,
} from '@/shared/types/authContext'
import { useAuthStore } from '@/shared/stores/authStore'

class ApiError extends Error {
  status: number
  code?: string

  constructor(status: number, message: string, code?: string) {
    super(message)
    this.status = status
    this.code = code
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
    let code: string | undefined
    try {
      const payload = (await response.json()) as { message?: string; error?: string; code?: string }
      message = payload.message ?? payload.error ?? message
      code = payload.code
    } catch {
      // Keep fallback message.
    }
    throw new ApiError(response.status, message, code)
  }

  if (response.status === 204) {
    return null as T
  }

  return (await response.json()) as T
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}

function normalizeContextOption(context: AuthContextOption): AuthContextOption {
  return {
    ...context,
    canonicalRole: context.canonicalRole ?? context.role,
    canonicalRoles: context.canonicalRoles ?? [],
    roleAliases: context.roleAliases ?? [],
  }
}

function normalizeAuthMeResponse(payload: AuthMeResponse): AuthMeResponse {
  return {
    ...payload,
    canonicalRole: payload.canonicalRole ?? payload.role,
    canonicalRoles: payload.canonicalRoles ?? [],
    roleAliases: payload.roleAliases ?? [],
    availableContexts: (payload.availableContexts ?? []).map(normalizeContextOption),
    activeContext: payload.activeContext ? normalizeContextOption(payload.activeContext) : null,
  }
}

function normalizeTokenResponse(payload: TokenResponse): TokenResponse {
  return {
    ...payload,
    canonicalRole: payload.canonicalRole ?? payload.role,
    canonicalRoles: payload.canonicalRoles ?? [],
    roleAliases: payload.roleAliases ?? [],
  }
}

export async function fetchAuthMe(): Promise<AuthMeResponse> {
  const payload = await requestJson<AuthMeResponse>('/api/auth/me')
  return normalizeAuthMeResponse(payload)
}

/**
 * Anonymous public signup for end customers ({@code registerType: CLIENT}).
 * Email is used as platform login ID for non-admin accounts.
 */
export async function registerPublicClient(body: {
  email: string
  password: string
  /** Optional; server may ignore for CLIENT today. */
  name?: string
}): Promise<PublicRegisterResponse> {
  const payload: PublicRegisterRequestBody = {
    registerType: 'CLIENT',
    username: body.email.trim(),
    password: body.password,
    name: body.name?.trim() ? body.name.trim() : '',
    slug: '',
  }
  const response = await fetch('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  if (!response.ok) {
    let message = `Request failed: ${response.status}`
    let code: string | undefined
    try {
      const errPayload = (await response.json()) as { message?: string; code?: string }
      message = errPayload.message ?? message
      code = errPayload.code
    } catch {
      // ignore
    }
    throw new ApiError(response.status, message, code)
  }
  return (await response.json()) as PublicRegisterResponse
}

export async function loginWithPassword(loginId: string, password: string): Promise<TokenResponse> {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: loginId, password }),
  })
  if (!response.ok) {
    let message = `Request failed: ${response.status}`
    let code: string | undefined
    try {
      const payload = (await response.json()) as { message?: string; code?: string }
      message = payload.message ?? message
      code = payload.code
    } catch {
      // ignore
    }
    throw new ApiError(response.status, message, code)
  }
  const payload = (await response.json()) as TokenResponse
  return normalizeTokenResponse(payload)
}

export async function postLogout(): Promise<void> {
  await requestJson<null>('/api/auth/logout', { method: 'POST' })
}

export async function switchAuthContext(payload: ContextSelectRequest): Promise<TokenResponse> {
  const token = await requestJson<TokenResponse>('/api/auth/context/switch', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  return normalizeTokenResponse(token)
}

export async function enableMerchant(payload: MerchantEnableRequest): Promise<MerchantEnableResponse> {
  return requestJson<MerchantEnableResponse>('/api/auth/merchant/enable', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
