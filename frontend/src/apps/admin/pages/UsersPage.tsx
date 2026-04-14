import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Users, Search, Plus, MoreHorizontal, Shield, ShieldCheck, ShieldAlert } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  fetchSystemUsers,
  isSystemAdminApiError,
  putSystemUserStatus,
  type SystemUserSummaryDto,
} from '@/shared/lib/systemAdminApi';

export default function UsersPage() {
  const { t } = useTranslation(['admin', 'common']);
  const [searchQuery, setSearchQuery] = useState('');
  const [users, setUsers] = useState<SystemUserSummaryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    async function loadUsers() {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchSystemUsers();
        if (mounted) {
          setUsers(data);
        }
      } catch (e) {
        if (!mounted) {
          return;
        }
        if (isSystemAdminApiError(e)) {
          setError(e.message);
        } else {
          setError(t('common:errors.generic'));
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    }
    void loadUsers();
    return () => {
      mounted = false;
    };
  }, [t]);

  const filteredUsers = useMemo(
    () =>
      users.filter(
        (user) =>
          user.username.toLowerCase().includes(searchQuery.toLowerCase()) ||
          user.primaryRole.toLowerCase().includes(searchQuery.toLowerCase())
      ),
    [searchQuery, users],
  );

  const getRoleIcon = (role: string) => {
    switch (role) {
      case 'ADMIN':
      case 'SYSTEM_ADMIN':
        return <ShieldAlert className="h-4 w-4 text-destructive" />;
      case 'MERCHANT':
      case 'MERCHANT_OWNER':
      case 'MERCHANT_STAFF':
        return <ShieldCheck className="h-4 w-4 text-primary" />;
      default:
        return <Shield className="h-4 w-4 text-muted-foreground" />;
    }
  };

  const getRoleBadge = (role: string) => {
    const variants: Record<string, 'default' | 'secondary' | 'outline'> = {
      ADMIN: 'default',
      SYSTEM_ADMIN: 'default',
      MERCHANT: 'secondary',
      MERCHANT_OWNER: 'secondary',
      MERCHANT_STAFF: 'secondary',
      CLIENT: 'outline',
      CLIENT_USER: 'outline',
    };
    const roleKey = role.toLowerCase();
    return (
      <Badge variant={variants[role] || 'outline'} className="gap-1">
        {getRoleIcon(role)}
        {t(`admin:users.roles.${roleKey}`, { defaultValue: role })}
      </Badge>
    );
  };

  const getInitials = (name: string) => {
    return name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase();
  };

  const formatLastLogin = (value: string | null) => {
    if (!value) {
      return '-';
    }
    return new Date(value).toLocaleString();
  };

  const handleToggleStatus = async (user: SystemUserSummaryDto) => {
    setError(null);
    try {
      const updated = await putSystemUserStatus(user.id, !user.enabled);
      setUsers((current) =>
        current.map((row) => (row.id === user.id ? { ...row, enabled: updated.enabled } : row)),
      );
    } catch (e) {
      if (isSystemAdminApiError(e)) {
        setError(e.message);
      } else {
        setError(t('common:errors.generic'));
      }
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            {t('admin:users.title')}
          </h1>
          <p className="text-muted-foreground">{t('admin:users.description')}</p>
        </div>
        <Button>
          <Plus className="mr-2 h-4 w-4" />
          {t('admin:users.addUser')}
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Users className="h-5 w-5" />
            {t('admin:users.allUsers')}
          </CardTitle>
          <CardDescription>
            {t('admin:users.totalUsers', { count: users.length })}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {loading && <p className="mb-4 text-sm text-muted-foreground">{t('common:status.loading')}</p>}
          {error && <p className="mb-4 text-sm text-destructive">{error}</p>}
          <div className="mb-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder={t('admin:users.searchPlaceholder')}
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9"
              />
            </div>
          </div>

          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t('admin:users.table.user')}</TableHead>
                <TableHead>{t('admin:users.table.role')}</TableHead>
                <TableHead>{t('admin:users.table.tenant')}</TableHead>
                <TableHead>{t('admin:users.table.lastLogin')}</TableHead>
                <TableHead>{t('admin:users.table.status')}</TableHead>
                <TableHead className="w-[50px]"></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredUsers.map((user) => (
                <TableRow key={user.id}>
                  <TableCell>
                    <div className="flex items-center gap-3">
                      <Avatar className="h-8 w-8">
                        <AvatarFallback>{getInitials(user.username)}</AvatarFallback>
                      </Avatar>
                      <div>
                        <div className="font-medium">{user.username}</div>
                        <div className="text-sm text-muted-foreground">
                          {user.username}
                        </div>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>{getRoleBadge(user.primaryRole)}</TableCell>
                  <TableCell>{user.primaryMerchantId ? `#${user.primaryMerchantId}` : '-'}</TableCell>
                  <TableCell>{formatLastLogin(user.lastLoginAt)}</TableCell>
                  <TableCell>
                    <Badge
                      variant={user.enabled ? 'outline' : 'destructive'}
                      className={
                        user.enabled
                          ? 'text-emerald-600 border-emerald-600'
                          : ''
                      }
                    >
                      {t(`admin:users.status.${user.enabled ? 'active' : 'suspended'}`)}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon">
                          <MoreHorizontal className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem>
                          {t('common:actions.view')}
                        </DropdownMenuItem>
                        <DropdownMenuItem>
                          {t('common:actions.edit')}
                        </DropdownMenuItem>
                        <DropdownMenuItem>
                          {t('admin:users.actions.resetPassword')}
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          className="text-destructive"
                          onClick={() => void handleToggleStatus(user)}
                        >
                          {user.enabled
                            ? t('admin:users.actions.suspend')
                            : t('admin:users.actions.activate')}
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))}
              {!loading && filteredUsers.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground">
                    {t('admin:users.totalUsers', { count: 0 })}
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}
