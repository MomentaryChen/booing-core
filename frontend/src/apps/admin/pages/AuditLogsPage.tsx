import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { FileText, Search, Filter, Download } from 'lucide-react';
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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  fetchSystemAuditLogs,
  isSystemAdminApiError,
  type SystemAuditLogDto,
} from '@/shared/lib/systemAdminApi';

export default function AuditLogsPage() {
  const { t } = useTranslation(['admin', 'common']);
  const [searchQuery, setSearchQuery] = useState('');
  const [levelFilter, setLevelFilter] = useState('all');
  const [logs, setLogs] = useState<SystemAuditLogDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    async function loadLogs() {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchSystemAuditLogs();
        if (mounted) {
          setLogs(data);
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
    void loadLogs();
    return () => {
      mounted = false;
    };
  }, [t]);

  const getLevel = (action: string) => {
    const normalized = action.toLowerCase();
    if (normalized.includes('error') || normalized.includes('fail') || normalized.includes('deny')) {
      return 'error';
    }
    if (normalized.includes('cancel') || normalized.includes('suspend')) {
      return 'warning';
    }
    return 'info';
  };

  const filteredLogs = useMemo(
    () =>
      logs.filter((log) => {
        const level = getLevel(log.action);
        const matchesSearch =
          log.actor.toLowerCase().includes(searchQuery.toLowerCase()) ||
          log.action.toLowerCase().includes(searchQuery.toLowerCase()) ||
          log.detail.toLowerCase().includes(searchQuery.toLowerCase());
        const matchesLevel = levelFilter === 'all' || level === levelFilter;
        return matchesSearch && matchesLevel;
      }),
    [levelFilter, logs, searchQuery],
  );

  const getLevelBadge = (level: string) => {
    const variants: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
      info: 'secondary',
      warning: 'outline',
      error: 'destructive',
    };
    const colors: Record<string, string> = {
      info: '',
      warning: 'text-amber-600 border-amber-600',
      error: '',
    };
    return (
      <Badge variant={variants[level]} className={colors[level]}>
        {t(`admin:auditLogs.levels.${level}`)}
      </Badge>
    );
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            {t('admin:auditLogs.title')}
          </h1>
          <p className="text-muted-foreground">
            {t('admin:auditLogs.description')}
          </p>
        </div>
        <Button variant="outline">
          <Download className="mr-2 h-4 w-4" />
          {t('admin:auditLogs.export')}
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <FileText className="h-5 w-5" />
            {t('admin:auditLogs.recentActivity')}
          </CardTitle>
          <CardDescription>
            {t('admin:auditLogs.totalLogs', { count: logs.length })}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {loading && <p className="mb-4 text-sm text-muted-foreground">{t('common:status.loading')}</p>}
          {error && <p className="mb-4 text-sm text-destructive">{error}</p>}
          <div className="mb-4 flex gap-4">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder={t('admin:auditLogs.searchPlaceholder')}
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9"
              />
            </div>
            <Select value={levelFilter} onValueChange={setLevelFilter}>
              <SelectTrigger className="w-[180px]">
                <Filter className="mr-2 h-4 w-4" />
                <SelectValue placeholder={t('admin:auditLogs.filterByLevel')} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">
                  {t('admin:auditLogs.allLevels')}
                </SelectItem>
                <SelectItem value="info">
                  {t('admin:auditLogs.levels.info')}
                </SelectItem>
                <SelectItem value="warning">
                  {t('admin:auditLogs.levels.warning')}
                </SelectItem>
                <SelectItem value="error">
                  {t('admin:auditLogs.levels.error')}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t('admin:auditLogs.table.timestamp')}</TableHead>
                <TableHead>{t('admin:auditLogs.table.user')}</TableHead>
                <TableHead>{t('admin:auditLogs.table.action')}</TableHead>
                <TableHead>{t('admin:auditLogs.table.resource')}</TableHead>
                <TableHead>{t('admin:auditLogs.table.details')}</TableHead>
                <TableHead>{t('admin:auditLogs.table.level')}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredLogs.map((log) => (
                <TableRow key={log.id}>
                  <TableCell className="font-mono text-sm">
                    {new Date(log.createdAt).toLocaleString()}
                  </TableCell>
                  <TableCell>{log.actor}</TableCell>
                  <TableCell>
                    <Badge variant="outline">{t(`admin:auditLogs.actions.${log.action}`, { defaultValue: log.action })}</Badge>
                  </TableCell>
                  <TableCell>{t(`admin:auditLogs.resources.${log.targetType}`, { defaultValue: log.targetType })}</TableCell>
                  <TableCell className="max-w-[300px] truncate">
                    {log.detail}
                  </TableCell>
                  <TableCell>{getLevelBadge(getLevel(log.action))}</TableCell>
                </TableRow>
              ))}
              {!loading && filteredLogs.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground">
                    {t('admin:auditLogs.totalLogs', { count: 0 })}
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
