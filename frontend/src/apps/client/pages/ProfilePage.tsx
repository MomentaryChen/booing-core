import { useEffect, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  User,
  Bell,
  Shield,
  Settings,
  Mail,
  Phone,
  MapPin,
  Calendar,
  Globe,
  Moon,
  Sun,
  Lock,
  Key,
  Smartphone,
  Eye,
  EyeOff,
  Check,
  X,
  Save,
  AlertCircle,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { useAuth } from '@/shared/hooks/useAuth'
import {
  fetchClientProfile,
  fetchClientProfilePreferences,
  isClientProfileApiError,
  patchClientProfilePassword,
  putClientProfile,
  putClientProfilePreferences,
} from '@/shared/lib/clientProfileApi'

type TabType = 'personal' | 'preferences' | 'notifications' | 'security'

interface FormState {
  firstName: string
  lastName: string
  email: string
  phone: string
  location: string
  bio: string
}

interface PreferencesState {
  language: string
  timezone: string
  currency: string
  theme: 'light' | 'dark' | 'system'
}

interface NotificationState {
  emailNotifications: boolean
  pushNotifications: boolean
  smsNotifications: boolean
  marketingEmails: boolean
  securityAlerts: boolean
  productUpdates: boolean
}

interface SecurityState {
  currentPassword: string
  newPassword: string
  confirmPassword: string
  twoFactorEnabled: boolean
}

interface FeedbackState {
  show: boolean
  type: 'success' | 'error' | 'info'
  message: string
}

export function ProfilePage() {
  const { user } = useAuth()
  const [activeTab, setActiveTab] = useState<TabType>('personal')
  const [showPassword, setShowPassword] = useState(false)
  const [feedback, setFeedback] = useState<FeedbackState>({ show: false, type: 'success', message: '' })
  const [loading, setLoading] = useState(true)

  const [formData, setFormData] = useState<FormState>({
    firstName: '',
    lastName: '',
    email: user?.email ?? '',
    phone: '',
    location: '',
    bio: '',
  })

  const [preferences, setPreferences] = useState<PreferencesState>({
    language: 'English',
    timezone: 'PST (UTC-8)',
    currency: 'USD',
    theme: 'system',
  })

  const [notifications, setNotifications] = useState<NotificationState>({
    emailNotifications: true,
    pushNotifications: true,
    smsNotifications: false,
    marketingEmails: false,
    securityAlerts: true,
    productUpdates: true,
  })

  const [security, setSecurity] = useState<SecurityState>({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
    twoFactorEnabled: false,
  })

  const showFeedback = (type: 'success' | 'error' | 'info', message: string) => {
    setFeedback({ show: true, type, message })
    setTimeout(() => setFeedback({ show: false, type: 'success', message: '' }), 3000)
  }

  useEffect(() => {
    let mounted = true
    const loadData = async () => {
      setLoading(true)
      try {
        const [profile, prefs] = await Promise.all([
          fetchClientProfile(),
          fetchClientProfilePreferences(),
        ])
        if (!mounted) return
        const fullName = profile.suggestedName ?? user?.name ?? ''
        const [firstName, ...restName] = fullName.trim().split(/\s+/).filter(Boolean)
        const lastName = restName.join(' ')

        setFormData((prev) => ({
          ...prev,
          firstName: firstName ?? '',
          lastName,
          email: user?.email ?? prev.email,
          phone: profile.suggestedContact ?? '',
          location: profile.location ?? '',
          bio: profile.bio ?? '',
        }))
        setPreferences((prev) => ({
          ...prev,
          language: prefs.language ?? prev.language,
          timezone: prefs.timezone ?? prev.timezone,
          currency: prefs.currency ?? prev.currency,
          theme: (prefs.theme as PreferencesState['theme']) ?? prev.theme,
        }))
        setNotifications((prev) => ({
          ...prev,
          emailNotifications: prefs.notificationPrefs.email ?? prev.emailNotifications,
          smsNotifications: prefs.notificationPrefs.sms ?? prev.smsNotifications,
          pushNotifications: prefs.notificationPrefs.push ?? prev.pushNotifications,
          marketingEmails: prefs.notificationPrefs.marketing ?? prev.marketingEmails,
          securityAlerts: prefs.notificationPrefs.securityAlerts ?? prev.securityAlerts,
          productUpdates: prefs.notificationPrefs.productUpdates ?? prev.productUpdates,
        }))
        setSecurity((prev) => ({
          ...prev,
          twoFactorEnabled: profile.twoFactorEnabled ?? prev.twoFactorEnabled,
        }))
      } catch (error) {
        if (!mounted) return
        showFeedback(
          'error',
          isClientProfileApiError(error) ? error.message : 'Failed to load profile data.',
        )
      } finally {
        if (mounted) {
          setLoading(false)
        }
      }
    }

    void loadData()
    return () => {
      mounted = false
    }
  }, [user?.email, user?.name])

  const handleSavePersonal = async () => {
    try {
      const suggestedName = `${formData.firstName} ${formData.lastName}`.trim()
      const updated = await putClientProfile({
        suggestedName,
        suggestedContact: formData.phone,
        location: formData.location,
        bio: formData.bio,
      })
      const normalizedName = updated.suggestedName ?? suggestedName
      const [firstName, ...restName] = normalizedName.trim().split(/\s+/).filter(Boolean)
      setFormData((prev) => ({
        ...prev,
        firstName: firstName ?? '',
        lastName: restName.join(' '),
        phone: updated.suggestedContact ?? prev.phone,
        location: updated.location ?? prev.location,
        bio: updated.bio ?? prev.bio,
      }))
      showFeedback('success', 'Personal information updated successfully!')
    } catch (error) {
      showFeedback(
        'error',
        isClientProfileApiError(error) ? error.message : 'Failed to update personal information.',
      )
    }
  }

  const handleSavePreferences = async () => {
    try {
      const updated = await putClientProfilePreferences({
        language: preferences.language,
        timezone: preferences.timezone,
        currency: preferences.currency,
        theme: preferences.theme,
      })
      setPreferences((prev) => ({
        ...prev,
        language: updated.language ?? prev.language,
        timezone: updated.timezone ?? prev.timezone,
        currency: updated.currency ?? prev.currency,
        theme: (updated.theme as PreferencesState['theme']) ?? prev.theme,
      }))
      showFeedback('success', 'Preferences saved successfully!')
    } catch (error) {
      showFeedback(
        'error',
        isClientProfileApiError(error) ? error.message : 'Failed to update preferences.',
      )
    }
  }

  const handleSaveNotifications = async () => {
    try {
      const updated = await putClientProfilePreferences({
        notificationPrefs: {
          email: notifications.emailNotifications,
          sms: notifications.smsNotifications,
          push: notifications.pushNotifications,
          marketing: notifications.marketingEmails,
          securityAlerts: notifications.securityAlerts,
          productUpdates: notifications.productUpdates,
        },
      })
      setNotifications((prev) => ({
        ...prev,
        emailNotifications: updated.notificationPrefs.email ?? prev.emailNotifications,
        smsNotifications: updated.notificationPrefs.sms ?? prev.smsNotifications,
        pushNotifications: updated.notificationPrefs.push ?? prev.pushNotifications,
        marketingEmails: updated.notificationPrefs.marketing ?? prev.marketingEmails,
        securityAlerts: updated.notificationPrefs.securityAlerts ?? prev.securityAlerts,
        productUpdates: updated.notificationPrefs.productUpdates ?? prev.productUpdates,
      }))
      showFeedback('success', 'Notification settings updated!')
    } catch (error) {
      showFeedback(
        'error',
        isClientProfileApiError(error) ? error.message : 'Failed to update notification settings.',
      )
    }
  }

  const handleSaveSecurity = async () => {
    try {
      const passwordProvided =
        !!security.currentPassword || !!security.newPassword || !!security.confirmPassword

      if (passwordProvided) {
        if (!security.currentPassword || !security.newPassword || !security.confirmPassword) {
          showFeedback('error', 'Please fill in all password fields.')
          return
        }
        if (security.newPassword !== security.confirmPassword) {
          showFeedback('error', 'Passwords do not match!')
          return
        }
        if (security.newPassword.length < 8) {
          showFeedback('error', 'New password must be at least 8 characters.')
          return
        }
        await patchClientProfilePassword({
          currentPassword: security.currentPassword,
          newPassword: security.newPassword,
        })
      }

      await putClientProfile({ twoFactorEnabled: security.twoFactorEnabled })
      showFeedback('success', 'Security settings updated successfully!')
      setSecurity((prev) => ({
        ...prev,
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
      }))
    } catch (error) {
      showFeedback(
        'error',
        isClientProfileApiError(error) ? error.message : 'Failed to update password.',
      )
    }
  }

  const tabs = [
    { id: 'personal' as TabType, label: 'Personal Info', icon: User },
    { id: 'preferences' as TabType, label: 'Preferences', icon: Settings },
    { id: 'notifications' as TabType, label: 'Notifications', icon: Bell },
    { id: 'security' as TabType, label: 'Security', icon: Shield },
  ]

  return (
    <div className="min-h-screen bg-background px-4 py-6 md:px-8 md:py-8">
      <div className="mx-auto max-w-7xl">
        {loading ? (
          <Alert className="mb-6 border-blue-500 bg-blue-50 dark:bg-blue-950">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>Loading profile...</AlertDescription>
          </Alert>
        ) : null}
        <div className="mb-8">
          <h1 className="mb-2 text-3xl font-bold text-foreground">Account Settings</h1>
          <p className="text-muted-foreground">Manage your account settings and preferences</p>
        </div>

        <Card className="mb-6">
          <CardContent className="pt-6">
            <div className="flex flex-col items-start gap-6 md:flex-row md:items-center">
              <Avatar className="h-24 w-24">
                <AvatarImage
                  src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(
                    `${formData.firstName} ${formData.lastName}`.trim() || 'User',
                  )}`}
                />
                <AvatarFallback>
                  {(formData.firstName[0] ?? 'U').toUpperCase()}
                  {(formData.lastName[0] ?? '').toUpperCase()}
                </AvatarFallback>
              </Avatar>
              <div className="flex-1">
                <div className="mb-2 flex items-center gap-3">
                  <h2 className="text-2xl font-semibold text-foreground">
                    {formData.firstName} {formData.lastName}
                  </h2>
                  <Badge
                    variant="secondary"
                    className="bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-100"
                  >
                    <Check className="mr-1 h-3 w-3" />
                    Verified
                  </Badge>
                </div>
                <p className="mb-3 text-muted-foreground">{formData.email}</p>
                <div className="flex flex-wrap gap-2">
                  <Badge variant="outline" className="flex items-center gap-1">
                    <MapPin className="h-3 w-3" />
                    {formData.location}
                  </Badge>
                  <Badge variant="outline" className="flex items-center gap-1">
                    <Calendar className="h-3 w-3" />
                    Joined March 2024
                  </Badge>
                </div>
              </div>
              <Button variant="outline">Change Photo</Button>
            </div>
          </CardContent>
        </Card>

        <div className="mb-6 overflow-x-auto">
          <div className="inline-flex min-w-full space-x-1 rounded-lg border border-border bg-muted p-1 md:min-w-0">
            {tabs.map((tab) => {
              const Icon = tab.icon
              return (
                <div key={tab.id} className="flex flex-1 items-center md:flex-initial">
                  <input
                    type="radio"
                    name="profile-tabs"
                    id={tab.id}
                    className="peer hidden"
                    checked={activeTab === tab.id}
                    onChange={() => setActiveTab(tab.id)}
                  />
                  <label
                    htmlFor={tab.id}
                    className="flex w-full cursor-pointer items-center justify-center gap-2 whitespace-nowrap rounded-md px-4 py-2.5 text-muted-foreground transition-all duration-200 peer-checked:bg-background peer-checked:text-foreground peer-checked:shadow-sm md:justify-start"
                  >
                    <Icon className="h-4 w-4" />
                    <span className="text-sm font-medium">{tab.label}</span>
                  </label>
                </div>
              )
            })}
          </div>
        </div>

        <AnimatePresence>
          {feedback.show && (
            <motion.div
              initial={{ opacity: 0, y: -20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              className="mb-6"
            >
              <Alert
                className={
                  feedback.type === 'success'
                    ? 'border-green-500 bg-green-50 dark:bg-green-950'
                    : feedback.type === 'error'
                      ? 'border-red-500 bg-red-50 dark:bg-red-950'
                      : 'border-blue-500 bg-blue-50 dark:bg-blue-950'
                }
              >
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>{feedback.message}</AlertDescription>
              </Alert>
            </motion.div>
          )}
        </AnimatePresence>

        <AnimatePresence mode="wait">
          <motion.div
            key={activeTab}
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            transition={{ duration: 0.2 }}
          >
            {activeTab === 'personal' && (
              <div className="space-y-6">
                <Card>
                  <CardHeader>
                    <CardTitle>Basic Information</CardTitle>
                    <CardDescription>Update your personal details and contact information</CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                      <div className="space-y-2">
                        <Label htmlFor="firstName">First Name</Label>
                        <Input
                          id="firstName"
                          value={formData.firstName}
                          onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="lastName">Last Name</Label>
                        <Input
                          id="lastName"
                          value={formData.lastName}
                          onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
                        />
                      </div>
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="email" className="flex items-center gap-2">
                        <Mail className="h-4 w-4" />
                        Email Address
                      </Label>
                      <Input
                        id="email"
                        type="email"
                        value={formData.email}
                        disabled
                      />
                      <p className="text-xs text-muted-foreground">Email comes from your account login profile.</p>
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="phone" className="flex items-center gap-2">
                        <Phone className="h-4 w-4" />
                        Phone Number
                      </Label>
                      <Input
                        id="phone"
                        value={formData.phone}
                        onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="location" className="flex items-center gap-2">
                        <MapPin className="h-4 w-4" />
                        Location
                      </Label>
                      <Input
                        id="location"
                        value={formData.location}
                        onChange={(e) => setFormData({ ...formData, location: e.target.value })}
                      />
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle>About</CardTitle>
                    <CardDescription>Tell us a bit about yourself</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-2">
                      <Label htmlFor="bio">Bio</Label>
                      <textarea
                        id="bio"
                        className="flex min-h-[100px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                        value={formData.bio}
                        onChange={(e) => setFormData({ ...formData, bio: e.target.value })}
                      />
                    </div>
                  </CardContent>
                </Card>

                <div className="flex justify-end gap-3">
                  <Button variant="outline">Cancel</Button>
                  <Button onClick={handleSavePersonal} className="flex items-center gap-2">
                    <Save className="h-4 w-4" />
                    Save Changes
                  </Button>
                </div>
              </div>
            )}

            {activeTab === 'preferences' && (
              <div className="space-y-6">
                <Card>
                  <CardHeader>
                    <CardTitle>Language &amp; Region</CardTitle>
                    <CardDescription>Customize your language and regional settings</CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="space-y-2">
                      <Label htmlFor="language" className="flex items-center gap-2">
                        <Globe className="h-4 w-4" />
                        Language
                      </Label>
                      <Input
                        id="language"
                        value={preferences.language}
                        onChange={(e) => setPreferences({ ...preferences, language: e.target.value })}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="timezone" className="flex items-center gap-2">
                        <Calendar className="h-4 w-4" />
                        Timezone
                      </Label>
                      <Input
                        id="timezone"
                        value={preferences.timezone}
                        onChange={(e) => setPreferences({ ...preferences, timezone: e.target.value })}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="currency" className="flex items-center gap-2">
                        <Calendar className="h-4 w-4" />
                        Currency
                      </Label>
                      <Input
                        id="currency"
                        value={preferences.currency}
                        onChange={(e) => setPreferences({ ...preferences, currency: e.target.value })}
                      />
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle>Appearance</CardTitle>
                    <CardDescription>Customize how the interface looks</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      <Label>Theme</Label>
                      <div className="flex gap-3">
                        {(['light', 'dark', 'system'] as const).map((theme) => (
                          <button
                            type="button"
                            key={theme}
                            onClick={() => setPreferences({ ...preferences, theme })}
                            className={`flex-1 rounded-lg border-2 p-4 transition-all ${
                              preferences.theme === theme
                                ? 'border-primary bg-primary/5'
                                : 'border-border hover:border-primary/50'
                            }`}
                          >
                            <div className="flex flex-col items-center gap-2">
                              {theme === 'light' && <Sun className="h-5 w-5" />}
                              {theme === 'dark' && <Moon className="h-5 w-5" />}
                              {theme === 'system' && <Settings className="h-5 w-5" />}
                              <span className="text-sm font-medium capitalize">{theme}</span>
                            </div>
                          </button>
                        ))}
                      </div>
                    </div>
                  </CardContent>
                </Card>

                <div className="flex justify-end gap-3">
                  <Button variant="outline">Cancel</Button>
                  <Button onClick={handleSavePreferences} className="flex items-center gap-2">
                    <Save className="h-4 w-4" />
                    Save Changes
                  </Button>
                </div>
              </div>
            )}

            {activeTab === 'notifications' && (
              <div className="space-y-6">
                <Card>
                  <CardHeader>
                    <CardTitle>Communication Preferences</CardTitle>
                    <CardDescription>Choose how you want to receive notifications</CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="flex items-center justify-between py-3">
                      <div className="space-y-0.5">
                        <Label htmlFor="email-notif" className="flex items-center gap-2 text-base">
                          <Mail className="h-4 w-4" />
                          Email Notifications
                        </Label>
                        <p className="text-sm text-muted-foreground">Receive notifications via email</p>
                      </div>
                      <Switch
                        id="email-notif"
                        checked={notifications.emailNotifications}
                        onCheckedChange={(checked) => setNotifications({ ...notifications, emailNotifications: checked })}
                      />
                    </div>
                    <Separator />
                    <div className="flex items-center justify-between py-3">
                      <div className="space-y-0.5">
                        <Label htmlFor="push-notif" className="flex items-center gap-2 text-base">
                          <Bell className="h-4 w-4" />
                          Push Notifications
                        </Label>
                        <p className="text-sm text-muted-foreground">Receive push notifications on your devices</p>
                      </div>
                      <Switch
                        id="push-notif"
                        checked={notifications.pushNotifications}
                        onCheckedChange={(checked) => setNotifications({ ...notifications, pushNotifications: checked })}
                      />
                    </div>
                    <Separator />
                    <div className="flex items-center justify-between py-3">
                      <div className="space-y-0.5">
                        <Label htmlFor="sms-notif" className="flex items-center gap-2 text-base">
                          <Smartphone className="h-4 w-4" />
                          SMS Notifications
                        </Label>
                        <p className="text-sm text-muted-foreground">Receive notifications via text message</p>
                      </div>
                      <Switch
                        id="sms-notif"
                        checked={notifications.smsNotifications}
                        onCheckedChange={(checked) => setNotifications({ ...notifications, smsNotifications: checked })}
                      />
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle>Notification Types</CardTitle>
                    <CardDescription>Select which types of notifications you want to receive</CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="flex items-center justify-between py-3">
                      <div className="space-y-0.5">
                        <Label htmlFor="security-alerts" className="text-base">
                          Security Alerts
                        </Label>
                        <p className="text-sm text-muted-foreground">Important security updates and alerts</p>
                      </div>
                      <Switch
                        id="security-alerts"
                        checked={notifications.securityAlerts}
                        onCheckedChange={(checked) => setNotifications({ ...notifications, securityAlerts: checked })}
                      />
                    </div>
                    <Separator />
                    <div className="flex items-center justify-between py-3">
                      <div className="space-y-0.5">
                        <Label htmlFor="product-updates" className="text-base">
                          Product Updates
                        </Label>
                        <p className="text-sm text-muted-foreground">New features and product announcements</p>
                      </div>
                      <Switch
                        id="product-updates"
                        checked={notifications.productUpdates}
                        onCheckedChange={(checked) => setNotifications({ ...notifications, productUpdates: checked })}
                      />
                    </div>
                    <Separator />
                    <div className="flex items-center justify-between py-3">
                      <div className="space-y-0.5">
                        <Label htmlFor="marketing" className="text-base">
                          Marketing Emails
                        </Label>
                        <p className="text-sm text-muted-foreground">Promotional content and special offers</p>
                      </div>
                      <Switch
                        id="marketing"
                        checked={notifications.marketingEmails}
                        onCheckedChange={(checked) => setNotifications({ ...notifications, marketingEmails: checked })}
                      />
                    </div>
                  </CardContent>
                </Card>
                <div className="flex justify-end gap-3">
                  <Button variant="outline">Cancel</Button>
                  <Button onClick={handleSaveNotifications} className="flex items-center gap-2">
                    <Save className="h-4 w-4" />
                    Save Changes
                  </Button>
                </div>
              </div>
            )}

            {activeTab === 'security' && (
              <div className="space-y-6">
                <Card>
                  <CardHeader>
                    <CardTitle>Change Password</CardTitle>
                    <CardDescription>Update your password to keep your account secure</CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="space-y-2">
                      <Label htmlFor="current-password" className="flex items-center gap-2">
                        <Lock className="h-4 w-4" />
                        Current Password
                      </Label>
                      <div className="relative">
                        <Input
                          id="current-password"
                          type={showPassword ? 'text' : 'password'}
                          value={security.currentPassword}
                          onChange={(e) => setSecurity({ ...security, currentPassword: e.target.value })}
                        />
                        <button
                          type="button"
                          onClick={() => setShowPassword(!showPassword)}
                          className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                        >
                          {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                        </button>
                      </div>
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="new-password" className="flex items-center gap-2">
                        <Key className="h-4 w-4" />
                        New Password
                      </Label>
                      <Input
                        id="new-password"
                        type={showPassword ? 'text' : 'password'}
                        value={security.newPassword}
                        onChange={(e) => setSecurity({ ...security, newPassword: e.target.value })}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="confirm-password">Confirm New Password</Label>
                      <Input
                        id="confirm-password"
                        type={showPassword ? 'text' : 'password'}
                        value={security.confirmPassword}
                        onChange={(e) => setSecurity({ ...security, confirmPassword: e.target.value })}
                      />
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle>Two-Factor Authentication</CardTitle>
                    <CardDescription>Add an extra layer of security to your account</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="flex items-center justify-between py-3">
                      <div className="space-y-0.5">
                        <Label htmlFor="2fa" className="flex items-center gap-2 text-base">
                          <Shield className="h-4 w-4" />
                          Enable Two-Factor Authentication
                        </Label>
                        <p className="text-sm text-muted-foreground">
                          Require a verification code in addition to your password
                        </p>
                      </div>
                      <Switch
                        id="2fa"
                        checked={security.twoFactorEnabled}
                        onCheckedChange={(checked) => setSecurity({ ...security, twoFactorEnabled: checked })}
                      />
                    </div>
                  </CardContent>
                </Card>

                <Card className="border-destructive/50">
                  <CardHeader>
                    <CardTitle className="text-destructive">Danger Zone</CardTitle>
                    <CardDescription>Irreversible actions for your account</CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-3">
                    <Button variant="outline" className="w-full justify-start text-destructive hover:text-destructive">
                      <X className="mr-2 h-4 w-4" />
                      Deactivate Account
                    </Button>
                    <Button variant="outline" className="w-full justify-start text-destructive hover:text-destructive">
                      <X className="mr-2 h-4 w-4" />
                      Delete Account
                    </Button>
                  </CardContent>
                </Card>

                <div className="flex justify-end gap-3">
                  <Button variant="outline">Cancel</Button>
                  <Button onClick={handleSaveSecurity} className="flex items-center gap-2">
                    <Save className="h-4 w-4" />
                    Save Changes
                  </Button>
                </div>
              </div>
            )}
          </motion.div>
        </AnimatePresence>
      </div>
    </div>
  )
}
