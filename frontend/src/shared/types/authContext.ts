export type PlatformRole =
  | 'CLIENT'
  | 'CLIENT_USER'
  | 'MERCHANT'
  | 'MERCHANT_OWNER'
  | 'SUB_MERCHANT'
  | 'MERCHANT_STAFF'
  | 'SYSTEM_ADMIN'
  | 'ADMIN'

export interface AuthContextOption {
  kind: string
  merchantId: number | null
  role: string
  canonicalRole?: string
  canonicalRoles?: string[]
  roleAliases?: string[]
}

export interface AuthMeResponse {
  username: string
  role: string
  roles: string[]
  canonicalRole: string
  canonicalRoles: string[]
  roleAliases: string[]
  permissions: string[]
  merchantId: number | null
  sessionState: string
  availableContexts: AuthContextOption[]
  activeContext: AuthContextOption | null
}

export interface ContextSelectRequest {
  merchantId: number | null
  role: string
}

export interface MerchantEnableRequest {
  name: string
  slug: string
}

export interface MerchantEnableResponse {
  merchantId: number
  name: string
  slug: string
  ownerRole: string
  membershipStatus: string
}

export interface TokenResponse {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
  role: string
  roles: string[]
  canonicalRole: string
  canonicalRoles: string[]
  roleAliases: string[]
  permissions: string[]
}

/** Matches {@code POST /api/auth/register} body (server allowlist on registerType). */
export type PublicRegisterType = 'MERCHANT' | 'CLIENT'

export interface PublicRegisterRequestBody {
  registerType: PublicRegisterType
  name?: string | null
  slug?: string | null
  username?: string | null
  password?: string | null
}

export interface PublicRegisterResponse {
  id: number
  name: string
  slug: string
  active: boolean
  /** Server-fixed path; safe to pass to client router (no open redirect). */
  nextDestination: string
}
