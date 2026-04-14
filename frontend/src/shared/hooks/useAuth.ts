import { useAuthStore } from '@/shared/stores/authStore'
import { getRedirectUrlForRole } from '@/shared/lib/jwt'
import type { LoginCredentials, UserRole } from '@/shared/types/auth'
import {
  fetchAuthMe,
  isApiError,
  loginWithPassword,
  postLogout,
} from '@/shared/lib/authContextApi'

export function useAuth() {
  const { login, logout, applyMeResponse, user, isAuthenticated, isLoading, hasRole, hasPermission } =
    useAuthStore()

  const loginWithCredentials = async (credentials: LoginCredentials): Promise<boolean> => {
    const loginId = credentials.email.trim()
    const password = credentials.password

    try {
      const tokenResponse = await loginWithPassword(loginId, password)
      login(tokenResponse.accessToken)
      try {
        const me = await fetchAuthMe()
        applyMeResponse(me)
      } catch {
        // JWT-derived user remains; /me is best-effort hydration.
      }
      const r = useAuthStore.getState().user?.role ?? ('CLIENT' as UserRole)
      window.location.href = getRedirectUrlForRole(r)
      return true
    } catch (err) {
      if (isApiError(err) && err.status === 400 && err.message.includes('JWT auth is not enabled')) {
        console.warn('booking-core: backend JWT is not enabled; set booking.jwt.secret (JWT_SECRET).')
      }
      return false
    }
  }

  const logoutAndRedirect = async () => {
    const { token } = useAuthStore.getState()
    if (token) {
      try {
        await postLogout()
      } catch {
        // Still clear local session.
      }
    }
    logout()
  }

  return {
    user,
    isAuthenticated,
    isLoading,
    login,
    logout: logoutAndRedirect,
    loginWithCredentials,
    hasRole,
    hasPermission,
  }
}
