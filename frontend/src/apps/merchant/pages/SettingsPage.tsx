import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Separator } from '@/components/ui/separator'
import { Switch } from '@/components/ui/switch'
import { useAuthStore } from '@/shared/stores/authStore'
import {
  fetchMerchantCustomization,
  fetchMerchantProfile,
  isMerchantPortalApiError,
  putMerchantCustomization,
  putMerchantProfile,
  type MerchantCustomizationDto,
} from '@/shared/lib/merchantPortalApi'

export function SettingsPage() {
  const { t } = useTranslation(['merchant', 'common'])
  const merchantId = Number(useAuthStore((s) => s.user?.tenantId))
  const [businessName, setBusinessName] = useState('')
  const [description, setDescription] = useState('')
  const [logoUrl, setLogoUrl] = useState<string | null>(null)
  const [address, setAddress] = useState('')
  const [phone, setPhone] = useState('')
  const [email, setEmail] = useState('')
  const [website, setWebsite] = useState('')
  const [customization, setCustomization] = useState<MerchantCustomizationDto | null>(null)
  const [loading, setLoading] = useState(true)
  const [saveNotice, setSaveNotice] = useState<string | null>(null)
  const [error, setError] = useState('')

  const loadSettings = useCallback(async () => {
    if (!merchantId) return
    setLoading(true)
    setError('')
    try {
      const [profile, customizationDto] = await Promise.all([
        fetchMerchantProfile(merchantId),
        fetchMerchantCustomization(merchantId),
      ])
      setCustomization(customizationDto)
      setBusinessName(customizationDto.heroTitle ?? '')
      setDescription(profile.description ?? '')
      setLogoUrl(profile.logoUrl)
      setAddress(profile.address ?? '')
      setPhone(profile.phone ?? '')
      setEmail(profile.email ?? '')
      setWebsite(profile.website ?? '')
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
    setError('')
    setSaveNotice(null)
    try {
      const cust = customization
      if (!cust) {
        setError(t('common:errors.generic'))
        return
      }
      await Promise.all([
        putMerchantProfile(merchantId, {
          description: description || null,
          logoUrl: logoUrl || null,
          address: address || null,
          phone: phone || null,
          email: email || null,
          website: website || null,
        }),
        putMerchantCustomization(merchantId, {
          themePreset: cust.themePreset?.trim() || 'minimal',
          themeColor: cust.themeColor?.trim() || '#2563eb',
          heroTitle: businessName?.trim()
            ? businessName
            : cust.heroTitle?.trim() || 'Welcome',
          bookingFlowText:
            cust.bookingFlowText?.trim() || 'Please choose service and timeslot.',
          inviteCode: cust.inviteCode ?? '',
          termsText: cust.termsText ?? '',
          announcementText: cust.announcementText ?? '',
          faqJson: cust.faqJson?.trim() || '[]',
          bufferMinutes: cust.bufferMinutes ?? 0,
          homepageSectionsJson:
            cust.homepageSectionsJson?.trim() || '["hero","services","booking"]',
          categoryOrderJson: cust.categoryOrderJson?.trim() || '[]',
        }),
      ])
      setSaveNotice(t('common:status.success'))
    } catch (e) {
      setError(isMerchantPortalApiError(e) ? e.message : t('common:errors.generic'))
    }
  }

  if (!merchantId) {
    return <p className="text-sm text-muted-foreground">{t('bookings.noMerchantContext')}</p>
  }

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">{t('settings.title')}</h1>
      {loading && <p className="text-sm text-muted-foreground">{t('common:status.loading')}</p>}
      {error && <p className="text-sm text-destructive">{error}</p>}
      {saveNotice && (
        <p className="text-sm text-muted-foreground" role="status">
          {saveNotice}
        </p>
      )}

      <Tabs defaultValue="business" className="w-full">
        <TabsList>
          <TabsTrigger value="business">{t('settings.tabs.business')}</TabsTrigger>
          <TabsTrigger value="payment">{t('settings.tabs.payment')}</TabsTrigger>
          <TabsTrigger value="notifications">{t('settings.tabs.notifications')}</TabsTrigger>
          <TabsTrigger value="team">{t('settings.tabs.team')}</TabsTrigger>
        </TabsList>

        <TabsContent value="business">
          <Card>
            <CardHeader>
              <CardTitle>{t('settings.tabs.business')}</CardTitle>
              <CardDescription>{t('settings.business.descriptionText')}</CardDescription>
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
                <Label htmlFor="description">{t('settings.business.description')}</Label>
                <Input
                  id="description"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                />
              </div>
              <Separator />
              <div className="grid gap-4 md:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="address">{t('settings.business.address')}</Label>
                  <Input
                    id="address"
                    value={address}
                    onChange={(e) => setAddress(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="phone">{t('settings.business.phone')}</Label>
                  <Input
                    id="phone"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="email">{t('settings.business.email')}</Label>
                  <Input
                    id="email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="website">{t('settings.business.website')}</Label>
                  <Input
                    id="website"
                    value={website}
                    onChange={(e) => setWebsite(e.target.value)}
                  />
                </div>
              </div>
              <Button onClick={handleSave}>{t('common:actions.save')}</Button>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="payment">
          <Card>
            <CardHeader>
              <CardTitle>{t('settings.tabs.payment')}</CardTitle>
              <CardDescription>{t('settings.payment.description')}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="rounded-lg border p-4">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="font-medium">{t('settings.payment.providers.stripe.name')}</p>
                    <p className="text-sm text-muted-foreground">{t('settings.payment.providers.stripe.description')}</p>
                  </div>
                  <Button variant="outline">{t('settings.payment.connect')}</Button>
                </div>
              </div>
              <div className="rounded-lg border p-4">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="font-medium">{t('settings.payment.providers.paypal.name')}</p>
                    <p className="text-sm text-muted-foreground">{t('settings.payment.providers.paypal.description')}</p>
                  </div>
                  <Button variant="outline">{t('settings.payment.connect')}</Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="notifications">
          <Card>
            <CardHeader>
              <CardTitle>{t('settings.tabs.notifications')}</CardTitle>
              <CardDescription>{t('settings.notifications.description')}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="flex items-center justify-between">
                <div>
                  <Label>{t('settings.notifications.newBooking.title')}</Label>
                  <p className="text-sm text-muted-foreground">
                    {t('settings.notifications.newBooking.text')}
                  </p>
                </div>
                <Switch defaultChecked />
              </div>
              <Separator />
              <div className="flex items-center justify-between">
                <div>
                  <Label>{t('settings.notifications.cancellation.title')}</Label>
                  <p className="text-sm text-muted-foreground">
                    {t('settings.notifications.cancellation.text')}
                  </p>
                </div>
                <Switch defaultChecked />
              </div>
              <Separator />
              <div className="flex items-center justify-between">
                <div>
                  <Label>{t('settings.notifications.dailySummary.title')}</Label>
                  <p className="text-sm text-muted-foreground">
                    {t('settings.notifications.dailySummary.text')}
                  </p>
                </div>
                <Switch />
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="team">
          <Card>
            <CardHeader>
              <CardTitle>{t('settings.tabs.team')}</CardTitle>
              <CardDescription>{t('settings.team.description')}</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-center py-8 text-muted-foreground">
                {t('settings.team.planNotice')}
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}
