import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Shield, Users, UserPlus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import {
  isCanonicalMerchantOwnerRole,
  isCanonicalMerchantRole,
  isCanonicalMerchantStaffRole,
} from '@/shared/lib/roleCompat'
import { useAuthStore } from '@/shared/stores/authStore'
import { isMerchantTeamApiError, listMerchantTeams } from '@/shared/lib/merchantTeamApi'
import type { MerchantTeamSummary } from '@/shared/types/merchantTeam'

export function TeamsPage() {
  const { t } = useTranslation(['merchant', 'common'])
  const { user } = useAuthStore()
  const [teams, setTeams] = useState<MerchantTeamSummary[]>([])
  const [state, setState] = useState<'loading' | 'ready' | 'empty' | 'error'>('loading')
  const [errorMessage, setErrorMessage] = useState('')

  const canonicalRole = user?.canonicalRole ?? user?.role
  const canAccessPage = isCanonicalMerchantRole(canonicalRole)
  const isOwner = isCanonicalMerchantOwnerRole(canonicalRole)
  const isStaff = isCanonicalMerchantStaffRole(canonicalRole)

  const roleBadgeKey = useMemo(() => {
    if (isOwner) return 'teams.guard.owner'
    if (isStaff) return 'teams.guard.staff'
    return 'teams.guard.unknown'
  }, [isOwner, isStaff])

  useEffect(() => {
    let mounted = true

    const loadTeams = async () => {
      if (!canAccessPage) {
        setState('error')
        setErrorMessage(t('teams.states.forbidden'))
        return
      }

      const merchantId = user?.tenantId
      if (!merchantId) {
        setState('error')
        setErrorMessage(t('teams.states.merchantMissing'))
        return
      }

      try {
        setState('loading')
        setErrorMessage('')
        const result = await listMerchantTeams(merchantId)
        if (!mounted) return
        const data = result.teams ?? []
        setTeams(data)
        setState(data.length > 0 ? 'ready' : 'empty')
      } catch (error) {
        if (!mounted) return
        if (isMerchantTeamApiError(error) && error.status === 404) {
          setErrorMessage(t('teams.states.endpointPending'))
        } else {
          setErrorMessage(t('teams.states.loadError'))
        }
        setState('error')
      }
    }

    void loadTeams()
    return () => {
      mounted = false
    }
  }, [canAccessPage, t, user?.tenantId])

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">{t('teams.title')}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t('teams.description')}</p>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant="outline" className="gap-1">
            <Shield className="h-3.5 w-3.5" />
            {t(roleBadgeKey)}
          </Badge>
          <Button className="gap-2" disabled={!isOwner}>
            <Users className="h-4 w-4" />
            {t('teams.actions.createTeam')}
          </Button>
          <Button variant="outline" className="gap-2" disabled={!isOwner}>
            <UserPlus className="h-4 w-4" />
            {t('teams.actions.addMember')}
          </Button>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>{t('teams.list.title')}</CardTitle>
          <CardDescription>{t('teams.list.description')}</CardDescription>
        </CardHeader>
        <CardContent>
          {state === 'loading' ? <p className="text-sm text-muted-foreground">{t('common:status.loading')}</p> : null}
          {state === 'empty' ? <p className="text-sm text-muted-foreground">{t('teams.states.empty')}</p> : null}
          {state === 'error' ? <p className="text-sm text-destructive">{errorMessage}</p> : null}

          {state === 'ready' ? (
            <div className="space-y-3">
              {teams.map((team) => (
                <div
                  key={team.id}
                  className="flex items-center justify-between rounded-md border px-3 py-2"
                >
                  <div>
                    <p className="font-medium">{team.name}</p>
                    <p className="text-sm text-muted-foreground">
                      {t('teams.list.memberCount', { count: team.memberCount })}
                    </p>
                  </div>
                  <Badge variant={team.active ? 'default' : 'secondary'}>
                    {team.active ? t('teams.states.active') : t('teams.states.inactive')}
                  </Badge>
                </div>
              ))}
            </div>
          ) : null}
        </CardContent>
      </Card>
    </div>
  )
}

