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
      const payload = (await response.json()) as { message?: string }
      message = payload.message ?? message
    } catch {
      // ignore
    }
    throw new ApiError(response.status, message)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

export function isSystemAdminApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}

export interface SystemOverviewDto {
  totalMerchants: number
  activeMerchants: number
  totalBookings: number
  domainTemplates: number
}

export interface SystemCommandCenterDto {
  pendingActions?: number
  pendingActionsTotal?: number
  revenueToday?: number
  occupancyRate?: number
  liveFeed?: Array<{
    bookingId: number
    merchantId: number
    merchantName: string
    customerName: string
    startAt: string
    status: string
    serviceName: string
  }>
}

export interface SystemMerchantDto {
  id: number
  name: string
  slug: string
  active: boolean
  serviceLimit?: number
}

export interface SystemUserSummaryDto {
  id: number
  username: string
  enabled: boolean
  primaryRole: string
  primaryMerchantId: number | null
  roleCodes: string[]
  lastLoginAt: string | null
}

export interface SystemRbacRoleDto {
  roleCode: string
  permissions: string[]
}

export interface SystemAuditLogDto {
  id: number
  actor: string
  action: string
  targetType: string
  targetId: number
  detail: string
  correlationId: string
  createdAt: string
}

export function fetchSystemOverview(): Promise<SystemOverviewDto> {
  return requestJson<SystemOverviewDto>('/api/system/overview')
}

export function fetchSystemCommandCenter(): Promise<SystemCommandCenterDto> {
  return requestJson<SystemCommandCenterDto>('/api/system/command-center')
}

export function fetchSystemMerchants(): Promise<SystemMerchantDto[]> {
  return requestJson<SystemMerchantDto[]>('/api/system/merchants')
}

export function putSystemMerchantStatus(
  merchantId: number,
  active: boolean,
): Promise<SystemMerchantDto> {
  return requestJson<SystemMerchantDto>(`/api/system/merchants/${merchantId}/status`, {
    method: 'PUT',
    body: JSON.stringify({ active }),
  })
}

export function fetchSystemUsers(): Promise<SystemUserSummaryDto[]> {
  return requestJson<SystemUserSummaryDto[]>('/api/system/users')
}

export function putSystemUserStatus(
  userId: number,
  enabled: boolean,
): Promise<SystemUserSummaryDto> {
  return requestJson<SystemUserSummaryDto>(`/api/system/users/${userId}/status`, {
    method: 'PUT',
    body: JSON.stringify({ enabled }),
  })
}

export function fetchSystemRbacRoles(): Promise<SystemRbacRoleDto[]> {
  return requestJson<SystemRbacRoleDto[]>('/api/system/rbac/roles')
}

export function fetchSystemAuditLogs(): Promise<SystemAuditLogDto[]> {
  return requestJson<SystemAuditLogDto[]>('/api/system/audit-logs')
}
