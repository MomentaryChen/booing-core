import { useCallback, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Plus, Search, Copy } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Switch } from '@/components/ui/switch'
import { useToast } from '@/components/ui/use-toast'
import { useAuthStore } from '@/shared/stores/authStore'
import {
  batchUpdateMerchantResourceBusinessHours,
  batchUpdateMerchantResourcePrice,
  batchUpdateMerchantResourceStatus,
  cloneMerchantService,
  createMerchantResource,
  createMerchantService,
  fetchMerchantResources,
  fetchMerchantServices,
  isMerchantPortalApiError,
  toggleMerchantServiceActive,
  updateMerchantResource,
  type MerchantResourceDto,
  type MerchantServiceDto,
} from '@/shared/lib/merchantPortalApi'

const statusClassName: Record<string, string> = {
  ACTIVE: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
  MAINTENANCE: 'bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-200',
  FULLY_BOOKED: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200',
}

export function ResourcesPage() {
  const { t } = useTranslation(['merchant', 'common'])
  const { toast } = useToast()
  const merchantId = Number(useAuthStore((s) => s.user?.tenantId))
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [services, setServices] = useState<MerchantServiceDto[]>([])
  const [resources, setResources] = useState<MerchantResourceDto[]>([])
  const [query, setQuery] = useState('')
  const [selectedServiceId, setSelectedServiceId] = useState<number | null>(null)
  const [selectedResourceId, setSelectedResourceId] = useState<number | null>(null)
  const [selectedBatchIds, setSelectedBatchIds] = useState<number[]>([])
  const [activeTab, setActiveTab] = useState<'basic' | 'schedule' | 'pricing'>('basic')
  const [page, setPage] = useState(0)
  const [size] = useState(20)
  const [total, setTotal] = useState(0)

  const notifyError = useCallback(
    (e: unknown) => {
      const message = isMerchantPortalApiError(e) ? e.message : t('common:errors.generic')
      setError(message)
      toast({
        variant: 'destructive',
        title: t('common:errors.generic'),
        description: message,
      })
    },
    [t, toast],
  )

  const loadData = useCallback(async () => {
    if (!merchantId) return
    setLoading(true)
    setError('')
    try {
      const [serviceData, resourceData] = await Promise.all([
        fetchMerchantServices(merchantId),
        fetchMerchantResources(merchantId, { page, size }),
      ])
      setServices(serviceData)
      setResources(resourceData.items)
      setTotal(resourceData.total)
      if (selectedServiceId == null && serviceData.length > 0) {
        setSelectedServiceId(serviceData[0].id)
      }
    } catch (e) {
      notifyError(e)
    } finally {
      setLoading(false)
    }
  }, [merchantId, notifyError, page, selectedServiceId, size])

  useEffect(() => {
    void loadData()
  }, [loadData])

  const filteredServices = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return services
    return services.filter((s) => s.name.toLowerCase().includes(q) || s.category.toLowerCase().includes(q))
  }, [query, services])

  const resourcesOfSelectedService = useMemo(() => {
    if (!selectedServiceId) return []
    return resources.filter((resource) => {
      if (!resource.name.toLowerCase().includes(query.toLowerCase())) {
        return false
      }
      try {
        const serviceIds = JSON.parse(resource.serviceItemsJson ?? '[]') as number[]
        return Array.isArray(serviceIds) && serviceIds.includes(selectedServiceId)
      } catch {
        return false
      }
    })
  }, [query, resources, selectedServiceId])

  const selectedResource = useMemo(
    () => resources.find((resource) => resource.id === selectedResourceId) ?? null,
    [resources, selectedResourceId],
  )

  const handleCreateService = async () => {
    if (!merchantId) return
    try {
      await createMerchantService(merchantId, {
        name: `Service ${Date.now().toString().slice(-4)}`,
        category: 'wellness',
        durationMinutes: 60,
        price: 100,
      })
      toast({ title: t('common:status.success') })
      await loadData()
    } catch (e) {
      notifyError(e)
    }
  }

  const handleCreateResource = async () => {
    if (!merchantId || !selectedServiceId) return
    try {
      await createMerchantResource(merchantId, {
        name: `Resource ${Date.now().toString().slice(-4)}`,
        type: 'SERVICE',
        category: 'GENERAL',
        capacity: 1,
        active: true,
        serviceItemsJson: JSON.stringify([selectedServiceId]),
        price: 100,
      })
      toast({ title: t('common:status.success') })
      await loadData()
    } catch (e) {
      notifyError(e)
    }
  }

  const handleCloneService = async (serviceId: number) => {
    if (!merchantId) return
    try {
      await cloneMerchantService(merchantId, serviceId, { nameSuffix: '(Clone)' })
      toast({ title: t('common:status.success') })
      await loadData()
    } catch (e) {
      notifyError(e)
    }
  }

  const handleToggleService = async (serviceId: number, active: boolean) => {
    if (!merchantId) return
    try {
      await toggleMerchantServiceActive(merchantId, serviceId, active)
      await loadData()
    } catch (e) {
      notifyError(e)
    }
  }

  const handleBatchStatus = async (status: 'ACTIVE' | 'MAINTENANCE') => {
    if (!merchantId || selectedBatchIds.length === 0) return
    try {
      await batchUpdateMerchantResourceStatus(merchantId, { resourceIds: selectedBatchIds, status })
      toast({ title: t('common:status.success') })
      setSelectedBatchIds([])
      await loadData()
    } catch (e) {
      notifyError(e)
    }
  }

  const handleBatchPrice = async () => {
    if (!merchantId || selectedBatchIds.length === 0) return
    const value = Number(window.prompt('Batch price', '100'))
    if (!Number.isFinite(value) || value < 0) return
    try {
      await batchUpdateMerchantResourcePrice(merchantId, { resourceIds: selectedBatchIds, price: value })
      toast({ title: t('common:status.success') })
      setSelectedBatchIds([])
      await loadData()
    } catch (e) {
      notifyError(e)
    }
  }

  const handleBatchHours = async () => {
    if (!merchantId || selectedBatchIds.length === 0) return
    const value = window.prompt('Business hours JSON', '[{"day":"MONDAY","start":"09:00","end":"18:00"}]')
    if (!value) return
    try {
      await batchUpdateMerchantResourceBusinessHours(merchantId, {
        resourceIds: selectedBatchIds,
        businessHoursJson: value,
      })
      toast({ title: t('common:status.success') })
      setSelectedBatchIds([])
      await loadData()
    } catch (e) {
      notifyError(e)
    }
  }

  const handleSaveBasicInfo = async () => {
    if (!merchantId || !selectedResource) return
    const nextName = window.prompt('Resource name', selectedResource.name)
    if (!nextName) return
    try {
      await updateMerchantResource(merchantId, selectedResource.id, { name: nextName.trim() })
      toast({ title: t('common:status.success') })
      await loadData()
    } catch (e) {
      notifyError(e)
    }
  }

  if (!merchantId) {
    return <p className="text-sm text-muted-foreground">{t('bookings.noMerchantContext')}</p>
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-3xl font-bold">{t('resources.title')}</h1>
        <div className="flex gap-2">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input value={query} onChange={(e) => setQuery(e.target.value)} className="w-72 pl-9" placeholder="Search services" />
          </div>
          <Button onClick={() => void handleCreateService()}>
            <Plus className="mr-1 h-4 w-4" />
            Add Service
          </Button>
        </div>
      </div>

      {error ? <p className="text-sm text-destructive">{error}</p> : null}
      {loading ? <p className="text-sm text-muted-foreground">{t('common:status.loading')}</p> : null}

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
        <Card className="lg:col-span-4">
          <CardHeader className="text-sm font-semibold">Service Overview</CardHeader>
          <CardContent className="space-y-3">
            {filteredServices.map((service) => (
              <button
                type="button"
                key={service.id}
                className={`w-full rounded border px-3 py-2 text-left ${selectedServiceId === service.id ? 'border-primary' : 'border-border'}`}
                onClick={() => setSelectedServiceId(service.id)}
              >
                <div className="flex items-center justify-between gap-2">
                  <p className="font-medium">{service.name}</p>
                  <Switch checked={service.active} onCheckedChange={(v) => void handleToggleService(service.id, v)} />
                </div>
                <p className="text-xs text-muted-foreground">{service.category}</p>
                <p className="text-xs text-muted-foreground">Resources: {service.resourceCount ?? 0}</p>
                <div className="mt-2 flex gap-2">
                  <Button size="sm" variant="outline" onClick={() => void handleCloneService(service.id)}>
                    <Copy className="mr-1 h-3 w-3" />
                    Clone
                  </Button>
                  <Button size="sm" variant="outline" onClick={() => setActiveTab('schedule')}>
                    Edit Schedule
                  </Button>
                </div>
              </button>
            ))}
          </CardContent>
        </Card>

        <Card className="lg:col-span-8">
          <CardHeader className="flex flex-row items-center justify-between">
            <span className="text-sm font-semibold">Resource Management</span>
            <Button size="sm" onClick={() => void handleCreateResource()}>
              <Plus className="mr-1 h-4 w-4" />
              Add Resource
            </Button>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex gap-2">
              <Button size="sm" variant={activeTab === 'basic' ? 'default' : 'outline'} onClick={() => setActiveTab('basic')}>Basic Info</Button>
              <Button size="sm" variant={activeTab === 'schedule' ? 'default' : 'outline'} onClick={() => setActiveTab('schedule')}>Schedule</Button>
              <Button size="sm" variant={activeTab === 'pricing' ? 'default' : 'outline'} onClick={() => setActiveTab('pricing')}>Price Rules</Button>
            </div>

            <div className="max-h-72 space-y-2 overflow-auto rounded border p-2">
              {resourcesOfSelectedService.map((resource) => {
                const selected = selectedBatchIds.includes(resource.id)
                return (
                  <label key={resource.id} className="flex items-center justify-between rounded border p-2">
                    <div
                      className="min-w-0 cursor-pointer"
                      onClick={() => setSelectedResourceId(resource.id)}
                      role="button"
                      tabIndex={0}
                    >
                      <p className="truncate font-medium">{resource.name}</p>
                      <p className="text-xs text-muted-foreground">{resource.category}</p>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge className={statusClassName[resource.status] ?? ''}>{resource.status}</Badge>
                      <input
                        type="checkbox"
                        checked={selected}
                        onChange={(e) =>
                          setSelectedBatchIds((prev) =>
                            e.target.checked ? [...prev, resource.id] : prev.filter((id) => id !== resource.id),
                          )
                        }
                      />
                    </div>
                  </label>
                )
              })}
            </div>

            {selectedBatchIds.length > 0 ? (
              <div className="rounded border p-3">
                <p className="text-sm font-medium">Batch Actions ({selectedBatchIds.length})</p>
                <div className="mt-2 flex flex-wrap gap-2">
                  <Button size="sm" variant="outline" onClick={() => void handleBatchPrice()}>Batch Price</Button>
                  <Button size="sm" variant="outline" onClick={() => void handleBatchStatus('ACTIVE')}>Set Active</Button>
                  <Button size="sm" variant="outline" onClick={() => void handleBatchStatus('MAINTENANCE')}>Set Maintenance</Button>
                  <Button size="sm" variant="outline" onClick={() => void handleBatchHours()}>Batch Business Hours</Button>
                </div>
              </div>
            ) : null}

            <div className="rounded border p-3">
              <p className="text-sm font-medium">
                {selectedResource ? `Selected: ${selectedResource.name}` : 'Select a resource'}
              </p>
              {selectedResource ? (
                <div className="mt-2 space-y-2 text-sm">
                  <p>Status: {selectedResource.status}</p>
                  <p>Price: ${selectedResource.price}</p>
                  <p>Business Hours JSON: {selectedResource.businessHoursJson || '[]'}</p>
                  {activeTab === 'basic' ? <Button size="sm" onClick={() => void handleSaveBasicInfo()}>Save Basic Info</Button> : null}
                </div>
              ) : null}
            </div>

            <div className="flex items-center justify-end gap-2 text-xs text-muted-foreground">
              <Button size="sm" variant="outline" onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0}>Prev</Button>
              <span>
                Page {page + 1} / {Math.max(1, Math.ceil(total / size))}
              </span>
              <Button size="sm" variant="outline" onClick={() => setPage((p) => p + 1)} disabled={(page + 1) * size >= total}>
                Next
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
