import { jwtDecode } from 'jwt-decode'
import { resolveCanonicalRole, toCanonicalRole } from '@/shared/lib/roleCompat'

export type UserRole =
  | 'CLIENT'
  | 'CLIENT_USER'
  | 'MERCHANT'
  | 'MERCHANT_OWNER'
  | 'SUB_MERCHANT'
  | 'MERCHANT_STAFF'
  | 'SYSTEM_ADMIN'
  | 'ADMIN'

/** Matches backend JWT (`sub`, `role`, optional `merchantId`, `cv`). */
export interface JWTPayload {
  sub: string
  email?: string
  name?: string
  role?: UserRole | string
  canonicalRole?: UserRole
  canonicalRoles?: UserRole[]
  roleAliases?: string[]
  roles?: string[]
  /** Merchant scope from backend JWT claim */
  merchantId?: number
  tenantId?: string
  permissions?: string[]
  cv?: number
  exp: number
  iat: number
}

export function decodeToken(token: string): JWTPayload | null {
  try {
    return jwtDecode<JWTPayload>(token)
  } catch {
    return null
  }
}

export function isTokenExpired(token: string): boolean {
  const decoded = decodeToken(token)
  if (!decoded) return true
  return decoded.exp * 1000 < Date.now()
}

export function getRedirectUrlForRole(role: UserRole): string {
  const canonicalRole = toCanonicalRole(role)
  switch (canonicalRole) {
    case 'CLIENT_USER':
      return '/client'
    case 'MERCHANT_OWNER':
    case 'MERCHANT_STAFF':
      return '/merchant'
    case 'SYSTEM_ADMIN':
      return '/system'
    default:
      return '/'
  }
}

export function getRoleFromToken(token: string): UserRole | null {
  const decoded = decodeToken(token)
  if (!decoded) return null
  return (
    resolveCanonicalRole({
      canonicalRole: decoded.canonicalRole,
      canonicalRoles: decoded.canonicalRoles,
      role: decoded.role,
      roles: decoded.roles,
      roleAliases: decoded.roleAliases,
    }) ?? null
  )
}
