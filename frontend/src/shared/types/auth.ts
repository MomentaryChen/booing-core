export type UserRole =
  | 'CLIENT'
  | 'CLIENT_USER'
  | 'MERCHANT'
  | 'MERCHANT_OWNER'
  | 'SUB_MERCHANT'
  | 'MERCHANT_STAFF'
  | 'SYSTEM_ADMIN'
  | 'ADMIN'

export interface User {
  id: string
  email: string
  name: string
  role: UserRole
  canonicalRole?: UserRole
  canonicalRoles?: UserRole[]
  roleAliases?: string[]
  tenantId?: string
  permissions: string[]
  avatar?: string
}

export interface LoginCredentials {
  email: string
  password: string
}

export interface RegisterData {
  email: string
  password: string
  name: string
  role?: UserRole
}

export interface AuthResponse {
  token: string
  user: User
  canonicalRole?: UserRole
  canonicalRoles?: UserRole[]
  roleAliases?: string[]
}
