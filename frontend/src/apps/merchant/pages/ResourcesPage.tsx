import { useCallback, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Plus, Search, MoreHorizontal, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { useAuthStore } from '@/shared/stores/authStore'
import {
  createMerchantService,
  createMerchantResource,
  deleteMerchantService,
  deleteMerchantResource,
  fetchMerchantServices,
  fetchMerchantResources,
  isMerchantPortalApiError,
  type MerchantServiceDto,
  type MerchantResourceDto,
} from '@/shared/lib/merchantPortalApi'
import { resolveDemoImageUrl } from '@/shared/lib/demoMedia'

const statusColors: Record<string, string> = {
  active: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
  inactive: 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-200',
  draft: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200',
}

export function ResourcesPage() {
  const { t } = useTranslation(['merchant', 'common'])
  const merchantId = Number(useAuthStore((s) => s.user?.tenantId))
  const [searchQuery, setSearchQuery] = useState('')
  const [services, setServices] = useState<MerchantServiceDto[]>([])
  const [resources, setResources] = useState<MerchantResourceDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [serviceName, setServiceName] = useState('')
  const [serviceCategory, setServiceCategory] = useState('wellness')
  const [servicePrice, setServicePrice] = useState('100')
  const [serviceDuration, setServiceDuration] = useState('60')
  const [serviceImageData, setServiceImageData] = useState<string | null>(null)

  const loadData = useCallback(async () => {
    if (!merchantId) return
    setLoading(true)
    setError('')
    try {
      const [serviceData, resourceData] = await Promise.all([
        fetchMerchantServices(merchantId),
        fetchMerchantResources(merchantId),
      ])
      setServices(serviceData)
      setResources(resourceData)
    } catch (e) {
      setError(isMerchantPortalApiError(e) ? e.message : t('common:errors.generic'))
    } finally {
      setLoading(false)
    }
  }, [merchantId, t])

  useEffect(() => {
    void loadData()
  }, [loadData])

  const filteredResources = useMemo(
    () =>
      resources.filter((resource) =>
        resource.name.toLowerCase().includes(searchQuery.toLowerCase()),
      ),
    [resources, searchQuery],
  )

  const handleServiceImageChange = (file?: File) => {
    if (!file) {
      setServiceImageData(null)
      return
    }
    const reader = new FileReader()
    reader.onload = () => {
      setServiceImageData(typeof reader.result === 'string' ? reader.result : null)
    }
    reader.onerror = () => {
      setError(t('common:errors.generic'))
    }
    reader.readAsDataURL(file)
  }

  const handleCreateService = async () => {
    if (!merchantId) return
    setError('')
    try {
      await createMerchantService(merchantId, {
        name: serviceName.trim() || `Service ${Date.now().toString().slice(-4)}`,
        category: serviceCategory.trim() || 'wellness',
        durationMinutes: Number(serviceDuration) || 60,
        price: Number(servicePrice) || 100,
        imageUrl: serviceImageData,
      })
      setServiceName('')
      setServicePrice('100')
      setServiceDuration('60')
      setServiceImageData(null)
      await loadData()
    } catch (e) {
      setError(isMerchantPortalApiError(e) ? e.message : t('common:errors.generic'))
    }
  }

  const handleDeleteService = async (serviceId: number) => {
    if (!merchantId) return
    setError('')
    try {
      await deleteMerchantService(merchantId, serviceId)
      await loadData()
    } catch (e) {
      setError(isMerchantPortalApiError(e) ? e.message : t('common:errors.generic'))
    }
  }

  const handleCreate = async () => {
    if (!merchantId) return
    setError('')
    try {
      await createMerchantResource(merchantId, {
        name: `Resource ${Date.now().toString().slice(-4)}`,
        type: 'SERVICE',
        category: 'wellness',
        capacity: 1,
        active: true,
        serviceItemsJson: '[]',
        price: 100,
      })
      await loadData()
    } catch (e) {
      setError(isMerchantPortalApiError(e) ? e.message : t('common:errors.generic'))
    }
  }

  const handleDelete = async (resourceId: number) => {
    if (!merchantId) return
    setError('')
    try {
      await deleteMerchantResource(merchantId, resourceId)
      await loadData()
    } catch (e) {
      setError(isMerchantPortalApiError(e) ? e.message : t('common:errors.generic'))
    }
  }

  if (!merchantId) {
    return <p className="text-sm text-muted-foreground">{t('bookings.noMerchantContext')}</p>
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold">{t('resources.title')}</h1>
        <Button className="gap-2" onClick={() => void handleCreate()}>
          <Plus className="h-4 w-4" />
          {t('resources.create')}
        </Button>
      </div>
      {error ? <p className="text-sm text-destructive">{error}</p> : null}

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-semibold">Services</h2>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-3 md:grid-cols-5">
            <Input
              placeholder="Service name"
              value={serviceName}
              onChange={(e) => setServiceName(e.target.value)}
            />
            <Input
              placeholder="Category"
              value={serviceCategory}
              onChange={(e) => setServiceCategory(e.target.value)}
            />
            <Input
              placeholder="Price"
              type="number"
              min={0}
              value={servicePrice}
              onChange={(e) => setServicePrice(e.target.value)}
            />
            <Input
              placeholder="Duration(min)"
              type="number"
              min={10}
              step={10}
              value={serviceDuration}
              onChange={(e) => setServiceDuration(e.target.value)}
            />
            <Input
              type="file"
              accept="image/*"
              onChange={(e) => handleServiceImageChange(e.target.files?.[0])}
            />
          </div>
          <div className="flex items-center gap-3">
            <Button className="gap-2" onClick={() => void handleCreateService()}>
              <Plus className="h-4 w-4" />
              Add Service
            </Button>
            <img
              src={resolveDemoImageUrl(serviceImageData)}
              alt="service preview"
              className="h-12 w-12 rounded border object-cover"
            />
          </div>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Image</TableHead>
                <TableHead>{t('resources.table.name')}</TableHead>
                <TableHead>{t('resources.table.category')}</TableHead>
                <TableHead>{t('resources.table.price')}</TableHead>
                <TableHead>{t('resources.table.duration')}</TableHead>
                <TableHead className="w-[50px]">{t('resources.table.actions')}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {services.map((service) => (
                <TableRow key={service.id}>
                  <TableCell>
                    <img
                      src={resolveDemoImageUrl(service.imageUrl)}
                      alt={service.name}
                      className="h-10 w-10 rounded border object-cover"
                    />
                  </TableCell>
                  <TableCell className="font-medium">{service.name}</TableCell>
                  <TableCell>{service.category}</TableCell>
                  <TableCell>${service.price}</TableCell>
                  <TableCell>{t('resources.durationMinutes', { count: service.durationMinutes })}</TableCell>
                  <TableCell>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon">
                          <MoreHorizontal className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem
                          className="gap-2 text-destructive"
                          onClick={() => void handleDeleteService(service.id)}
                        >
                          <Trash2 className="h-4 w-4" />
                          {t('common:actions.delete')}
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))}
              {!loading && services.length === 0 ? (
                <TableRow>
                  <TableCell className="text-center text-muted-foreground" colSpan={6}>
                    No services yet
                  </TableCell>
                </TableRow>
              ) : null}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <div className="flex items-center gap-4">
            <div className="relative flex-1 max-w-sm">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder={t('resources.search')}
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9"
              />
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {loading ? <p className="text-sm text-muted-foreground">{t('common:status.loading')}</p> : null}
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Image</TableHead>
                <TableHead>{t('resources.table.name')}</TableHead>
                <TableHead>{t('resources.table.category')}</TableHead>
                <TableHead>{t('resources.table.price')}</TableHead>
                <TableHead>{t('resources.table.duration')}</TableHead>
                <TableHead>{t('resources.table.status')}</TableHead>
                <TableHead className="w-[50px]">{t('resources.table.actions')}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredResources.map((resource) => (
                <TableRow key={resource.id}>
                  <TableCell>
                    <img
                      src={resolveDemoImageUrl(resource.imageUrl)}
                      alt=""
                      className="h-10 w-10 rounded border object-cover"
                    />
                  </TableCell>
                  <TableCell className="font-medium">{resource.name}</TableCell>
                  <TableCell>{resource.category}</TableCell>
                  <TableCell>${resource.price}</TableCell>
                  <TableCell>{t('resources.durationMinutes', { count: 60 })}</TableCell>
                  <TableCell>
                    <Badge className={statusColors[resource.active ? 'active' : 'inactive']}>
                      {t(`resources.status.${resource.active ? 'active' : 'inactive'}`)}
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
                        <DropdownMenuItem
                          className="gap-2 text-destructive"
                          onClick={() => void handleDelete(resource.id)}
                        >
                          <Trash2 className="h-4 w-4" />
                          {t('common:actions.delete')}
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))}
              {!loading && filteredResources.length === 0 ? (
                <TableRow>
                  <TableCell className="text-center text-muted-foreground" colSpan={7}>
                    {t('resources.search')}
                  </TableCell>
                </TableRow>
              ) : null}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  )
}
