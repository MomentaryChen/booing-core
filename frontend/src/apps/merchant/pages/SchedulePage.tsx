import { useCallback, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Plus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Switch } from '@/components/ui/switch'
import { Label } from '@/components/ui/label'
import { Input } from '@/components/ui/input'
import { Separator } from '@/components/ui/separator'
import { useAuthStore } from '@/shared/stores/authStore'
import {
  createMerchantAvailabilityException,
  deleteMerchantAvailabilityException,
  fetchMerchantAvailabilityExceptions,
  fetchMerchantBusinessHours,
  isMerchantPortalApiError,
  putMerchantBusinessHours,
  type MerchantAvailabilityExceptionDto,
} from '@/shared/lib/merchantPortalApi'

const dayDefs = [
  { key: 'monday', api: 'MONDAY' },
  { key: 'tuesday', api: 'TUESDAY' },
  { key: 'wednesday', api: 'WEDNESDAY' },
  { key: 'thursday', api: 'THURSDAY' },
  { key: 'friday', api: 'FRIDAY' },
  { key: 'saturday', api: 'SATURDAY' },
  { key: 'sunday', api: 'SUNDAY' },
] as const

export function SchedulePage() {
  const { t } = useTranslation(['merchant', 'common'])
  const merchantId = Number(useAuthStore((s) => s.user?.tenantId))
  const [schedule, setSchedule] = useState(
    dayDefs.map((d) => ({ day: d.key, apiDay: d.api, enabled: false, start: '09:00', end: '18:00' })),
  )
  const [exceptions, setExceptions] = useState<MerchantAvailabilityExceptionDto[]>([])
  const [newExceptionDate, setNewExceptionDate] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadSchedule = useCallback(async () => {
    if (!merchantId) return
    setLoading(true)
    setError('')
    try {
      const [hours, ex] = await Promise.all([
        fetchMerchantBusinessHours(merchantId),
        fetchMerchantAvailabilityExceptions(merchantId),
      ])
      const map = new Map(hours.map((h) => [h.dayOfWeek, h]))
      setSchedule(
        dayDefs.map((d) => {
          const current = map.get(d.api)
          return {
            day: d.key,
            apiDay: d.api,
            enabled: Boolean(current),
            start: current?.startTime?.slice(0, 5) ?? '09:00',
            end: current?.endTime?.slice(0, 5) ?? '18:00',
          }
        }),
      )
      setExceptions(ex)
    } catch (e) {
      setError(isMerchantPortalApiError(e) ? e.message : t('common:errors.generic'))
    } finally {
      setLoading(false)
    }
  }, [merchantId, t])

  useEffect(() => {
    void loadSchedule()
  }, [loadSchedule])

  const toggleDay = (dayIndex: number) => {
    setSchedule((current) => {
      const next = [...current]
      next[dayIndex] = { ...next[dayIndex], enabled: !next[dayIndex].enabled }
      return next
    })
  }

  const updateDayTime = (dayIndex: number, field: 'start' | 'end', value: string) => {
    setSchedule((current) => {
      const next = [...current]
      next[dayIndex] = { ...next[dayIndex], [field]: value }
      return next
    })
  }

  const handleSaveHours = async () => {
    if (!merchantId) return
    setError('')
    try {
      const payload = schedule
        .filter((d) => d.enabled)
        .map((d) => ({ dayOfWeek: d.apiDay, startTime: d.start, endTime: d.end }))
      await putMerchantBusinessHours(merchantId, payload)
      await loadSchedule()
    } catch (e) {
      setError(isMerchantPortalApiError(e) ? e.message : t('common:errors.generic'))
    }
  }

  const handleAddException = async () => {
    if (!merchantId || !newExceptionDate) return
    setError('')
    try {
      await createMerchantAvailabilityException(merchantId, {
        type: 'CLOSED',
        startAt: `${newExceptionDate}T00:00:00`,
        endAt: `${newExceptionDate}T23:59:59`,
        reason: 'holiday',
      })
      setNewExceptionDate('')
      await loadSchedule()
    } catch (e) {
      setError(isMerchantPortalApiError(e) ? e.message : t('common:errors.generic'))
    }
  }

  const handleDeleteException = async (exceptionId: number) => {
    if (!merchantId) return
    setError('')
    try {
      await deleteMerchantAvailabilityException(merchantId, exceptionId)
      await loadSchedule()
    } catch (e) {
      setError(isMerchantPortalApiError(e) ? e.message : t('common:errors.generic'))
    }
  }

  const normalizedExceptions = useMemo(
    () =>
      exceptions.map((item) => ({
        id: item.id,
        date: item.startAt.slice(0, 10),
        type: item.type.toLowerCase(),
        reason: item.reason ?? '',
        start: item.startAt.slice(11, 16),
        end: item.endAt.slice(11, 16),
      })),
    [exceptions],
  )

  if (!merchantId) {
    return <p className="text-sm text-muted-foreground">{t('bookings.noMerchantContext')}</p>
  }

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">{t('schedule.title')}</h1>
      {loading ? <p className="text-sm text-muted-foreground">{t('common:status.loading')}</p> : null}
      {error ? <p className="text-sm text-destructive">{error}</p> : null}

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Working Hours */}
        <Card>
          <CardHeader>
            <CardTitle>{t('schedule.workingHours')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {schedule.map((day, index) => (
              <div key={day.day} className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Switch
                      checked={day.enabled}
                      onCheckedChange={() => toggleDay(index)}
                    />
                    <Label className="font-medium w-24">
                      {t(`schedule.days.${day.day}`)}
                    </Label>
                  </div>
                  {day.enabled && (
                    <div className="flex items-center gap-2">
                      <Input
                        type="time"
                        value={day.start}
                        onChange={(e) => updateDayTime(index, 'start', e.target.value)}
                        className="w-28"
                        disabled={!day.enabled}
                      />
                      <span className="text-muted-foreground">-</span>
                      <Input
                        type="time"
                        value={day.end}
                        onChange={(e) => updateDayTime(index, 'end', e.target.value)}
                        className="w-28"
                        disabled={!day.enabled}
                      />
                    </div>
                  )}
                </div>
              </div>
            ))}
            <Separator />
            <Button className="w-full" onClick={() => void handleSaveHours()}>
              {t('common:actions.save')}
            </Button>
          </CardContent>
        </Card>

        {/* Exceptions */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle>{t('schedule.exceptions')}</CardTitle>
            <div className="flex items-center gap-2">
              <Input
                type="date"
                value={newExceptionDate}
                onChange={(e) => setNewExceptionDate(e.target.value)}
                className="h-8"
              />
              <Button size="sm" className="gap-2" onClick={() => void handleAddException()}>
              <Plus className="h-4 w-4" />
              {t('schedule.addException')}
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            {normalizedExceptions.length > 0 ? (
              <div className="space-y-3">
                {normalizedExceptions.map((exception) => (
                  <div
                    key={exception.id}
                    className="flex items-center justify-between rounded-lg border p-3"
                  >
                    <div>
                      <p className="font-medium">{exception.date}</p>
                      <p className="text-sm text-muted-foreground">
                        {exception.type.includes('closed')
                          ? t('schedule.exceptionTypes.closed')
                          : `${exception.start} - ${exception.end}`}
                        {exception.reason ? ` - ${exception.reason}` : ''}
                      </p>
                    </div>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-destructive"
                      onClick={() => void handleDeleteException(exception.id)}
                    >
                      {t('common:actions.delete')}
                    </Button>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-center text-muted-foreground py-8">
                {t('schedule.noExceptions')}
              </p>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
