import type { UserRole } from '@/shared/types/auth'

const LEGACY_TO_CANONICAL: Record<string, UserRole> = {
  CLIENT: 'CLIENT_USER',
  CLIENT_USER: 'CLIENT_USER',
  MERCHANT: 'MERCHANT_OWNER',
  MERCHANT_OWNER: 'MERCHANT_OWNER',
  SUB_MERCHANT: 'MERCHANT_STAFF',
  MERCHANT_STAFF: 'MERCHANT_STAFF',
  ADMIN: 'SYSTEM_ADMIN',
  SYSTEM_ADMIN: 'SYSTEM_ADMIN',
}

export function toCanonicalRole(role: string | null | undefined): UserRole | null {
  if (!role) return null
  return LEGACY_TO_CANONICAL[role] ?? null
}

export function resolveCanonicalRole(input: {
  canonicalRole?: string | null
  canonicalRoles?: string[] | null
  role?: string | null
  roles?: string[] | null
  roleAliases?: string[] | null
}): UserRole | null {
  const orderedCandidates = [
    input.canonicalRole,
    ...(input.canonicalRoles ?? []),
    input.role,
    ...(input.roles ?? []),
    ...(input.roleAliases ?? []),
  ]

  for (const candidate of orderedCandidates) {
    const canonical = toCanonicalRole(candidate)
    if (canonical) return canonical
  }
  return null
}

export function canonicalRoleMatches(actualRole: string | null | undefined, requiredRole: UserRole): boolean {
  const canonicalActual = toCanonicalRole(actualRole)
  const canonicalRequired = toCanonicalRole(requiredRole)
  if (!canonicalActual || !canonicalRequired) return false
  return canonicalActual === canonicalRequired
}

export function isCanonicalMerchantRole(role: string | null | undefined): boolean {
  const canonical = toCanonicalRole(role)
  return canonical === 'MERCHANT_OWNER' || canonical === 'MERCHANT_STAFF'
}

export function isCanonicalMerchantOwnerRole(role: string | null | undefined): boolean {
  return toCanonicalRole(role) === 'MERCHANT_OWNER'
}

export function isCanonicalMerchantStaffRole(role: string | null | undefined): boolean {
  return toCanonicalRole(role) === 'MERCHANT_STAFF'
}

export function isCanonicalClientRole(role: string | null | undefined): boolean {
  return toCanonicalRole(role) === 'CLIENT_USER'
}
