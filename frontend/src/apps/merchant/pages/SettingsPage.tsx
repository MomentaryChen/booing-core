import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type ChangeEvent,
  type ComponentType,
  type ReactNode,
} from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  Store,
  Headset,
  CalendarClock,
  Wallet,
  Receipt,
  Truck,
  Users,
  Bell,
  Palette,
  Puzzle,
  MapPin,
  Loader2,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { Switch } from '@/components/ui/switch'
import { Badge } from '@/components/ui/badge'
import { ToastAction } from '@/components/ui/toast'
import { useToast } from '@/components/ui/use-toast'
import { useAuthStore } from '@/shared/stores/authStore'
import { cn } from '@/shared/lib/utils'
import {
  createMerchantAvailabilityException,
  deleteMerchantAvailabilityException,
  fetchMerchantAvailabilityExceptions,
  fetchMerchantBusinessHours,
  fetchMerchantCustomization,
  fetchMerchantProfile,
  isMerchantPortalApiError,
  putMerchantBusinessHours,
  putMerchantCustomization,
  putMerchantProfile,
  type MerchantAvailabilityExceptionDto,
  type MerchantCustomizationDto,
} from '@/shared/lib/merchantPortalApi'

const DAY_DEFS = [
  { key: 'monday', api: 'MONDAY' },
  { key: 'tuesday', api: 'TUESDAY' },
  { key: 'wednesday', api: 'WEDNESDAY' },
  { key: 'thursday', api: 'THURSDAY' },
  { key: 'friday', api: 'FRIDAY' },
  { key: 'saturday', api: 'SATURDAY' },
  { key: 'sunday', api: 'SUNDAY' },
] as const

type SettingsPanel =
  | 'a-store'
  | 'a-contact'
  | 'a-hours'
  | 'b-payments'
  | 'b-tax'
  | 'b-delivery'
  | 'c-staff'
  | 'c-notifications'
  | 'd-theme'
  | 'd-integrations'

const VALID_SETTINGS_PANELS: SettingsPanel[] = [
  'a-store',
  'a-contact',
  'a-hours',
  'b-payments',
  'b-tax',
  'b-delivery',
  'c-staff',
  'c-notifications',
  'd-theme',
  'd-integrations',
]

function parseSettingsPanelParam(raw: string | null): SettingsPanel {
  if (raw && (VALID_SETTINGS_PANELS as string[]).includes(raw)) {
    return raw as SettingsPanel
  }
  return 'a-store'
}

type DayRow = {
  day: (typeof DAY_DEFS)[number]['key']
  apiDay: (typeof DAY_DEFS)[number]['api']
  enabled: boolean
  start: string
  end: string
}

function EmptyStateCard({
  icon: Icon,
  title,
  description,
  action,
}: {
  icon: ComponentType<{ className?: string }>
  title: string
  description: string
  action?: ReactNode
}) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-lg border border-dashed bg-muted/30 px-6 py-10 text-center">
      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-muted">
        <Icon className="h-6 w-6 text-muted-foreground" />
      </div>
      <div className="space-y-1">
        <p className="text-sm font-medium">{title}</p>
        <p className="text-sm text-muted-foreground">{description}</p>
      </div>
      {action ? <div className="pt-1">{action}</div> : null}
    </div>
  )
}

function ClientLivePreview({
  previewTitle,
  businessName,
  description,
  logoUrl,
  storeCategory,
  themeColor,
  logoPlaceholder,
  storeNameFallback,
  descriptionFallback,
}: {
  previewTitle: string
  businessName: string
  description: string
  logoUrl: string | null
  storeCategory: string
  themeColor: string
  logoPlaceholder: string
  storeNameFallback: string
  descriptionFallback: string
}) {
  const title = businessName.trim() || storeNameFallback
  const desc = description.trim() || descriptionFallback
  return (
    <div className="space-y-3">
      <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{previewTitle}</p>
      <div className="mx-auto w-[min(100%,260px)] rounded-[2rem] border-[10px] border-muted bg-muted p-2 shadow-xl">
        <div className="overflow-hidden rounded-2xl bg-background shadow-inner">
          <div className="h-28 w-full" style={{ backgroundColor: themeColor || '#2563eb' }} />
          <div className="flex flex-col items-center gap-2 px-4 pb-4 pt-0">
            <div className="-mt-10 flex h-20 w-20 items-center justify-center overflow-hidden rounded-2xl border-4 border-background bg-background shadow-sm">
              {logoUrl ? (
                <img src={logoUrl} alt="" className="h-full w-full object-cover" />
              ) : (
                <span className="text-xs text-muted-foreground">{logoPlaceholder}</span>
              )}
            </div>
            <div className="w-full space-y-1 text-center">
              <p className="text-base font-semibold leading-tight">{title}</p>
              {storeCategory.trim() ? (
                <Badge variant="secondary" className="mx-auto text-xs">
                  {storeCategory.trim()}
                </Badge>
              ) : null}
              <p className="line-clamp-3 text-xs text-muted-foreground">{desc}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export function SettingsPage() {
  const { t } = useTranslation(['merchant', 'common'])
  const merchantId = Number(useAuthStore((s) => s.user?.tenantId))
  const [searchParams, setSearchParams] = useSearchParams()
  const panel = parseSettingsPanelParam(searchParams.get('panel'))

  const navigatePanel = useCallback(
    (next: SettingsPanel) => {
      setSearchParams(
        (prev) => {
          const n = new URLSearchParams(prev)
          n.set('panel', next)
          return n
        },
        { replace: true },
      )
    },
    [setSearchParams],
  )

  useEffect(() => {
    if (!searchParams.get('panel')) {
      setSearchParams({ panel: 'a-store' }, { replace: true })
    }
  }, [searchParams, setSearchParams])
  const [businessName, setBusinessName] = useState('')
  const [description, setDescription] = useState('')
  const [storeCategory, setStoreCategory] = useState('')
  const [logoUrl, setLogoUrl] = useState<string | null>(null)
  const [address, setAddress] = useState('')
  const [phone, setPhone] = useState('')
  const [email, setEmail] = useState('')
  const [website, setWebsite] = useState('')
  const [lineContactUrl, setLineContactUrl] = useState('')
  const [customization, setCustomization] = useState<MerchantCustomizationDto | null>(null)
  const [themePreset, setThemePreset] = useState('minimal')
  const [themeColor, setThemeColor] = useState('#2563eb')
  const [bannerText, setBannerText] = useState('')
  const [schedule, setSchedule] = useState<DayRow[]>(
    DAY_DEFS.map((d) => ({
      day: d.key,
      apiDay: d.api,
      enabled: false,
      start: '09:00',
      end: '18:00',
    })),
  )
  const [exceptions, setExceptions] = useState<MerchantAvailabilityExceptionDto[]>([])
  const [newExceptionDate, setNewExceptionDate] = useState('')
  const [notifNewBooking, setNotifNewBooking] = useState(true)
  const [notifCancellation, setNotifCancellation] = useState(true)
  const [notifDailySummary, setNotifDailySummary] = useState(false)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const logoFileInputRef = useRef<HTMLInputElement | null>(null)
  const { toast } = useToast()

  const readImageAsDataUrl = (file: File): Promise<string> =>
    new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => {
        const result = reader.result
        if (typeof result === 'string' && result.startsWith('data:image/')) {
          resolve(result)
          return
        }
        reject(new Error('Invalid image data URL'))
      }
      reader.onerror = () => reject(new Error('Failed to read image file'))
      reader.readAsDataURL(file)
    })

  const loadSettings = useCallback(async () => {
    if (!merchantId) return
    setLoading(true)
    setError('')
    try {
      const [profile, customizationDto, hours, ex] = await Promise.all([
        fetchMerchantProfile(merchantId),
        fetchMerchantCustomization(merchantId),
        fetchMerchantBusinessHours(merchantId),
        fetchMerchantAvailabilityExceptions(merchantId),
      ])
      setCustomization(customizationDto)
      setBusinessName(customizationDto.heroTitle ?? '')
      setDescription(profile.description ?? '')
      setStoreCategory(profile.storeCategory ?? '')
      setLogoUrl(profile.logoUrl)
      setAddress(profile.address ?? '')
      setPhone(profile.phone ?? '')
      setEmail(profile.email ?? '')
      setWebsite(profile.website ?? '')
      setLineContactUrl(profile.lineContactUrl ?? '')
      setThemePreset(customizationDto.themePreset?.trim() || 'minimal')
      setThemeColor(customizationDto.themeColor?.trim() || '#2563eb')
      setBannerText(customizationDto.announcementText ?? '')

      const map = new Map(hours.map((h) => [h.dayOfWeek, h]))
      setSchedule(
        DAY_DEFS.map((d) => {
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

      setNotifNewBooking(customizationDto.notificationNewBooking ?? true)
      setNotifCancellation(customizationDto.notificationCancellation ?? true)
      setNotifDailySummary(customizationDto.notificationDailySummary ?? false)
    } catch (e) {
      setError(isMerchantPortalApiError(e) ? e.message : t('common:errors.generic'))
    } finally {
      setLoading(false)
    }
  }, [merchantId, t])

  useEffect(() => {
    void loadSettings()
  }, [loadSettings])

  const handleSave = async () => {
    if (!merchantId) return
    const cust = customization
    if (!cust) {
      setError(t('common:errors.generic'))
      return
    }
    setError('')
    setSaving(true)
    try {
      const hoursPayload = schedule
        .filter((d) => d.enabled)
        .map((d) => ({ dayOfWeek: d.apiDay, startTime: d.start, endTime: d.end }))

      const requests: Promise<unknown>[] = [
        putMerchantProfile(merchantId, {
          description: description.trim() || null,
          logoUrl: logoUrl || null,
          address: address.trim() || null,
          phone: phone.trim() || null,
          email: email.trim() || null,
          website: website.trim() || null,
          storeCategory: storeCategory.trim() || null,
          lineContactUrl: lineContactUrl.trim() || null,
        }),
        putMerchantCustomization(merchantId, {
          themePreset: themePreset.trim() || 'minimal',
          themeColor: themeColor.trim() || '#2563eb',
          heroTitle: businessName.trim() ? businessName : cust.heroTitle?.trim() || 'Welcome',
          bookingFlowText: cust.bookingFlowText?.trim() || 'Please choose service and timeslot.',
          inviteCode: cust.inviteCode ?? '',
          termsText: cust.termsText ?? '',
          announcementText: bannerText ?? '',
          faqJson: cust.faqJson?.trim() || '[]',
          bufferMinutes: cust.bufferMinutes ?? 0,
          homepageSectionsJson: cust.homepageSectionsJson?.trim() || '["hero","services","booking"]',
          categoryOrderJson: cust.categoryOrderJson?.trim() || '[]',
          notificationNewBooking: notifNewBooking,
          notificationCancellation: notifCancellation,
          notificationDailySummary: notifDailySummary,
        }),
        putMerchantBusinessHours(merchantId, hoursPayload),
      ]

      await Promise.all(requests)
      toast({
        title: t('common:status.success'),
        description: t('settings.layout.stickyHint'),
      })
      await loadSettings()
    } catch (e) {
      const message = isMerchantPortalApiError(e) ? e.message : t('common:errors.generic')
      setError(message)
      toast({
        variant: 'destructive',
        title: t('common:errors.generic'),
        description: message,
        action: (
          <ToastAction altText={t('common:actions.retry', { defaultValue: 'Retry' })} onClick={() => void handleSave()}>
            {t('common:actions.retry', { defaultValue: 'Retry' })}
          </ToastAction>
        ),
      })
    } finally {
      setSaving(false)
    }
  }

  const handleLogoFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) {
      return
    }

    setError('')
    if (!file.type.startsWith('image/')) {
      const message = t('settings.business.logoOnlyImage')
      setError(message)
      toast({
        variant: 'destructive',
        title: t('common:errors.generic'),
        description: message,
      })
      event.target.value = ''
      return
    }

    try {
      const dataUrl = await readImageAsDataUrl(file)
      setLogoUrl(dataUrl)
    } catch {
      const message = t('settings.business.logoUploadFailed')
      setError(message)
      toast({
        variant: 'destructive',
        title: t('common:errors.generic'),
        description: message,
      })
    } finally {
      event.target.value = ''
    }
  }

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
      toast({
        title: t('common:status.success'),
        description: t('settings.hours.addException'),
      })
      await loadSettings()
    } catch (e) {
      const message = isMerchantPortalApiError(e) ? e.message : t('common:errors.generic')
      setError(message)
      toast({
        variant: 'destructive',
        title: t('common:errors.generic'),
        description: message,
      })
    }
  }

  const handleDeleteException = async (exceptionId: number) => {
    if (!merchantId) return
    setError('')
    try {
      await deleteMerchantAvailabilityException(merchantId, exceptionId)
      toast({
        title: t('common:status.success'),
        description: t('common:actions.delete'),
      })
      await loadSettings()
    } catch (e) {
      const message = isMerchantPortalApiError(e) ? e.message : t('common:errors.generic')
      setError(message)
      toast({
        variant: 'destructive',
        title: t('common:errors.generic'),
        description: message,
      })
    }
  }

  const mapsHref =
    address.trim().length > 0
      ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(address.trim())}`
      : null

  const showLivePreview = panel === 'a-store' || panel === 'd-theme'

  const navGroups: Array<{
    id: string
    titleKey: string
    items: Array<{ panel: SettingsPanel; labelKey: string; icon: ComponentType<{ className?: string }> }>
  }> = [
    {
      id: 'a',
      titleKey: 'settings.layout.dimA',
      items: [
        { panel: 'a-store', labelKey: 'settings.layout.storeProfile', icon: Store },
        { panel: 'a-contact', labelKey: 'settings.layout.contactInfo', icon: Headset },
        { panel: 'a-hours', labelKey: 'settings.layout.businessHours', icon: CalendarClock },
      ],
    },
    {
      id: 'b',
      titleKey: 'settings.layout.dimB',
      items: [
        { panel: 'b-payments', labelKey: 'settings.layout.payments', icon: Wallet },
        { panel: 'b-tax', labelKey: 'settings.layout.tax', icon: Receipt },
        { panel: 'b-delivery', labelKey: 'settings.layout.delivery', icon: Truck },
      ],
    },
    {
      id: 'c',
      titleKey: 'settings.layout.dimC',
      items: [
        { panel: 'c-staff', labelKey: 'settings.layout.staff', icon: Users },
        { panel: 'c-notifications', labelKey: 'settings.layout.notifications', icon: Bell },
      ],
    },
    {
      id: 'd',
      titleKey: 'settings.layout.dimD',
      items: [
        { panel: 'd-theme', labelKey: 'settings.layout.theme', icon: Palette },
        { panel: 'd-integrations', labelKey: 'settings.layout.integrations', icon: Puzzle },
      ],
    },
  ]

  if (!merchantId) {
    return <p className="text-sm text-muted-foreground">{t('bookings.noMerchantContext')}</p>
  }

  return (
    <div className="flex min-h-full flex-col gap-6 pb-28">
      <div>
        <h1 className="text-3xl font-bold">{t('settings.title')}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t('settings.layout.subtitle')}</p>
      </div>

      {loading ? (
        <p className="text-sm text-muted-foreground">{t('common:status.loading')}</p>
      ) : null}
      {error ? <p className="text-sm text-destructive">{error}</p> : null}

      <div className="flex flex-col gap-8 xl:flex-row xl:items-start">
        <aside className="w-full shrink-0 space-y-6 xl:w-64">
          {navGroups.map((group) => (
            <div key={group.id} className="space-y-2">
              <p className="px-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                {t(group.titleKey)}
              </p>
              <nav className="flex flex-col gap-1">
                {group.items.map((item) => {
                  const Icon = item.icon
                  const active = panel === item.panel
                  return (
                    <button
                      key={item.panel}
                      type="button"
                      onClick={() => navigatePanel(item.panel)}
                      className={cn(
                        'flex items-center gap-2 rounded-md px-3 py-2 text-left text-sm transition-colors',
                        active ? 'bg-accent text-accent-foreground' : 'hover:bg-muted/80',
                      )}
                    >
                      <Icon className="h-4 w-4 shrink-0 opacity-80" />
                      <span>{t(item.labelKey)}</span>
                    </button>
                  )
                })}
              </nav>
            </div>
          ))}
        </aside>

        <div className="grid min-w-0 flex-1 gap-8 lg:grid-cols-[minmax(0,1fr)] xl:grid-cols-[minmax(0,1fr)_minmax(240px,280px)]">
          <div className="min-w-0 space-y-6">
            {panel === 'a-store' ? (
              <Card>
                <CardHeader>
                  <div className="flex items-center gap-2">
                    <Store className="h-5 w-5" />
                    <CardTitle>{t('settings.layout.storeProfile')}</CardTitle>
                  </div>
                  <CardDescription>{t('settings.storeProfile.description')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-6">
                  <div className="space-y-2">
                    <Label htmlFor="businessName">{t('settings.business.name')}</Label>
                    <Input
                      id="businessName"
                      value={businessName}
                      onChange={(e) => setBusinessName(e.target.value)}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="storeCategory">{t('settings.storeProfile.category')}</Label>
                    <Input
                      id="storeCategory"
                      value={storeCategory}
                      onChange={(e) => setStoreCategory(e.target.value)}
                      placeholder={t('settings.storeProfile.categoryPlaceholder')}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="description">{t('settings.business.description')}</Label>
                    <textarea
                      id="description"
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                      rows={4}
                      className={cn(
                        'placeholder:text-muted-foreground selection:bg-primary selection:text-primary-foreground dark:bg-input/30 border-input flex w-full min-w-0 rounded-md border bg-transparent px-3 py-2 text-base shadow-xs transition-[color,box-shadow] outline-none md:text-sm',
                        'focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px]',
                      )}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>{t('settings.business.logo')}</Label>
                    <div className="flex flex-col gap-3 rounded-md border p-3">
                      <div className="flex flex-wrap items-center gap-3">
                        <Button type="button" variant="outline" onClick={() => logoFileInputRef.current?.click()}>
                          {t('settings.business.uploadLogo')}
                        </Button>
                        {logoUrl ? (
                          <Button type="button" variant="ghost" onClick={() => setLogoUrl(null)}>
                            {t('settings.business.removeLogo')}
                          </Button>
                        ) : null}
                      </div>
                      <input
                        ref={logoFileInputRef}
                        type="file"
                        accept="image/*"
                        className="hidden"
                        onChange={handleLogoFileChange}
                      />
                      {logoUrl ? (
                        <img
                          src={logoUrl}
                          alt={t('settings.business.logoPreviewAlt')}
                          className="h-20 w-20 rounded-md border object-contain"
                        />
                      ) : (
                        <p className="text-sm text-muted-foreground">{t('settings.business.logoHint')}</p>
                      )}
                    </div>
                  </div>
                </CardContent>
              </Card>
            ) : null}

            {panel === 'a-contact' ? (
              <Card>
                <CardHeader>
                  <div className="flex items-center gap-2">
                    <Headset className="h-5 w-5" />
                    <CardTitle>{t('settings.layout.contactInfo')}</CardTitle>
                  </div>
                  <CardDescription>{t('settings.contact.description')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-6">
                  <div className="grid gap-4 md:grid-cols-2">
                    <div className="space-y-2">
                      <Label htmlFor="phone">{t('settings.business.phone')}</Label>
                      <Input id="phone" value={phone} onChange={(e) => setPhone(e.target.value)} />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="email">{t('settings.business.email')}</Label>
                      <Input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
                    </div>
                    <div className="space-y-2 md:col-span-2">
                      <Label htmlFor="lineContactUrl">{t('settings.contact.line')}</Label>
                      <Input
                        id="lineContactUrl"
                        value={lineContactUrl}
                        onChange={(e) => setLineContactUrl(e.target.value)}
                        placeholder="https://line.me/..."
                      />
                    </div>
                    <div className="space-y-2 md:col-span-2">
                      <Label htmlFor="website">{t('settings.contact.website')}</Label>
                      <Input id="website" value={website} onChange={(e) => setWebsite(e.target.value)} />
                    </div>
                    <div className="space-y-2 md:col-span-2">
                      <div className="flex items-center gap-2">
                        <MapPin className="h-4 w-4 text-muted-foreground" />
                        <Label htmlFor="address">{t('settings.business.address')}</Label>
                      </div>
                      <Input id="address" value={address} onChange={(e) => setAddress(e.target.value)} />
                      {mapsHref ? (
                        <a
                          href={mapsHref}
                          target="_blank"
                          rel="noreferrer"
                          className="text-sm text-primary underline-offset-4 hover:underline"
                        >
                          {t('settings.contact.openMaps')}
                        </a>
                      ) : (
                        <p className="text-xs text-muted-foreground">{t('settings.contact.mapsHint')}</p>
                      )}
                    </div>
                  </div>
                </CardContent>
              </Card>
            ) : null}

            {panel === 'a-hours' ? (
              <Card>
                <CardHeader>
                  <div className="flex items-center gap-2">
                    <CalendarClock className="h-5 w-5" />
                    <CardTitle>{t('settings.layout.businessHours')}</CardTitle>
                  </div>
                  <CardDescription>{t('settings.hours.description')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-6">
                  <div className="space-y-3">
                    {schedule.map((row, index) => (
                      <div
                        key={row.day}
                        className="flex flex-col gap-3 rounded-md border p-3 sm:flex-row sm:items-center sm:justify-between"
                      >
                        <div className="flex items-center gap-3">
                          <Switch checked={row.enabled} onCheckedChange={() => toggleDay(index)} id={`day-${row.day}`} />
                          <Label htmlFor={`day-${row.day}`} className="min-w-[6rem] font-medium capitalize">
                            {t(`schedule.days.${row.day}`)}
                          </Label>
                        </div>
                        <div className="flex flex-wrap items-center gap-2">
                          <Input
                            type="time"
                            className="w-32"
                            disabled={!row.enabled}
                            value={row.start}
                            onChange={(e) => updateDayTime(index, 'start', e.target.value)}
                          />
                          <span className="text-muted-foreground">–</span>
                          <Input
                            type="time"
                            className="w-32"
                            disabled={!row.enabled}
                            value={row.end}
                            onChange={(e) => updateDayTime(index, 'end', e.target.value)}
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                  <Separator />
                  <div className="space-y-3">
                    <p className="text-sm font-medium">{t('settings.hours.exceptionsTitle')}</p>
                    <div className="flex flex-wrap gap-2">
                      <Input
                        type="date"
                        className="w-44"
                        value={newExceptionDate}
                        onChange={(e) => setNewExceptionDate(e.target.value)}
                      />
                      <Button type="button" variant="secondary" onClick={() => void handleAddException()}>
                        {t('settings.hours.addException')}
                      </Button>
                    </div>
                    <ul className="space-y-2 text-sm">
                      {exceptions.map((ex) => (
                        <li
                          key={ex.id}
                          className="flex items-center justify-between gap-2 rounded-md border px-3 py-2"
                        >
                          <span className="truncate">
                            {ex.startAt.slice(0, 10)} · {ex.type}
                          </span>
                          <Button type="button" variant="ghost" size="sm" onClick={() => void handleDeleteException(ex.id)}>
                            {t('common:actions.delete')}
                          </Button>
                        </li>
                      ))}
                      {exceptions.length === 0 ? (
                        <li className="text-muted-foreground">{t('schedule.noExceptions')}</li>
                      ) : null}
                    </ul>
                    <p className="text-xs text-muted-foreground">{t('settings.hours.saveHint')}</p>
                  </div>
                </CardContent>
              </Card>
            ) : null}

            {panel === 'b-payments' ? (
              <Card>
                <CardHeader>
                  <div className="flex items-center gap-2">
                    <Wallet className="h-5 w-5" />
                    <CardTitle>{t('settings.layout.payments')}</CardTitle>
                  </div>
                  <CardDescription>{t('settings.payment.description')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-6">
                  <EmptyStateCard
                    icon={Wallet}
                    title={t('settings.financials.paymentsEmptyTitle')}
                    description={t('settings.financials.paymentsEmptyBody')}
                  />
                  <div className="rounded-lg border p-4">
                    <div className="flex items-center justify-between gap-4">
                      <div>
                        <p className="font-medium">{t('settings.payment.providers.stripe.name')}</p>
                        <p className="text-sm text-muted-foreground">
                          {t('settings.payment.providers.stripe.description')}
                        </p>
                      </div>
                      <Button type="button" variant="outline" disabled title={t('settings.financials.connectDisabled')}>
                        {t('settings.payment.connect')}
                      </Button>
                    </div>
                  </div>
                  <div className="rounded-lg border p-4">
                    <div className="flex items-center justify-between gap-4">
                      <div>
                        <p className="font-medium">{t('settings.payment.providers.paypal.name')}</p>
                        <p className="text-sm text-muted-foreground">
                          {t('settings.payment.providers.paypal.description')}
                        </p>
                      </div>
                      <Button type="button" variant="outline" disabled title={t('settings.financials.connectDisabled')}>
                        {t('settings.payment.connect')}
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ) : null}

            {panel === 'b-tax' ? (
              <Card>
                <CardHeader>
                  <div className="flex items-center gap-2">
                    <Receipt className="h-5 w-5" />
                    <CardTitle>{t('settings.layout.tax')}</CardTitle>
                  </div>
                  <CardDescription>{t('settings.financials.taxDescription')}</CardDescription>
                </CardHeader>
                <CardContent>
                  <EmptyStateCard
                    icon={Receipt}
                    title={t('settings.financials.taxEmptyTitle')}
                    description={t('settings.financials.taxEmptyBody')}
                  />
                </CardContent>
              </Card>
            ) : null}

            {panel === 'b-delivery' ? (
              <Card>
                <CardHeader>
                  <div className="flex items-center gap-2">
                    <Truck className="h-5 w-5" />
                    <CardTitle>{t('settings.layout.delivery')}</CardTitle>
                  </div>
                  <CardDescription>{t('settings.financials.deliveryDescription')}</CardDescription>
                </CardHeader>
                <CardContent>
                  <EmptyStateCard
                    icon={Truck}
                    title={t('settings.financials.deliveryEmptyTitle')}
                    description={t('settings.financials.deliveryEmptyBody')}
                  />
                </CardContent>
              </Card>
            ) : null}

            {panel === 'c-staff' ? (
              <Card>
                <CardHeader>
                  <div className="flex items-center gap-2">
                    <Users className="h-5 w-5" />
                    <CardTitle>{t('settings.layout.staff')}</CardTitle>
                  </div>
                  <CardDescription>{t('settings.security.staffDescription')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <EmptyStateCard
                    icon={Users}
                    title={t('settings.security.staffEmptyTitle')}
                    description={t('settings.security.staffEmptyBody')}
                    action={
                      <Button asChild variant="default">
                        <Link to="/teams">{t('settings.security.goTeams')}</Link>
                      </Button>
                    }
                  />
                </CardContent>
              </Card>
            ) : null}

            {panel === 'c-notifications' ? (
              <Card>
                <CardHeader>
                  <div className="flex items-center gap-2">
                    <Bell className="h-5 w-5" />
                    <CardTitle>{t('settings.layout.notifications')}</CardTitle>
                  </div>
                  <CardDescription>{t('settings.notifications.description')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-6">
                  <p className="text-xs text-muted-foreground">{t('settings.notifications.localHint')}</p>
                  <div className="flex items-center justify-between gap-4">
                    <div>
                      <Label>{t('settings.notifications.newBooking.title')}</Label>
                      <p className="text-sm text-muted-foreground">{t('settings.notifications.newBooking.text')}</p>
                    </div>
                    <Switch checked={notifNewBooking} onCheckedChange={setNotifNewBooking} />
                  </div>
                  <Separator />
                  <div className="flex items-center justify-between gap-4">
                    <div>
                      <Label>{t('settings.notifications.cancellation.title')}</Label>
                      <p className="text-sm text-muted-foreground">{t('settings.notifications.cancellation.text')}</p>
                    </div>
                    <Switch checked={notifCancellation} onCheckedChange={setNotifCancellation} />
                  </div>
                  <Separator />
                  <div className="flex items-center justify-between gap-4">
                    <div>
                      <Label>{t('settings.notifications.dailySummary.title')}</Label>
                      <p className="text-sm text-muted-foreground">{t('settings.notifications.dailySummary.text')}</p>
                    </div>
                    <Switch checked={notifDailySummary} onCheckedChange={setNotifDailySummary} />
                  </div>
                </CardContent>
              </Card>
            ) : null}

            {panel === 'd-theme' ? (
              <Card>
                <CardHeader>
                  <div className="flex items-center gap-2">
                    <Palette className="h-5 w-5" />
                    <CardTitle>{t('settings.layout.theme')}</CardTitle>
                  </div>
                  <CardDescription>{t('settings.advanced.themeDescription')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-6">
                  <div className="grid gap-4 md:grid-cols-2">
                    <div className="space-y-2">
                      <Label htmlFor="themePreset">{t('settings.advanced.themePreset')}</Label>
                      <select
                        id="themePreset"
                        value={themePreset}
                        onChange={(e) => setThemePreset(e.target.value)}
                        className="h-9 w-full rounded-md border border-input bg-transparent px-3 text-sm shadow-xs"
                      >
                        <option value="minimal">{t('settings.advanced.presets.minimal')}</option>
                        <option value="ocean">{t('settings.advanced.presets.ocean')}</option>
                        <option value="warm">{t('settings.advanced.presets.warm')}</option>
                        <option value="contrast">{t('settings.advanced.presets.contrast')}</option>
                      </select>
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="themeColor">{t('settings.advanced.themeColor')}</Label>
                      <div className="flex gap-2">
                        <Input
                          id="themeColor"
                          type="color"
                          className="h-9 w-14 cursor-pointer p-1"
                          value={themeColor.startsWith('#') ? themeColor : '#2563eb'}
                          onChange={(e) => setThemeColor(e.target.value)}
                        />
                        <Input value={themeColor} onChange={(e) => setThemeColor(e.target.value)} />
                      </div>
                    </div>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="bannerText">{t('settings.advanced.banner')}</Label>
                    <textarea
                      id="bannerText"
                      value={bannerText}
                      onChange={(e) => setBannerText(e.target.value)}
                      rows={3}
                      className={cn(
                        'placeholder:text-muted-foreground selection:bg-primary selection:text-primary-foreground dark:bg-input/30 border-input flex w-full min-w-0 rounded-md border bg-transparent px-3 py-2 text-base shadow-xs transition-[color,box-shadow] outline-none md:text-sm',
                        'focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px]',
                      )}
                    />
                  </div>
                </CardContent>
              </Card>
            ) : null}

            {panel === 'd-integrations' ? (
              <Card>
                <CardHeader>
                  <div className="flex items-center gap-2">
                    <Puzzle className="h-5 w-5" />
                    <CardTitle>{t('settings.layout.integrations')}</CardTitle>
                  </div>
                  <CardDescription>{t('settings.advanced.integrationsDescription')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-6">
                  <EmptyStateCard
                    icon={Puzzle}
                    title={t('settings.advanced.integrationsEmptyTitle')}
                    description={t('settings.advanced.integrationsEmptyBody')}
                  />
                  <div className="space-y-2">
                    <Label htmlFor="webhookUrl">{t('settings.advanced.webhookUrl')}</Label>
                    <Input id="webhookUrl" disabled placeholder="https://example.com/webhooks/booking" />
                  </div>
                </CardContent>
              </Card>
            ) : null}
          </div>

          {showLivePreview ? (
            <aside className="hidden xl:block">
              <div className="sticky top-4 rounded-xl border bg-card p-4 shadow-sm">
                <ClientLivePreview
                  previewTitle={t('settings.layout.livePreview')}
                  businessName={businessName}
                  description={description}
                  logoUrl={logoUrl}
                  storeCategory={storeCategory}
                  themeColor={themeColor}
                  logoPlaceholder={t('settings.layout.logoPlaceholder')}
                  storeNameFallback={t('settings.layout.previewStoreFallback')}
                  descriptionFallback={t('settings.layout.previewDescriptionFallback')}
                />
              </div>
            </aside>
          ) : null}
        </div>
      </div>

      <div className="sticky bottom-0 z-20 -mx-6 mt-auto border-t bg-background/95 px-6 py-3 backdrop-blur supports-[backdrop-filter]:bg-background/80">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-xs text-muted-foreground">{t('settings.layout.stickyHint')}</p>
          <Button type="button" onClick={() => void handleSave()} disabled={saving || loading} className="min-w-[120px]">
            {saving ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
            {t('common:actions.save')}
          </Button>
        </div>
      </div>
    </div>
  )
}
