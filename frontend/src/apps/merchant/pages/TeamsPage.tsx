import { useCallback, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Shield, Users, UserPlus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { ToastAction } from '@/components/ui/toast'
import { useToast } from '@/components/ui/use-toast'
import {
  isCanonicalMerchantOwnerRole,
  isCanonicalMerchantRole,
  isCanonicalMerchantStaffRole,
} from '@/shared/lib/roleCompat'
import { useAuthStore } from '@/shared/stores/authStore'
import {
  addMerchantTeamMember,
  createMerchantTeam,
  isMerchantTeamApiError,
  listMerchantTeamMembers,
  listMerchantTeams,
} from '@/shared/lib/merchantTeamApi'
import type { MerchantTeamSummary } from '@/shared/types/merchantTeam'

async function teamsWithMemberCounts(
  merchantId: string,
  teams: MerchantTeamSummary[],
): Promise<MerchantTeamSummary[]> {
  return Promise.all(
    teams.map(async (team) => {
      try {
        const { members } = await listMerchantTeamMembers(merchantId, team.id)
        return { ...team, memberCount: members.length }
      } catch {
        return team
      }
    }),
  )
}

export function TeamsPage() {
  const { t } = useTranslation(['merchant', 'common'])
  const { toast } = useToast()
  const { user } = useAuthStore()
  const [teams, setTeams] = useState<MerchantTeamSummary[]>([])
  const [state, setState] = useState<'loading' | 'ready' | 'empty' | 'error'>('loading')
  const [errorMessage, setErrorMessage] = useState('')

  const [createOpen, setCreateOpen] = useState(false)
  const [createName, setCreateName] = useState('')
  const [createCode, setCreateCode] = useState('')
  const [createBusy, setCreateBusy] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)

  const [addOpen, setAddOpen] = useState(false)
  const [addTeamId, setAddTeamId] = useState('')
  const [addUserId, setAddUserId] = useState('')
  const [addRole, setAddRole] = useState('STAFF')
  const [addBusy, setAddBusy] = useState(false)
  const [addError, setAddError] = useState<string | null>(null)

  const canonicalRole = user?.canonicalRole ?? user?.role
  const canAccessPage = isCanonicalMerchantRole(canonicalRole)
  const isOwner = isCanonicalMerchantOwnerRole(canonicalRole)
  const isStaff = isCanonicalMerchantStaffRole(canonicalRole)

  const roleBadgeKey = useMemo(() => {
    if (isOwner) return 'teams.guard.owner'
    if (isStaff) return 'teams.guard.staff'
    return 'teams.guard.unknown'
  }, [isOwner, isStaff])

  const loadTeams = useCallback(async () => {
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
      const base = result.teams ?? []
      const withCounts = await teamsWithMemberCounts(merchantId, base)
      setTeams(withCounts)
      setState(withCounts.length > 0 ? 'ready' : 'empty')
      setAddTeamId((prev) => {
        if (withCounts.length === 0) return ''
        if (prev && withCounts.some((x) => x.id === prev)) return prev
        return withCounts[0].id
      })
    } catch (error) {
      if (isMerchantTeamApiError(error) && error.status === 404) {
        setErrorMessage(t('teams.states.endpointPending'))
      } else {
        setErrorMessage(t('teams.states.loadError'))
      }
      setState('error')
    }
  }, [canAccessPage, t, user?.tenantId])

  useEffect(() => {
    let mounted = true
    void (async () => {
      await loadTeams()
      if (!mounted) return
    })()
    return () => {
      mounted = false
    }
  }, [loadTeams])

  const openCreate = () => {
    setCreateError(null)
    setCreateName('')
    setCreateCode('')
    setCreateOpen(true)
  }

  const openAddMember = () => {
    setAddError(null)
    setAddUserId('')
    setAddRole('STAFF')
    if (teams.length > 0) {
      setAddTeamId((current) => (teams.some((x) => x.id === current) ? current : teams[0].id))
    }
    setAddOpen(true)
  }

  const submitCreate = async () => {
    const merchantId = user?.tenantId
    if (!merchantId) return
    const name = createName.trim()
    const code = createCode.trim()
    if (!name || !code) {
      setCreateError(t('teams.dialog.create.validation'))
      return
    }
    setCreateBusy(true)
    setCreateError(null)
    try {
      await createMerchantTeam(merchantId, { name, code, status: 'ACTIVE' })
      setCreateOpen(false)
      toast({ title: t('common:status.success'), description: t('teams.actions.createTeam') })
      await loadTeams()
    } catch (e) {
      let message = t('teams.dialog.create.genericError')
      if (isMerchantTeamApiError(e)) {
        message = e.message
        setCreateError(message)
      } else {
        setCreateError(message)
      }
      toast({
        variant: 'destructive',
        title: t('common:errors.generic'),
        description: message,
        action: (
          <ToastAction altText={t('common:actions.retry', { defaultValue: 'Retry' })} onClick={() => void submitCreate()}>
            {t('common:actions.retry', { defaultValue: 'Retry' })}
          </ToastAction>
        ),
      })
    } finally {
      setCreateBusy(false)
    }
  }

  const submitAddMember = async () => {
    const merchantId = user?.tenantId
    if (!merchantId || !addTeamId) {
      setAddError(t('teams.dialog.add.noTeam'))
      return
    }
    setAddBusy(true)
    setAddError(null)
    try {
      await addMerchantTeamMember(merchantId, addTeamId, {
        userId: addUserId.trim(),
        role: addRole.trim() || 'STAFF',
        status: 'ACTIVE',
      })
      setAddOpen(false)
      toast({ title: t('common:status.success'), description: t('teams.actions.addMember') })
      await loadTeams()
    } catch (e) {
      let message = t('teams.dialog.add.genericError')
      if (isMerchantTeamApiError(e)) {
        message = e.message
        setAddError(message)
      } else {
        setAddError(message)
      }
      toast({
        variant: 'destructive',
        title: t('common:errors.generic'),
        description: message,
        action: (
          <ToastAction
            altText={t('common:actions.retry', { defaultValue: 'Retry' })}
            onClick={() => void submitAddMember()}
          >
            {t('common:actions.retry', { defaultValue: 'Retry' })}
          </ToastAction>
        ),
      })
    } finally {
      setAddBusy(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">{t('teams.title')}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t('teams.description')}</p>
        </div>
        <div className="flex flex-col items-stretch gap-2 sm:items-end">
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant="outline" className="gap-1">
              <Shield className="h-3.5 w-3.5" />
              {t(roleBadgeKey)}
            </Badge>
            <Button type="button" className="gap-2" disabled={!isOwner} onClick={() => void openCreate()}>
              <Users className="h-4 w-4" />
              {t('teams.actions.createTeam')}
            </Button>
            <Button
              type="button"
              variant="outline"
              className="gap-2"
              disabled={!isOwner}
              onClick={() => void openAddMember()}
            >
              <UserPlus className="h-4 w-4" />
              {t('teams.actions.addMember')}
            </Button>
          </div>
          {isStaff && canAccessPage ? (
            <p className="max-w-md text-right text-xs text-muted-foreground">{t('teams.actions.ownerOnly')}</p>
          ) : null}
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

      {createOpen ? (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
          role="presentation"
          onClick={() => !createBusy && setCreateOpen(false)}
        >
          <Card
            className="w-full max-w-md shadow-lg"
            onClick={(e) => e.stopPropagation()}
          >
            <CardHeader>
              <CardTitle>{t('teams.dialog.create.title')}</CardTitle>
              <CardDescription>{t('teams.dialog.create.description')}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {createError ? <p className="text-sm text-destructive">{createError}</p> : null}
              <div className="space-y-2">
                <Label htmlFor="team-name">{t('teams.dialog.create.name')}</Label>
                <Input
                  id="team-name"
                  value={createName}
                  onChange={(e) => setCreateName(e.target.value)}
                  disabled={createBusy}
                  autoComplete="off"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="team-code">{t('teams.dialog.create.code')}</Label>
                <Input
                  id="team-code"
                  value={createCode}
                  onChange={(e) => setCreateCode(e.target.value)}
                  disabled={createBusy}
                  maxLength={80}
                  autoComplete="off"
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <Button type="button" variant="outline" disabled={createBusy} onClick={() => setCreateOpen(false)}>
                  {t('common:actions.cancel')}
                </Button>
                <Button type="button" disabled={createBusy} onClick={() => void submitCreate()}>
                  {createBusy ? t('common:status.loading') : t('teams.dialog.create.submit')}
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      ) : null}

      {addOpen ? (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
          role="presentation"
          onClick={() => !addBusy && setAddOpen(false)}
        >
          <Card
            className="w-full max-w-md shadow-lg"
            onClick={(e) => e.stopPropagation()}
          >
            <CardHeader>
              <CardTitle>{t('teams.dialog.add.title')}</CardTitle>
              <CardDescription>{t('teams.dialog.add.description')}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {teams.length === 0 ? (
                <p className="text-sm text-muted-foreground">{t('teams.dialog.add.noTeam')}</p>
              ) : null}
              {addError ? <p className="text-sm text-destructive">{addError}</p> : null}
              {teams.length > 0 ? (
                <div className="space-y-2">
                  <Label>{t('teams.dialog.add.team')}</Label>
                  <Select value={addTeamId} onValueChange={setAddTeamId} disabled={addBusy}>
                    <SelectTrigger className="w-full">
                      <SelectValue placeholder={t('teams.dialog.add.team')} />
                    </SelectTrigger>
                    <SelectContent>
                      {teams.map((team) => (
                        <SelectItem key={team.id} value={team.id}>
                          {team.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              ) : null}
              <div className="space-y-2">
                <Label htmlFor="member-user-id">{t('teams.dialog.add.userId')}</Label>
                <Input
                  id="member-user-id"
                  inputMode="numeric"
                  value={addUserId}
                  onChange={(e) => setAddUserId(e.target.value)}
                  disabled={addBusy || teams.length === 0}
                  placeholder={t('teams.dialog.add.userIdPlaceholder')}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="member-role">{t('teams.dialog.add.role')}</Label>
                <Input
                  id="member-role"
                  value={addRole}
                  onChange={(e) => setAddRole(e.target.value)}
                  disabled={addBusy || teams.length === 0}
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <Button type="button" variant="outline" disabled={addBusy} onClick={() => setAddOpen(false)}>
                  {t('common:actions.cancel')}
                </Button>
                <Button
                  type="button"
                  disabled={addBusy || teams.length === 0}
                  onClick={() => void submitAddMember()}
                >
                  {addBusy ? t('common:status.loading') : t('teams.dialog.add.submit')}
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      ) : null}
    </div>
  )
}
