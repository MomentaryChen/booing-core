import { Outlet } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { LanguageSwitcher } from '@/shared/components/LanguageSwitcher'

export function AuthLayout() {
  const { t } = useTranslation('common')

  return (
    <div className="min-h-screen flex flex-col bg-background">
      {/* Header */}
      <header className="flex items-center justify-between px-6 py-4">
        <div className="text-xl font-semibold text-foreground">
          {t('appName')}
        </div>
        <LanguageSwitcher />
      </header>

      {/* Main Content */}
      <main className="flex flex-1 items-center justify-center px-4 py-8">
        <Outlet />
      </main>

      {/* Footer */}
      <footer className="py-4 text-center text-sm text-muted-foreground">
        <p>&copy; {new Date().getFullYear()} {t('appName')}. All rights reserved.</p>
      </footer>
    </div>
  )
}
