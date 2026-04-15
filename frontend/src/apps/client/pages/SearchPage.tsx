import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Search, Clock, SlidersHorizontal, Loader2, MapPin, Users, Building2, CalendarDays } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  fetchClientCategories,
  fetchClientResources,
  isClientCatalogApiError,
  type ClientCategoryDto,
  type ClientCatalogResourceDto,
} from '@/shared/lib/clientCatalogApi'
import { resolveDemoImageUrl } from '@/shared/lib/demoMedia'

type SortKey = 'relevance' | 'priceAsc' | 'priceDesc' | 'rating'
type ResourceType = 'all' | 'service' | 'space' | 'class'
type AvailabilityKey = 'all' | 'today'

export function SearchPage() {
  const { t } = useTranslation(['client', 'common'])
  const [searchParams, setSearchParams] = useSearchParams()
  const [resources, setResources] = useState<ClientCatalogResourceDto[]>([])
  const [categories, setCategories] = useState<ClientCategoryDto[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [searchQuery, setSearchQuery] = useState(searchParams.get('q') ?? '')
  const [selectedCategory, setSelectedCategory] = useState(searchParams.get('category') ?? 'all')
  const [resourceType, setResourceType] = useState<ResourceType>(
    (searchParams.get('resourceType') as ResourceType) ?? 'all',
  )
  const [availability, setAvailability] = useState<AvailabilityKey>(
    (searchParams.get('availability') as AvailabilityKey) ?? 'all',
  )
  const [sortBy, setSortBy] = useState<SortKey>((searchParams.get('sort') as SortKey) ?? 'relevance')
  const [page, setPage] = useState(Number(searchParams.get('page') ?? '0') || 0)
  const size = 12

  useEffect(() => {
    const q = new URLSearchParams()
    if (searchQuery.trim()) q.set('q', searchQuery.trim())
    if (selectedCategory !== 'all') q.set('category', selectedCategory)
    if (resourceType !== 'all') q.set('resourceType', resourceType)
    if (availability !== 'all') q.set('availability', availability)
    if (sortBy !== 'relevance') q.set('sort', sortBy)
    if (page > 0) q.set('page', String(page))
    setSearchParams(q, { replace: true })
  }, [searchQuery, selectedCategory, resourceType, availability, sortBy, page, setSearchParams])

  useEffect(() => {
    let cancelled = false
    const run = async () => {
      setLoading(true)
      setLoadError('')
      try {
        const [resourcePage, categoryList] = await Promise.all([
          fetchClientResources({
            q: searchQuery,
            category: selectedCategory,
            resourceType,
            sort: sortBy,
            page,
            size,
          }),
          fetchClientCategories(),
        ])
        if (!cancelled) {
          setResources(
            resourcePage.items.filter((item) =>
              availability === 'today' ? (item.availabilityLabel ?? 'available') !== 'unavailable' : true,
            ),
          )
          setCategories(categoryList)
        }
      } catch (e) {
        if (!cancelled) {
          setLoadError(isClientCatalogApiError(e) ? e.message : t('common:errors.generic'))
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    void run()
    return () => {
      cancelled = true
    }
  }, [searchQuery, selectedCategory, resourceType, availability, sortBy, page, t])

  const filteredResources = resources
  const hasPrev = page > 0
  const hasNext = filteredResources.length >= size
  const resourceTypeLabels = useMemo(
    () => ({
      all: t('search.resourceType.all'),
      service: t('search.resourceType.service'),
      space: t('search.resourceType.space'),
      class: t('search.resourceType.class'),
    }),
    [t],
  )

  const resolveVariant = (r: ClientCatalogResourceDto): ResourceType => {
    const raw = (r.resourceType ?? '').toLowerCase()
    if (raw.includes('room') || raw.includes('space') || raw.includes('spot') || raw.includes('court')) return 'space'
    if (raw.includes('class') || raw.includes('session') || raw.includes('course')) return 'class'
    return 'service'
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="mb-8 text-3xl font-bold">{t('search.title')}</h1>

      <div className="mb-6 rounded-lg border bg-card p-4">
        <p className="mb-3 text-sm font-medium text-muted-foreground">{t('search.filterBar.title')}</p>
        <div className="mb-4 flex flex-wrap items-center gap-2">
          {(['all', 'service', 'space', 'class'] as const).map((type) => (
            <Button
              key={type}
              type="button"
              variant={resourceType === type ? 'default' : 'outline'}
              size="sm"
              onClick={() => {
                setResourceType(type)
                setPage(0)
              }}
            >
              {resourceTypeLabels[type]}
            </Button>
          ))}
        </div>
        <div className="flex flex-col gap-4 md:flex-row">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder={t('search.placeholder')}
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value)
                setPage(0)
              }}
              className="pl-9"
            />
          </div>
          <div className="flex flex-wrap gap-2">
            <Select
              value={selectedCategory}
              onValueChange={(value) => {
                setSelectedCategory(value)
                setPage(0)
              }}
            >
              <SelectTrigger className="w-[150px]">
                <SlidersHorizontal className="mr-2 h-4 w-4" />
                <SelectValue placeholder={t('search.filters.category')} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem key="all" value="all">
                  {t('search.categories.all')}
                </SelectItem>
                {categories.map((category) => (
                  <SelectItem key={category.key} value={category.key}>
                    {t(`search.categories.${category.key}`, { defaultValue: category.label })}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Select
              value={availability}
              onValueChange={(value) => {
                setAvailability(value as AvailabilityKey)
                setPage(0)
              }}
            >
              <SelectTrigger className="w-[150px]">
                <Clock className="mr-2 h-4 w-4" />
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">{t('search.availability.all')}</SelectItem>
                <SelectItem value="today">{t('search.availability.today')}</SelectItem>
              </SelectContent>
            </Select>
            <Select value={sortBy} onValueChange={(v) => setSortBy(v as SortKey)}>
              <SelectTrigger className="w-[180px]">
                <SelectValue placeholder={t('search.sortBy.label')} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="relevance">{t('search.sortBy.relevance')}</SelectItem>
                <SelectItem value="priceAsc">{t('search.sortBy.priceAsc')}</SelectItem>
                <SelectItem value="priceDesc">{t('search.sortBy.priceDesc')}</SelectItem>
                <SelectItem value="rating">{t('search.sortBy.rating')}</SelectItem>
              </SelectContent>
            </Select>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => {
                setSearchQuery('')
                setSelectedCategory('all')
                setResourceType('all')
                setAvailability('all')
                setSortBy('relevance')
                setPage(0)
              }}
            >
              {t('search.filterBar.clear')}
            </Button>
          </div>
        </div>
      </div>

      <div className="mb-8 flex items-center justify-between rounded-md bg-muted/40 p-3 text-sm text-muted-foreground">
        <span className="inline-flex items-center gap-2">
          <MapPin className="h-4 w-4" />
          {t('search.mapHint')}
        </span>
        <Badge variant="outline">{t('search.discoveryMode')}</Badge>
      </div>

      <div className="mb-8 flex flex-col gap-4 md:flex-row">
        <div className="relative flex-1">
          <p className="text-sm text-muted-foreground">{t('search.results', { count: filteredResources.length })}</p>
        </div>
      </div>

      {loadError ? <p className="mb-4 text-sm text-destructive">{loadError}</p> : null}

      {loading ? (
        <div className="flex justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      ) : (
        <>
          {filteredResources.length > 0 ? (
            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
              {filteredResources.map((r) => (
                <Card key={r.id} className="flex flex-col">
                  <CardHeader>
                    <Badge variant="secondary" className="mb-2 w-fit">
                      {resourceTypeLabels[resolveVariant(r)]}
                    </Badge>
                    <CardTitle className="text-lg">{r.name}</CardTitle>
                    <p className="text-sm text-muted-foreground">{r.merchantName}</p>
                  </CardHeader>
                  <CardContent className="mt-auto flex flex-col gap-3">
                    <Link
                      to={`/booking/${r.id}`}
                      className="flex items-center justify-between gap-3 rounded-md border px-3 py-2 text-sm hover:bg-muted"
                    >
                      <span className="flex min-w-0 flex-1 items-center gap-3">
                        <img
                          src={resolveDemoImageUrl(r.imageUrl)}
                          alt=""
                          className="h-9 w-9 shrink-0 rounded border object-cover"
                        />
                        <span className="truncate">{r.name}</span>
                      </span>
                      <span className="flex shrink-0 items-center gap-2 text-muted-foreground">
                        <Clock className="h-3 w-3" />${r.price}
                      </span>
                    </Link>
                    <div className="rounded-md border bg-muted/40 px-3 py-2 text-xs text-muted-foreground">
                      {resolveVariant(r) === 'service' ? (
                        <span className="inline-flex items-center gap-2">
                          <Clock className="h-3 w-3" />
                          {t('search.feed.fastestSlot')}
                        </span>
                      ) : null}
                      {resolveVariant(r) === 'space' ? (
                        <span className="inline-flex items-center gap-2">
                          <Building2 className="h-3 w-3" />
                          {t('search.feed.remainingSpaces', { count: r.remainingUnits ?? 0 })}
                        </span>
                      ) : null}
                      {resolveVariant(r) === 'class' ? (
                        <span className="inline-flex items-center gap-2">
                          <Users className="h-3 w-3" />
                          {t('search.feed.remainingSeats', { count: r.seatsLeft ?? 0 })}
                        </span>
                      ) : null}
                    </div>
                    <Badge variant="outline" className="w-fit">
                      <CalendarDays className="mr-1 h-3 w-3" />
                      {r.availabilityLabel ? t(`search.availabilityLabels.${r.availabilityLabel}`) : t('search.availabilityLabels.available')}
                    </Badge>
                  </CardContent>
                </Card>
              ))}
            </div>
          ) : (
            <div className="py-12 text-center">
              <p className="text-muted-foreground">{t('search.noResults')}</p>
              <Button
                variant="outline"
                className="mt-4"
                onClick={() => {
                  setSearchQuery('')
                  setSelectedCategory('all')
                  setResourceType('all')
                  setAvailability('all')
                  setSortBy('relevance')
                  setPage(0)
                }}
              >
                {t('common:actions.reset')}
              </Button>
            </div>
          )}
          <div className="mt-8 flex items-center justify-between">
            <Button type="button" variant="outline" disabled={!hasPrev || loading} onClick={() => setPage((p) => Math.max(0, p - 1))}>
              {t('search.pagination.prev')}
            </Button>
            <span className="text-sm text-muted-foreground">
              {t('search.pagination.page', { page: page + 1 })}
            </span>
            <Button type="button" variant="outline" disabled={!hasNext || loading} onClick={() => setPage((p) => p + 1)}>
              {t('search.pagination.next')}
            </Button>
          </div>
        </>
      )}
    </div>
  )
}
