import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Search, Clock, SlidersHorizontal, Loader2 } from 'lucide-react'
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
  fetchMerchantStorefront,
  fetchVisibleMerchants,
  isClientCatalogApiError,
  type ClientMerchantCardDto,
  type ResourceItemSummaryDto,
} from '@/shared/lib/clientCatalogApi'
import { resolveDemoImageUrl } from '@/shared/lib/demoMedia'

type SortKey = 'relevance' | 'priceAsc' | 'priceDesc' | 'rating'

const CATEGORIES = ['all'] as const

export function SearchPage() {
  const { t } = useTranslation(['client', 'common'])
  const [merchants, setMerchants] = useState<ClientMerchantCardDto[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('all')
  const [sortBy, setSortBy] = useState<SortKey>('relevance')
  const [expandedSlug, setExpandedSlug] = useState<string | null>(null)
  const [expandedResources, setExpandedResources] = useState<ResourceItemSummaryDto[]>([])
  const [expandLoading, setExpandLoading] = useState(false)
  const [expandError, setExpandError] = useState('')

  useEffect(() => {
    let cancelled = false
    const run = async () => {
      setLoading(true)
      setLoadError('')
      try {
        const list = await fetchVisibleMerchants()
        if (!cancelled) setMerchants(list)
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
  }, [t])

  const q = searchQuery.toLowerCase()
  const filteredMerchants = merchants.filter((m) =>
    (m.name ?? '').toLowerCase().includes(q),
  )

  const handleToggleMerchant = async (slug: string) => {
    if (expandedSlug === slug) {
      setExpandedSlug(null)
      setExpandedResources([])
      setExpandError('')
      return
    }
    setExpandLoading(true)
    setExpandError('')
    setExpandedSlug(slug)
    try {
      const detail = await fetchMerchantStorefront(slug)
      setExpandedResources(detail.resources.filter((r) => r.active))
    } catch (e) {
      setExpandError(isClientCatalogApiError(e) ? e.message : t('common:errors.generic'))
      setExpandedResources([])
    } finally {
      setExpandLoading(false)
    }
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="mb-8 text-3xl font-bold">{t('search.title')}</h1>

      <div className="mb-8 flex flex-col gap-4 md:flex-row">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder={t('search.placeholder')}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-9"
          />
        </div>
        <div className="flex gap-2">
          <Select value={selectedCategory} onValueChange={setSelectedCategory}>
            <SelectTrigger className="w-[150px]">
              <SlidersHorizontal className="mr-2 h-4 w-4" />
              <SelectValue placeholder={t('search.filters.category')} />
            </SelectTrigger>
            <SelectContent>
              {CATEGORIES.map((category) => (
                <SelectItem key={category} value={category}>
                  {t(`search.categories.${category}`)}
                </SelectItem>
              ))}
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
        </div>
      </div>

      {loadError ? <p className="mb-4 text-sm text-destructive">{loadError}</p> : null}

      {loading ? (
        <div className="flex justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      ) : (
        <>
          <p className="mb-6 text-muted-foreground">
            {t('search.results', { count: filteredMerchants.length })}
          </p>

          {filteredMerchants.length > 0 ? (
            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
              {filteredMerchants.map((m) => (
                <Card key={m.merchantId} className="flex flex-col">
                  <CardHeader>
                    <Badge variant="secondary" className="mb-2 w-fit">
                      {t(`search.visibility.${m.visibility.toLowerCase()}`, { defaultValue: m.visibility })}
                    </Badge>
                    <CardTitle className="text-lg">{m.name}</CardTitle>
                    <p className="text-sm text-muted-foreground">/{m.slug}</p>
                  </CardHeader>
                  <CardContent className="mt-auto flex flex-col gap-3">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => void handleToggleMerchant(m.slug)}
                    >
                      {expandedSlug === m.slug ? t('search.hideResources') : t('search.showResources')}
                    </Button>
                    {expandedSlug === m.slug ? (
                      expandLoading ? (
                        <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
                      ) : expandError ? (
                        <p className="text-sm text-destructive">{expandError}</p>
                      ) : expandedResources.length === 0 ? (
                        <p className="text-sm text-muted-foreground">{t('search.noResources')}</p>
                      ) : (
                        <ul className="space-y-2">
                          {expandedResources.map((r) => (
                            <li key={r.id}>
                              <Link
                                to={`/booking/${r.id}`}
                                state={{ merchantSlug: m.slug }}
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
                            </li>
                          ))}
                        </ul>
                      )
                    ) : null}
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
                }}
              >
                {t('common:actions.reset')}
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
