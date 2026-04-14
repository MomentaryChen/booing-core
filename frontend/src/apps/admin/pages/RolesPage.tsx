import { useTranslation } from 'react-i18next';
import { Shield, Plus, Check, X } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
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
  fetchSystemRbacRoles,
  fetchSystemUsers,
  isSystemAdminApiError,
} from '@/shared/lib/systemAdminApi';

const permissionKeys = [
  'manageUsers',
  'manageTenants',
  'manageRoles',
  'viewAuditLogs',
  'manageSettings',
] as const;

export default function RolesPage() {
  const { t } = useTranslation(['admin', 'common']);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [roles, setRoles] = useState<Array<{ code: string; users: number; permissions: Record<string, boolean> }>>([]);

  useEffect(() => {
    let mounted = true;
    async function loadRoles() {
      setLoading(true);
      setError(null);
      try {
        const [roleCatalog, users] = await Promise.all([
          fetchSystemRbacRoles(),
          fetchSystemUsers(),
        ]);
        if (!mounted) {
          return;
        }
        const rows = roleCatalog.map((role) => {
          const permissions = permissionKeys.reduce<Record<string, boolean>>((acc, key) => {
            acc[key] = false;
            return acc;
          }, {});
          role.permissions.forEach((permission) => {
            if (permission.includes('users')) {
              permissions.manageUsers = true;
            }
            if (permission.includes('merchant.registry') || permission.includes('dashboard')) {
              permissions.manageTenants = true;
            }
            if (permission.includes('rbac') || permission.includes('users.write')) {
              permissions.manageRoles = true;
            }
            if (permission.includes('dashboard')) {
              permissions.viewAuditLogs = true;
            }
            if (permission.includes('settings')) {
              permissions.manageSettings = true;
            }
          });
          return {
            code: role.roleCode,
            users: users.filter((user) => user.roleCodes.includes(role.roleCode)).length,
            permissions,
          };
        });
        setRoles(rows);
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
    void loadRoles();
    return () => {
      mounted = false;
    };
  }, [t]);

  const permissionsByKey = useMemo(
    () =>
      permissionKeys.reduce<Record<string, string[]>>((acc, key) => {
        acc[key] = roles
          .filter((role) => role.permissions[key])
          .map((role) => role.code);
        return acc;
      }, {}),
    [roles],
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            {t('admin:roles.title')}
          </h1>
          <p className="text-muted-foreground">{t('admin:roles.description')}</p>
        </div>
        <Button>
          <Plus className="mr-2 h-4 w-4" />
          {t('admin:roles.addRole')}
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Shield className="h-5 w-5" />
            {t('admin:roles.allRoles')}
          </CardTitle>
          <CardDescription>
            {t('admin:roles.totalRoles', { count: roles.length })}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {loading && <p className="mb-4 text-sm text-muted-foreground">{t('common:status.loading')}</p>}
          {error && <p className="mb-4 text-sm text-destructive">{error}</p>}
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t('admin:roles.table.role')}</TableHead>
                <TableHead>{t('admin:roles.table.users')}</TableHead>
                {permissionKeys.map((key) => (
                  <TableHead key={key} className="text-center">
                    {t(`admin:roles.permissions.${key}`)}
                  </TableHead>
                ))}
              </TableRow>
            </TableHeader>
            <TableBody>
              {roles.map((role) => (
                <TableRow key={role.code}>
                  <TableCell>
                    <div>
                      <div className="font-medium">{t(`admin:roles.roleNames.${role.code.toLowerCase()}`, { defaultValue: role.code })}</div>
                      <Badge variant="outline" className="mt-1">
                        {role.code}
                      </Badge>
                    </div>
                  </TableCell>
                  <TableCell>{role.users.toLocaleString()}</TableCell>
                  {permissionKeys.map((key) => (
                    <TableCell key={key} className="text-center">
                      {role.permissions[key] ? (
                        <Check className="mx-auto h-5 w-5 text-emerald-600" />
                      ) : (
                        <X className="mx-auto h-5 w-5 text-muted-foreground" />
                      )}
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{t('admin:roles.permissionsMatrix')}</CardTitle>
          <CardDescription>
            {t('admin:roles.permissionsMatrixDescription')}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {permissionKeys.map((key) => (
              <div
                key={key}
                className="rounded-lg border p-4"
              >
                <h3 className="font-medium">
                  {t(`admin:roles.permissions.${key}`)}
                </h3>
                <p className="mt-1 text-sm text-muted-foreground">
                  {t(`admin:roles.permissionDescriptions.${key}`)}
                </p>
                <div className="mt-3 flex flex-wrap gap-1">
                  {permissionsByKey[key].map((roleCode) => (
                    <Badge key={roleCode} variant="secondary">
                      {roleCode}
                    </Badge>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
