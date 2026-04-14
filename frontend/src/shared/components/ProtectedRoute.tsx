import { useEffect } from 'react'
import { useAuthStore } from '@/shared/stores/authStore'
import type { UserRole } from '@/shared/types/auth'
import { Spinner } from '@/components/ui/spinner'
import { getRedirectUrlForRole } from '@/shared/lib/jwt'
import { canonicalRoleMatches } from '@/shared/lib/roleCompat'

interface ProtectedRouteProps {
  children: React.ReactNode
  requiredRole: UserRole
}

export function ProtectedRoute({ children, requiredRole }: ProtectedRouteProps) {
  const { isAuthenticated, isLoading, user, checkAuth } = useAuthStore()

  useEffect(() => {
    checkAuth()
  }, [checkAuth])

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }

  if (!isAuthenticated) {
    // Escape basename-scoped routers and return to auth entry.
    window.location.assign('/')
    return null
  }

  if (!canonicalRoleMatches(user?.role, requiredRole)) {
    // User is authenticated but doesn't have the required role
    // Redirect to their appropriate surface.
    const redirectUrl = user?.role ? getRedirectUrlForRole(user.role as UserRole) : '/'
    window.location.assign(redirectUrl)
    return null
  }

  return <>{children}</>
}
