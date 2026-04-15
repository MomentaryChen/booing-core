import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '@/shared/hooks/useAuth'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Switch } from '@/components/ui/switch'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Separator } from '@/components/ui/separator'
import {
  fetchClientProfile,
  fetchClientProfilePreferences,
  isClientProfileApiError,
  patchClientProfilePassword,
  putClientProfile,
  putClientProfilePreferences,
} from '@/shared/lib/clientProfileApi'

export function ProfilePage() {
  const { t, i18n } = useTranslation(['client', 'common'])
  const { user } = useAuth()
  const [name, setName] = useState(user?.name || '')
  const [email] = useState(user?.email || '')
  const [phone, setPhone] = useState('')
  const [language, setLanguage] = useState(i18n.language)
  const [emailNotifications, setEmailNotifications] = useState(true)
  const [smsNotifications, setSmsNotifications] = useState(false)
  const [saveNotice, setSaveNotice] = useState<string | null>(null)
  const [securityNotice, setSecurityNotice] = useState<string | null>(null)
  const [loadError, setLoadError] = useState('')
  const [loading, setLoading] = useState(true)
  const [timezone, setTimezone] = useState('utc+8')
  const [currency, setCurrency] = useState('usd')
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  useEffect(() => {
    let mounted = true
    async function loadProfile() {
      setLoading(true)
      setLoadError('')
      try {
        const [profile, preferences] = await Promise.all([
          fetchClientProfile(),
          fetchClientProfilePreferences(),
        ])
        if (!mounted) return
        if (profile.suggestedName) {
          setName(profile.suggestedName)
        }
        if (profile.suggestedContact) {
          setPhone(profile.suggestedContact)
        }
        if (preferences.language) {
          setLanguage(preferences.language)
        }
        if (preferences.timezone) {
          setTimezone(preferences.timezone)
        }
        if (preferences.currency) {
          setCurrency(preferences.currency)
        }
        if (preferences.notificationPrefs.email != null) {
          setEmailNotifications(preferences.notificationPrefs.email)
        }
        if (preferences.notificationPrefs.sms != null) {
          setSmsNotifications(preferences.notificationPrefs.sms)
        }
      } catch (e) {
        if (!mounted) return
        setLoadError(isClientProfileApiError(e) ? e.message : t('common:errors.generic'))
      } finally {
        if (mounted) {
          setLoading(false)
        }
      }
    }
    void loadProfile()
    return () => {
      mounted = false
    }
  }, [t])

  const getInitials = (name: string) => {
    return name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2)
  }

  const handleLanguageChange = (lang: string) => {
    setLanguage(lang)
    i18n.changeLanguage(lang)
  }

  const handleSavePersonal = async () => {
    setSaveNotice(null)
    setLoadError('')
    try {
      const updated = await putClientProfile({
        suggestedName: name,
        suggestedContact: phone,
      })
      setName(updated.suggestedName ?? name)
      setPhone(updated.suggestedContact ?? phone)
      setSaveNotice(t('common:status.success'))
    } catch (e) {
      setLoadError(isClientProfileApiError(e) ? e.message : t('common:errors.generic'))
    }
  }

  const handleSavePreferences = async () => {
    setSaveNotice(null)
    setLoadError('')
    try {
      const updated = await putClientProfilePreferences({
        language,
        timezone,
        currency,
        notificationPrefs: {
          email: emailNotifications,
          sms: smsNotifications,
        },
      })
      setLanguage(updated.language ?? language)
      setTimezone(updated.timezone ?? timezone)
      setCurrency(updated.currency ?? currency)
      if (updated.notificationPrefs.email != null) {
        setEmailNotifications(updated.notificationPrefs.email)
      }
      if (updated.notificationPrefs.sms != null) {
        setSmsNotifications(updated.notificationPrefs.sms)
      }
      setSaveNotice(t('common:status.success'))
    } catch (e) {
      setLoadError(isClientProfileApiError(e) ? e.message : t('common:errors.generic'))
    }
  }

  const handleUpdatePassword = async () => {
    setSecurityNotice(null)
    setLoadError('')
    if (!currentPassword || !newPassword || !confirmPassword) {
      setLoadError(t('common:validation.required'))
      return
    }
    if (newPassword !== confirmPassword) {
      setLoadError(t('profile.security.passwordMismatch'))
      return
    }
    if (newPassword.length < 8) {
      setLoadError(t('common:validation.minLength', { min: 8 }))
      return
    }
    try {
      await patchClientProfilePassword({ currentPassword, newPassword })
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setSecurityNotice(t('profile.security.passwordUpdated'))
    } catch (e) {
      setLoadError(isClientProfileApiError(e) ? e.message : t('common:errors.generic'))
    }
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-3xl">
      <h1 className="text-3xl font-bold mb-8">{t('profile.title')}</h1>
      {loading ? <p className="mb-4 text-sm text-muted-foreground">{t('common:status.loading')}</p> : null}
      {loadError ? <p className="mb-4 text-sm text-destructive">{loadError}</p> : null}
      {saveNotice && (
        <p className="mb-4 text-sm text-muted-foreground" role="status">
          {saveNotice}
        </p>
      )}

      <Tabs defaultValue="personal" className="w-full">
        <TabsList className="mb-6">
          <TabsTrigger value="personal">{t('profile.tabs.personal')}</TabsTrigger>
          <TabsTrigger value="preferences">{t('profile.tabs.preferences')}</TabsTrigger>
          <TabsTrigger value="notifications">{t('profile.tabs.notifications')}</TabsTrigger>
          <TabsTrigger value="security">{t('profile.tabs.security')}</TabsTrigger>
        </TabsList>

        <TabsContent value="personal">
          <Card>
            <CardHeader>
              <CardTitle>{t('profile.tabs.personal')}</CardTitle>
              <CardDescription>{t('profile.personal.description')}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="flex items-center gap-6">
                <Avatar className="h-20 w-20">
                  <AvatarFallback className="text-xl">
                    {user?.name ? getInitials(user.name) : 'U'}
                  </AvatarFallback>
                </Avatar>
                <Button variant="outline">{t('profile.personal.changeAvatar')}</Button>
              </div>
              <Separator />
              <div className="grid gap-4">
                <div className="space-y-2">
                  <Label htmlFor="name">{t('profile.personal.name')}</Label>
                  <Input
                    id="name"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="email">{t('profile.personal.email')}</Label>
                  <Input
                    id="email"
                    type="email"
                    value={email}
                    disabled
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="phone">{t('profile.personal.phone')}</Label>
                  <Input
                    id="phone"
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder={t('profile.personal.phonePlaceholder')}
                  />
                </div>
              </div>
              <Button onClick={() => void handleSavePersonal()}>{t('common:actions.save')}</Button>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="preferences">
          <Card>
            <CardHeader>
              <CardTitle>{t('profile.tabs.preferences')}</CardTitle>
              <CardDescription>{t('profile.preferences.description')}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-2">
                <Label>{t('profile.preferences.language')}</Label>
                <Select value={language} onValueChange={handleLanguageChange}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="en-US">{t('common:language.en-US')}</SelectItem>
                    <SelectItem value="zh-TW">{t('common:language.zh-TW')}</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>{t('profile.preferences.timezone')}</Label>
                <Select value={timezone} onValueChange={setTimezone}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="utc-8">{t('profile.preferences.timezones.pacific')}</SelectItem>
                    <SelectItem value="utc-5">{t('profile.preferences.timezones.eastern')}</SelectItem>
                    <SelectItem value="utc+8">{t('profile.preferences.timezones.taipei')}</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>{t('profile.preferences.currency')}</Label>
                <Select value={currency} onValueChange={setCurrency}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="usd">{t('profile.preferences.currencies.usd')}</SelectItem>
                    <SelectItem value="twd">{t('profile.preferences.currencies.twd')}</SelectItem>
                    <SelectItem value="eur">{t('profile.preferences.currencies.eur')}</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <Button onClick={() => void handleSavePreferences()}>{t('common:actions.save')}</Button>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="notifications">
          <Card>
            <CardHeader>
              <CardTitle>{t('profile.tabs.notifications')}</CardTitle>
              <CardDescription>{t('profile.notifications.description')}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="flex items-center justify-between">
                <div>
                  <Label>{t('profile.notifications.email.title')}</Label>
                  <p className="text-sm text-muted-foreground">
                    {t('profile.notifications.email.description')}
                  </p>
                </div>
                <Switch
                  checked={emailNotifications}
                  onCheckedChange={setEmailNotifications}
                />
              </div>
              <Separator />
              <div className="flex items-center justify-between">
                <div>
                  <Label>{t('profile.notifications.sms.title')}</Label>
                  <p className="text-sm text-muted-foreground">
                    {t('profile.notifications.sms.description')}
                  </p>
                </div>
                <Switch
                  checked={smsNotifications}
                  onCheckedChange={setSmsNotifications}
                />
              </div>
              <Button onClick={() => void handleSavePreferences()}>{t('common:actions.save')}</Button>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="security">
          <Card>
            <CardHeader>
              <CardTitle>{t('profile.tabs.security')}</CardTitle>
              <CardDescription>{t('profile.security.description')}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-2">
                <Label>{t('profile.security.currentPassword')}</Label>
                <Input
                  type="password"
                  placeholder={t('profile.security.currentPasswordPlaceholder')}
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label>{t('profile.security.newPassword')}</Label>
                <Input
                  type="password"
                  placeholder={t('profile.security.newPasswordPlaceholder')}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label>{t('profile.security.confirmPassword')}</Label>
                <Input
                  type="password"
                  placeholder={t('profile.security.confirmPasswordPlaceholder')}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                />
              </div>
              {securityNotice ? (
                <p className="text-sm text-muted-foreground" role="status">
                  {securityNotice}
                </p>
              ) : null}
              <Button onClick={() => void handleUpdatePassword()}>{t('profile.security.updatePassword')}</Button>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}
