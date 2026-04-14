import { Outlet, Link, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Home, Search, CalendarCheck, User, LogOut } from 'lucide-react'
import { LanguageSwitcher } from '@/shared/components/LanguageSwitcher'
import { AuthContextSwitcher } from '@/shared/components/AuthContextSwitcher'
import { useAuth } from '@/shared/hooks/useAuth'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { cn } from '@/shared/lib/utils'

const navItems = [
  { path: '/', icon: Home, labelKey: 'nav.home' },
  { path: '/search', icon: Search, labelKey: 'nav.search' },
  { path: '/my-bookings', icon: CalendarCheck, labelKey: 'nav.bookings' },
]

export function ClientLayout() {
  const { t } = useTranslation('common')
  const { user, logout } = useAuth()
  const location = useLocation()

  const getInitials = (name: string) => {
    return name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2)
  }

  return (
    <div className="min-h-screen flex flex-col bg-background">
      {/* Header */}
      <header className="sticky top-0 z-50 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container mx-auto flex h-16 items-center justify-between px-4">
          <div className="flex items-center gap-8">
            <Link to="/" className="text-xl font-semibold text-foreground">
              {t('appName')}
            </Link>
            <nav className="hidden md:flex items-center gap-1">
              {navItems.map(({ path, icon: Icon, labelKey }) => (
                <Link key={path} to={path}>
                  <Button
                    variant="ghost"
                    className={cn(
                      'gap-2',
                      location.pathname === path && 'bg-accent'
                    )}
                  >
                    <Icon className="h-4 w-4" />
                    {t(labelKey)}
                  </Button>
                </Link>
              ))}
            </nav>
          </div>

          <div className="flex items-center gap-2">
            <AuthContextSwitcher />
            <LanguageSwitcher />
            <Button variant="ghost" size="sm" onClick={logout} className="gap-2">
              <LogOut className="h-4 w-4" />
              {t('actions.logout')}
            </Button>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" className="relative h-9 w-9 rounded-full">
                  <Avatar className="h-9 w-9">
                    <AvatarFallback>
                      {user?.name ? getInitials(user.name) : 'U'}
                    </AvatarFallback>
                  </Avatar>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56">
                <div className="flex items-center gap-2 p-2">
                  <Avatar className="h-8 w-8">
                    <AvatarFallback>
                      {user?.name ? getInitials(user.name) : 'U'}
                    </AvatarFallback>
                  </Avatar>
                  <div className="flex flex-col">
                    <span className="text-sm font-medium">{user?.name}</span>
                    <span className="text-xs text-muted-foreground">{user?.email}</span>
                  </div>
                </div>
                <DropdownMenuSeparator />
                <DropdownMenuItem asChild>
                  <Link to="/profile" className="flex items-center gap-2">
                    <User className="h-4 w-4" />
                    {t('nav.profile')}
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  onClick={logout}
                  className="flex items-center gap-2 text-destructive"
                >
                  <LogOut className="h-4 w-4" />
                  {t('actions.logout')}
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </header>

      {/* Mobile Navigation */}
      <nav className="md:hidden fixed bottom-0 left-0 right-0 z-50 border-t bg-background">
        <div className="flex items-center justify-around h-16">
          {navItems.map(({ path, icon: Icon, labelKey }) => (
            <Link
              key={path}
              to={path}
              className={cn(
                'flex flex-col items-center gap-1 px-4 py-2 text-muted-foreground',
                location.pathname === path && 'text-primary'
              )}
            >
              <Icon className="h-5 w-5" />
              <span className="text-xs">{t(labelKey)}</span>
            </Link>
          ))}
          <Link
            to="/profile"
            className={cn(
              'flex flex-col items-center gap-1 px-4 py-2 text-muted-foreground',
              location.pathname === '/profile' && 'text-primary'
            )}
          >
            <User className="h-5 w-5" />
            <span className="text-xs">{t('nav.profile')}</span>
          </Link>
        </div>
      </nav>

      {/* Main Content */}
      <main className="flex-1 pb-20 md:pb-0">
        <Outlet />
      </main>
    </div>
  )
}
