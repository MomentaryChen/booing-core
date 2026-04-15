import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { MerchantLayout } from './layouts/MerchantLayout'
import { DashboardPage } from './pages/DashboardPage'
import { ResourcesPage } from './pages/ResourcesPage'
import { SchedulePage } from './pages/SchedulePage'
import { BookingsPage } from './pages/BookingsPage'
import { SettingsPage } from './pages/SettingsPage'
import { TeamsPage } from './pages/TeamsPage'
import { Spinner } from '@/components/ui/spinner'
import { Button } from '@/components/ui/button'
import { useAuthStore } from '@/shared/stores/authStore'
import { fetchAuthMe, isApiError, switchAuthContext } from '@/shared/lib/authContextApi'
import { getRedirectUrlForRole } from '@/shared/lib/jwt'
import { isCanonicalClientRole, isCanonicalMerchantRole } from '@/shared/lib/roleCompat'
import type { AuthContextOption } from '@/shared/types/authContext'
import type { UserRole } from '@/shared/types/auth'
import { BootstrapGatePage } from './pages/BootstrapGatePage'
import { Toaster } from '@/components/ui/toaster'

function MerchantEntryGuard({ children }: { children: React.ReactNode }) {
  const { t } = useTranslation('merchant')
  const { isAuthenticated, isLoading, user, checkAuth, login } = useAuthStore()
  const [bootstrapState, setBootstrapState] = useState<'loading' | 'ready' | 'forbidden' | 'error'>('loading')
  const [hasMerchantBootstrap, setHasMerchantBootstrap] = useState(false)
  const [merchantContextOption, setMerchantContextOption] = useState<AuthContextOption | null>(null)
  const [activeMerchantContext, setActiveMerchantContext] = useState(false)
  const [switchingContext, setSwitchingContext] = useState(false)
  const [contextSwitchError, setContextSwitchError] = useState('')

  useEffect(() => {
    checkAuth()
  }, [checkAuth])

  useEffect(() => {
    let mounted = true
    const loadBootstrap = async () => {
      if (!isAuthenticated || !user) return
      try {
        setBootstrapState('loading')
        const me = await fetchAuthMe()
        if (!mounted) return
        const hasMerchantContext = (me.availableContexts ?? []).some(
          (ctx) => isCanonicalMerchantRole(ctx.canonicalRole ?? ctx.role) && ctx.merchantId != null
        )
        const preferredMerchantContext =
          (me.availableContexts ?? []).find(
            (ctx) => isCanonicalMerchantRole(ctx.canonicalRole ?? ctx.role) && ctx.merchantId != null
          ) ?? null
        const isActiveMerchant =
          !!me.activeContext &&
          isCanonicalMerchantRole(me.activeContext.canonicalRole ?? me.activeContext.role) &&
          me.activeContext.merchantId != null
        setHasMerchantBootstrap(hasMerchantContext)
        setMerchantContextOption(preferredMerchantContext)
        setActiveMerchantContext(isActiveMerchant)
        setBootstrapState('ready')
      } catch (err) {
        if (!mounted) return
        if (isApiError(err) && err.status === 403) {
          setBootstrapState('forbidden')
        } else {
          setBootstrapState('error')
        }
      }
    }
    void loadBootstrap()
    return () => {
      mounted = false
    }
  }, [isAuthenticated, user])

  if (isLoading || bootstrapState === 'loading') {
    return (
      <div className="flex h-screen items-center justify-center">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }

  if (!isAuthenticated || !user) {
    window.location.assign('/')
    return null
  }

  if (bootstrapState === 'forbidden') {
    return (
      <div className="flex h-screen items-center justify-center px-4 text-center text-destructive">
        {t('bootstrap.accessForbidden')}
      </div>
    )
  }

  if (bootstrapState === 'error') {
    return (
      <div className="flex h-screen items-center justify-center px-4 text-center text-destructive">
        {t('bootstrap.loadError')}
      </div>
    )
  }

  if (hasMerchantBootstrap) {
    if (!activeMerchantContext) {
      const handleSwitchToMerchantContext = async () => {
        if (!merchantContextOption || switchingContext) return
        setSwitchingContext(true)
        setContextSwitchError('')
        try {
          const token = await switchAuthContext({
            role: merchantContextOption.role,
            merchantId: merchantContextOption.merchantId,
          })
          login(token.accessToken)
          window.location.assign('/merchant')
        } catch (err) {
          if (isApiError(err) && err.status === 403) {
            setContextSwitchError(t('contextGate.switchForbidden'))
          } else {
            setContextSwitchError(t('contextGate.switchError'))
          }
        } finally {
          setSwitchingContext(false)
        }
      }

      return (
        <div className="flex h-screen items-center justify-center px-4">
          <div className="w-full max-w-md rounded-lg border bg-card p-6 shadow-sm">
            <h1 className="text-lg font-semibold">{t('contextGate.title')}</h1>
            <p className="mt-2 text-sm text-muted-foreground">{t('contextGate.description')}</p>
            <div className="mt-4 flex flex-col gap-2 sm:flex-row">
              <Button onClick={handleSwitchToMerchantContext} disabled={switchingContext}>
                {switchingContext ? t('contextGate.switching') : t('contextGate.switchCta')}
              </Button>
              <Button variant="outline" onClick={() => window.location.assign('/client')}>
                {t('contextGate.backToClient')}
              </Button>
            </div>
            {contextSwitchError ? (
              <p className="mt-3 text-sm text-destructive">{contextSwitchError}</p>
            ) : null}
          </div>
        </div>
      )
    }

    return <>{children}</>
  }

  if (isCanonicalClientRole(user.canonicalRole ?? user.role)) {
    return <BootstrapGatePage onEnabled={() => window.location.reload()} />
  }

  const redirectUrl = getRedirectUrlForRole(user.role as UserRole)
  window.location.assign(redirectUrl)
  return null
}

export function MerchantApp() {
  return (
    <BrowserRouter basename="/merchant">
      <>
        <Routes>
          <Route
            element={
              <MerchantEntryGuard>
                <MerchantLayout />
              </MerchantEntryGuard>
            }
          >
            <Route index element={<DashboardPage />} />
            <Route path="resources" element={<ResourcesPage />} />
            <Route path="schedule" element={<SchedulePage />} />
            <Route path="bookings" element={<BookingsPage />} />
            <Route path="teams" element={<TeamsPage />} />
            <Route path="settings" element={<SettingsPage />} />
            <Route path="*" element={<Navigate to="." replace />} />
          </Route>
        </Routes>
        <Toaster />
      </>
    </BrowserRouter>
  )
}
