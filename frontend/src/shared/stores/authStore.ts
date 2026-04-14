import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { User, UserRole } from '@/shared/types/auth'
import type { AuthMeResponse } from '@/shared/types/authContext'
import { decodeToken, isTokenExpired, getRedirectUrlForRole } from '@/shared/lib/jwt'
import { canonicalRoleMatches, resolveCanonicalRole } from '@/shared/lib/roleCompat'
import { AUTH_TOKEN_KEY } from '@/shared/lib/constants'

interface AuthState {
  token: string | null
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean

  // Actions
  login: (token: string) => void
  applyMeResponse: (me: AuthMeResponse) => void
  logout: () => void
  checkAuth: () => boolean
  getRedirectUrl: () => string
  hasRole: (role: UserRole) => boolean
  hasPermission: (permission: string) => boolean
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      user: null,
      isAuthenticated: false,
      isLoading: true,

      login: (token: string) => {
        const decoded = decodeToken(token)
        if (!decoded || isTokenExpired(token)) {
          set({ token: null, user: null, isAuthenticated: false, isLoading: false })
          return
        }

        const resolvedFromToken =
          resolveCanonicalRole({
            canonicalRole: decoded.canonicalRole,
            canonicalRoles: decoded.canonicalRoles,
            role: typeof decoded.role === 'string' ? decoded.role : undefined,
            roles: decoded.roles,
            roleAliases: decoded.roleAliases,
          }) ?? (typeof decoded.role === 'string' ? resolveCanonicalRole({ role: decoded.role }) : null)

        const effectiveRole = resolvedFromToken ?? (decoded.role as UserRole)

        const tenantFromJwt =
          decoded.tenantId ?? (decoded.merchantId != null ? String(decoded.merchantId) : undefined)

        const user: User = {
          id: decoded.sub,
          email: decoded.email ?? decoded.sub,
          name: decoded.name ?? decoded.sub,
          role: effectiveRole,
          canonicalRole: resolvedFromToken ?? undefined,
          canonicalRoles: (decoded.canonicalRoles ?? [])
            .map((role) => resolveCanonicalRole({ role }))
            .filter((role): role is UserRole => role != null),
          roleAliases: decoded.roleAliases ?? [],
          tenantId: tenantFromJwt,
          permissions: decoded.permissions ?? [],
        }

        set({ token, user, isAuthenticated: true, isLoading: false })
      },

      applyMeResponse: (me: AuthMeResponse) => {
        const { token } = get()
        if (!token) return
        const effectiveRole =
          resolveCanonicalRole({
            canonicalRole: me.canonicalRole,
            canonicalRoles: me.canonicalRoles,
            role: me.role,
            roles: me.roles,
            roleAliases: me.roleAliases,
          }) ?? resolveCanonicalRole({ role: me.role })

        if (!effectiveRole) return

        const user: User = {
          id: me.username,
          email: me.username,
          name: me.username,
          role: effectiveRole,
          canonicalRole:
            resolveCanonicalRole({
              canonicalRole: me.canonicalRole,
              canonicalRoles: me.canonicalRoles,
              role: me.role,
              roles: me.roles,
              roleAliases: me.roleAliases,
            }) ?? undefined,
          canonicalRoles: (me.canonicalRoles ?? [])
            .map((role) => resolveCanonicalRole({ role }))
            .filter((role): role is UserRole => role != null),
          roleAliases: me.roleAliases ?? [],
          tenantId: me.merchantId != null ? String(me.merchantId) : undefined,
          permissions: me.permissions ?? [],
        }

        set({ token, user, isAuthenticated: true, isLoading: false })
      },

      logout: () => {
        set({ token: null, user: null, isAuthenticated: false, isLoading: false })
        // Redirect to login page
        window.location.href = '/'
      },

      checkAuth: () => {
        const { token } = get()
        if (!token || isTokenExpired(token)) {
          set({ token: null, user: null, isAuthenticated: false, isLoading: false })
          return false
        }
        set({ isLoading: false })
        return true
      },

      getRedirectUrl: () => {
        const { user } = get()
        if (!user) return '/'
        return getRedirectUrlForRole(user.role)
      },

      hasRole: (role: UserRole) => {
        const { user } = get()
        return canonicalRoleMatches(user?.role, role)
      },

      hasPermission: (permission: string) => {
        const { user } = get()
        if (!user) return false
        // Admin has all permissions
        if (user.permissions.includes('*')) return true
        return user.permissions.includes(permission)
      },
    }),
    {
      name: AUTH_TOKEN_KEY,
      partialize: (state) => ({ token: state.token }),
      onRehydrateStorage: () => (state) => {
        if (state?.token) {
          state.login(state.token)
        } else {
          state?.checkAuth()
        }
      },
    }
  )
)
