import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ChevronsUpDown, RefreshCw } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { fetchAuthMe, isApiError, switchAuthContext } from '@/shared/lib/authContextApi'
import type { AuthContextOption } from '@/shared/types/authContext'
import { useAuthStore } from '@/shared/stores/authStore'
import { getRedirectUrlForRole } from '@/shared/lib/jwt'
import type { UserRole } from '@/shared/types/auth'

function contextKey(option: AuthContextOption): string {
  return `${option.role}::${option.merchantId ?? 'none'}`
}

function parseContextKey(key: string): { role: string; merchantId: number | null } {
  const [role, merchantIdRaw] = key.split('::')
  return { role, merchantId: merchantIdRaw === 'none' ? null : Number(merchantIdRaw) }
}

export function AuthContextSwitcher() {
  const { t } = useTranslation('common')
  const { user, login } = useAuthStore()
  const [contexts, setContexts] = useState<AuthContextOption[]>([])
  const [selected, setSelected] = useState<string>('')
  const [loading, setLoading] = useState(true)
  const [switching, setSwitching] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let mounted = true
    const load = async () => {
      try {
        setLoading(true)
        setError('')
        const me = await fetchAuthMe()
        if (!mounted) return
        const available = me.availableContexts ?? []
        setContexts(available)
        const active = me.activeContext
        if (active) {
          setSelected(contextKey(active))
        } else if (available.length > 0) {
          setSelected(contextKey(available[0]))
        }
      } catch (err) {
        if (!mounted) return
        if (isApiError(err) && err.status === 403) {
          setError(t('context.forbidden'))
        } else {
          setError(t('context.error'))
        }
      } finally {
        if (mounted) setLoading(false)
      }
    }
    void load()
    return () => {
      mounted = false
    }
  }, [t])

  const hasMultipleContexts = contexts.length > 1
  const getContextLabel = (ctx: AuthContextOption): string => {
    const tenantPart = ctx.merchantId == null ? t('context.platformScope') : `#${ctx.merchantId}`
    const roleLabel = t(`roles.${ctx.canonicalRole ?? ctx.role}`, ctx.canonicalRole ?? ctx.role)
    return `${roleLabel} · ${tenantPart}`
  }
  const selectedLabel = useMemo(() => {
    const selectedContext = contexts.find((ctx) => contextKey(ctx) === selected)
    if (!selectedContext) return ''
    return getContextLabel(selectedContext)
  }, [contexts, selected, t])

  if (!user || loading || !hasMultipleContexts) {
    return null
  }

  const handleSwitch = async (next: string) => {
    if (next === selected || switching) return
    setSwitching(true)
    setError('')
    try {
      const payload = parseContextKey(next)
      const token = await switchAuthContext(payload)
      login(token.accessToken)
      setSelected(next)
      setMobileOpen(false)
      window.location.assign(getRedirectUrlForRole(payload.role as UserRole))
    } catch (err) {
      if (isApiError(err) && err.status === 403) {
        setError(t('context.forbidden'))
      } else {
        setError(t('context.error'))
      }
    } finally {
      setSwitching(false)
    }
  }

  return (
    <div className="flex items-center gap-2">
      <div className="hidden md:flex items-center gap-2">
        <span className="text-xs text-muted-foreground">{t('context.label')}</span>
        <Select value={selected} onValueChange={handleSwitch} disabled={switching}>
          <SelectTrigger className="h-8 w-[230px]">
            <SelectValue placeholder={t('context.placeholder')}>
              {switching ? (
                <span className="inline-flex items-center gap-1">
                  <RefreshCw className="h-3 w-3 animate-spin" />
                  {t('context.switching')}
                </span>
              ) : (
                selectedLabel
              )}
            </SelectValue>
          </SelectTrigger>
          <SelectContent>
            {contexts.map((ctx) => (
              <SelectItem key={contextKey(ctx)} value={contextKey(ctx)}>
                {getContextLabel(ctx)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <DropdownMenu open={mobileOpen} onOpenChange={setMobileOpen}>
        <DropdownMenuTrigger asChild>
          <Button
            type="button"
            variant="outline"
            size="icon"
            className="md:hidden h-9 w-9"
            aria-label={t('context.mobileTrigger')}
          >
            {switching ? (
              <RefreshCw className="h-4 w-4 animate-spin" />
            ) : (
              <ChevronsUpDown className="h-4 w-4" />
            )}
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-72 md:hidden">
          <DropdownMenuLabel>{t('context.mobileTitle')}</DropdownMenuLabel>
          <p className="px-2 pb-2 text-xs text-muted-foreground">{t('context.mobileDescription')}</p>
          {contexts.map((ctx) => {
            const key = contextKey(ctx)
            const isActive = key === selected
            return (
              <DropdownMenuItem
                key={key}
                disabled={switching || isActive}
                onSelect={(event) => {
                  event.preventDefault()
                  void handleSwitch(key)
                }}
              >
                {getContextLabel(ctx)}
              </DropdownMenuItem>
            )
          })}
        </DropdownMenuContent>
      </DropdownMenu>

      {error ? (
        <Button variant="ghost" size="sm" className="text-destructive pointer-events-none">
          {error}
        </Button>
      ) : null}
    </div>
  )
}
