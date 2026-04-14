import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Building2, Search, Plus, MoreHorizontal, Check, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
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
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  fetchSystemMerchants,
  isSystemAdminApiError,
  putSystemMerchantStatus,
  type SystemMerchantDto,
} from '@/shared/lib/systemAdminApi';

export default function TenantsPage() {
  const { t } = useTranslation(['admin', 'common']);
  const [searchQuery, setSearchQuery] = useState('');
  const [tenants, setTenants] = useState<SystemMerchantDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    async function loadTenants() {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchSystemMerchants();
        if (mounted) {
          setTenants(data);
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
    void loadTenants();
    return () => {
      mounted = false;
    };
  }, [t]);

  const filteredTenants = useMemo(
    () =>
      tenants.filter((tenant) =>
        tenant.name.toLowerCase().includes(searchQuery.toLowerCase())
      ),
    [searchQuery, tenants],
  );

  const getPlanBadge = (plan: string) => {
    const variants: Record<string, 'default' | 'secondary' | 'outline'> = {
      starter: 'outline',
      professional: 'secondary',
      enterprise: 'default',
    };
    return <Badge variant={variants[plan] || 'outline'}>{t(`admin:tenants.plans.${plan}`)}</Badge>;
  };

  const resolvePlan = (serviceLimit?: number) => {
    if (serviceLimit == null || serviceLimit <= 5) {
      return 'starter';
    }
    if (serviceLimit <= 20) {
      return 'professional';
    }
    return 'enterprise';
  };

  const getStatusBadge = (active: boolean) => {
    if (active) {
      return (
        <Badge variant="outline" className="text-emerald-600 border-emerald-600">
          <Check className="mr-1 h-3 w-3" />
          {t('admin:tenants.status.active')}
        </Badge>
      );
    }
    return (
      <Badge variant="outline" className="text-destructive border-destructive">
        <X className="mr-1 h-3 w-3" />
        {t('admin:tenants.status.suspended')}
      </Badge>
    );
  };

  const handleToggleStatus = async (tenant: SystemMerchantDto) => {
    setError(null);
    try {
      const updated = await putSystemMerchantStatus(tenant.id, !tenant.active);
      setTenants((current) =>
        current.map((row) => (row.id === tenant.id ? updated : row)),
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
            {t('admin:tenants.title')}
          </h1>
          <p className="text-muted-foreground">
            {t('admin:tenants.description')}
          </p>
        </div>
        <Button>
          <Plus className="mr-2 h-4 w-4" />
          {t('admin:tenants.addTenant')}
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Building2 className="h-5 w-5" />
            {t('admin:tenants.allTenants')}
          </CardTitle>
          <CardDescription>
            {t('admin:tenants.totalTenants', { count: tenants.length })}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {loading && <p className="mb-4 text-sm text-muted-foreground">{t('common:status.loading')}</p>}
          {error && <p className="mb-4 text-sm text-destructive">{error}</p>}
          <div className="mb-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder={t('admin:tenants.searchPlaceholder')}
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9"
              />
            </div>
          </div>

          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t('admin:tenants.table.name')}</TableHead>
                <TableHead>{t('admin:tenants.table.plan')}</TableHead>
                <TableHead>{t('admin:tenants.table.users')}</TableHead>
                <TableHead>{t('admin:tenants.table.bookings')}</TableHead>
                <TableHead>{t('admin:tenants.table.status')}</TableHead>
                <TableHead>{t('admin:tenants.table.created')}</TableHead>
                <TableHead className="w-[50px]"></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredTenants.map((tenant) => (
                <TableRow key={tenant.id}>
                  <TableCell className="font-medium">{tenant.name}</TableCell>
                  <TableCell>{getPlanBadge(resolvePlan(tenant.serviceLimit))}</TableCell>
                  <TableCell>-</TableCell>
                  <TableCell>-</TableCell>
                  <TableCell>{getStatusBadge(tenant.active)}</TableCell>
                  <TableCell>-</TableCell>
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
                        <DropdownMenuItem
                          className="text-destructive"
                          onClick={() => void handleToggleStatus(tenant)}
                        >
                          {tenant.active
                            ? t('admin:tenants.actions.suspend')
                            : t('admin:tenants.actions.activate')}
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))}
              {!loading && filteredTenants.length === 0 && (
                <TableRow>
                  <TableCell colSpan={7} className="text-center text-muted-foreground">
                    {t('admin:tenants.totalTenants', { count: 0 })}
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
