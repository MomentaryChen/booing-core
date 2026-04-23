import { Link } from 'react-router-dom'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Search, ArrowRight, Scissors, Volleyball, GraduationCap, MapPin } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { useAuth } from '@/shared/hooks/useAuth'
import { enableMerchant, fetchAuthMe, isApiError } from '@/shared/lib/authContextApi'
import { isCanonicalMerchantRole } from '@/shared/lib/roleCompat'
import {
  fetchClientCategories,
  fetchFeaturedResources,
  isClientCatalogApiError,
  type ClientCategoryDto,
  type ClientCatalogResourceDto,
} from '@/shared/lib/clientCatalogApi'
import {
  fetchHomepageConfig,
  fetchHomepageSeo,
  postHomepageTrackingEvents,
  type HomepageConfigData,
} from '@/shared/lib/homepagePublicApi'
import { PageHeader } from '@/apps/client/components/PageHeader'
import { AsyncStateBlock } from '@/apps/client/components/AsyncStateBlock'

const CAMPAIGN = 'homepage_client'
const SECTION_IDS = ['nav', 'hero', 'features', 'process', 'use_cases', 'faq', 'cta', 'footer'] as const
type HomepageSectionId = (typeof SECTION_IDS)[number]

const CLIENT_SECTION_MAPPING: Record<
  'hero' | 'discovery' | 'categories' | 'featured' | 'become_merchant',
  HomepageSectionId[]
> = {
  hero: ['hero'],
  discovery: ['features'],
  categories: ['use_cases'],
  featured: ['process'],
  become_merchant: ['cta'],
}

export function HomePage() {
  const { t, i18n } = useTranslation(['client', 'common'])
  const { user } = useAuth()
  const locale = i18n.language === 'zh-TW' ? 'zh-TW' : 'en-US'
  const [loadingState, setLoadingState] = useState<'loading' | 'ready' | 'error' | 'forbidden'>('loading')
  const [merchantEnabled, setMerchantEnabled] = useState(false)
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState('')
  const [merchantName, setMerchantName] = useState('')
  const [merchantSlug, setMerchantSlug] = useState('')
  const [featuredResources, setFeaturedResources] = useState<ClientCatalogResourceDto[]>([])
  const [categories, setCategories] = useState<ClientCategoryDto[]>([])
  const [catalogError, setCatalogError] = useState('')
  const [homepageConfig, setHomepageConfig] = useState<HomepageConfigData | null>(null)
  const tenantId = user?.tenantId ?? 'default'
  const viewedSections = useRef(new Set<string>())

  useEffect(() => {
    let mounted = true
    const loadMe = async () => {
      try {
        setLoadingState('loading')
        const me = await fetchAuthMe()
        if (!mounted) return
        const hasMerchantContext = (me.availableContexts ?? []).some((ctx) =>
          isCanonicalMerchantRole(ctx.canonicalRole ?? ctx.role)
        )
        setMerchantEnabled(hasMerchantContext)
        setLoadingState('ready')
      } catch (err) {
        if (!mounted) return
        if (isApiError(err) && err.status === 403) {
          setLoadingState('forbidden')
        } else {
          setLoadingState('error')
        }
      }
    }
    void loadMe()
    return () => {
      mounted = false
    }
  }, [])

  useEffect(() => {
    let mounted = true
    const loadHomepage = async () => {
      setCatalogError('')
      try {
        const [featured, categoryList, config, seo] = await Promise.all([
          fetchFeaturedResources(6),
          fetchClientCategories(),
          fetchHomepageConfig({ tenantId, locale, pageVariant: 'default' }),
          fetchHomepageSeo({ tenantId, locale, variant: 'default' }),
        ])
        if (!mounted) return
        setFeaturedResources(featured)
        setCategories(categoryList)
        setHomepageConfig(config)
        document.title = seo.title
        let meta = document.querySelector('meta[name="description"]') as HTMLMetaElement | null
        if (!meta) {
          meta = document.createElement('meta')
          meta.name = 'description'
          document.head.appendChild(meta)
        }
        meta.content = seo.description
      } catch (err) {
        if (!mounted) return
        setCatalogError(
          isClientCatalogApiError(err) ? err.message : t('common:errors.generic'),
        )
      }
    }
    void loadHomepage()
    return () => {
      mounted = false
    }
  }, [locale, t, tenantId])

  const canSubmit = useMemo(
    () => merchantName.trim().length > 0 && merchantSlug.trim().length > 0 && !creating,
    [merchantName, merchantSlug, creating]
  )

  const onEnableMerchant = async () => {
    if (!canSubmit) return
    setCreating(true)
    setCreateError('')
    try {
      await enableMerchant({ name: merchantName.trim(), slug: merchantSlug.trim() })
      setMerchantEnabled(true)
    } catch (err) {
      if (isApiError(err) && err.status === 403) {
        setCreateError(t('home.becomeMerchant.forbidden'))
      } else {
        setCreateError(t('home.becomeMerchant.error'))
      }
    } finally {
      setCreating(false)
    }
  }

  const sectionVisible = useCallback(
    (ids: HomepageSectionId[]): boolean => {
      if (!homepageConfig) return true
      const indexed = new Set(homepageConfig.sections.map((section) => section.id))
      const hasAnyMappedSection = ids.some((id) => indexed.has(id))
      if (!hasAnyMappedSection) return true
      return homepageConfig.sections.some(
        (section) => ids.includes(section.id as HomepageSectionId) && section.enabled,
      )
    },
    [homepageConfig],
  )

  const trackEvent = useCallback(
    async (eventType: 'cta_click' | 'section_view', sectionId: string, metadata?: Record<string, unknown>) => {
      try {
        await postHomepageTrackingEvents([
          {
            eventType,
            tenantId,
            locale,
            sectionId,
            campaign: CAMPAIGN,
            pageVariant: 'default',
            metadata,
          },
        ])
      } catch {
        // Tracking should never block user flow.
      }
    },
    [locale, tenantId],
  )

  const trackCta = useCallback(
    (sectionId: string, ctaId: string, metadata?: Record<string, unknown>) =>
      trackEvent('cta_click', sectionId, { ctaId, surface: 'client_home', ...(metadata ?? {}) }),
    [trackEvent],
  )

  useEffect(() => {
    viewedSections.current.clear()
  }, [tenantId, locale])

  useEffect(() => {
    const nodes = document.querySelectorAll<HTMLElement>('[data-client-home-section]')
    if (!nodes.length) return
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting) continue
          const sectionId = (entry.target as HTMLElement).dataset.clientHomeSection
          if (!sectionId || viewedSections.current.has(sectionId)) continue
          viewedSections.current.add(sectionId)
          void trackEvent('section_view', sectionId)
        }
      },
      { threshold: 0.25 },
    )
    nodes.forEach((node) => observer.observe(node))
    return () => observer.disconnect()
  }, [trackEvent, homepageConfig, featuredResources.length, categories.length, loadingState])

  return (
    <div className="mx-auto w-full max-w-7xl space-y-10 px-4 py-6 md:py-8">
      <PageHeader title={t('home.hero.title')} subtitle={t('home.hero.subtitle')} />
      {/* Hero Section */}
      {sectionVisible(CLIENT_SECTION_MAPPING.hero) ? (
        <section className="text-center" data-client-home-section="hero">
        <Link to="/search">
          <Button
            size="lg"
            className="gap-2"
            onClick={() => {
              void trackCta('hero', 'hero_search')
            }}
          >
            <Search className="h-5 w-5" />
            {t('home.hero.cta')}
          </Button>
        </Link>
      </section>
      ) : null}

      {sectionVisible(CLIENT_SECTION_MAPPING.discovery) ? (
        <section data-client-home-section="discovery">
        <h2 className="mb-4 text-2xl font-semibold">{t('home.discovery.title')}</h2>
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          <Link
            to="/search?resourceType=service"
            className="rounded-lg border p-4 hover:border-primary/50"
            onClick={() => {
              void trackCta('discovery', 'discovery_service', { resourceType: 'service' })
            }}
          >
            <div className="mb-2 inline-flex rounded-md bg-muted p-2">
              <Scissors className="h-4 w-4" />
            </div>
            <p className="font-medium">{t('home.discovery.types.service.title')}</p>
            <p className="text-sm text-muted-foreground">{t('home.discovery.types.service.subtitle')}</p>
          </Link>
          <Link
            to="/search?resourceType=space"
            className="rounded-lg border p-4 hover:border-primary/50"
            onClick={() => {
              void trackCta('discovery', 'discovery_space', { resourceType: 'space' })
            }}
          >
            <div className="mb-2 inline-flex rounded-md bg-muted p-2">
              <Volleyball className="h-4 w-4" />
            </div>
            <p className="font-medium">{t('home.discovery.types.space.title')}</p>
            <p className="text-sm text-muted-foreground">{t('home.discovery.types.space.subtitle')}</p>
          </Link>
          <Link
            to="/search?resourceType=class"
            className="rounded-lg border p-4 hover:border-primary/50"
            onClick={() => {
              void trackCta('discovery', 'discovery_class', { resourceType: 'class' })
            }}
          >
            <div className="mb-2 inline-flex rounded-md bg-muted p-2">
              <GraduationCap className="h-4 w-4" />
            </div>
            <p className="font-medium">{t('home.discovery.types.class.title')}</p>
            <p className="text-sm text-muted-foreground">{t('home.discovery.types.class.subtitle')}</p>
          </Link>
          <Link
            to="/search?availability=today"
            className="rounded-lg border p-4 hover:border-primary/50"
            onClick={() => {
              void trackCta('discovery', 'discovery_nearby', { availability: 'today' })
            }}
          >
            <div className="mb-2 inline-flex rounded-md bg-muted p-2">
              <MapPin className="h-4 w-4" />
            </div>
            <p className="font-medium">{t('home.discovery.types.nearby.title')}</p>
            <p className="text-sm text-muted-foreground">{t('home.discovery.types.nearby.subtitle')}</p>
          </Link>
        </div>
      </section>
      ) : null}

      {/* Categories */}
      {sectionVisible(CLIENT_SECTION_MAPPING.categories) ? (
        <section data-client-home-section="categories">
        <h2 className="text-2xl font-semibold mb-6">{t('home.categories.title')}</h2>
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
          {categories.map((category) => (
            <Link
              key={category.key}
              to={`/search?category=${category.key}`}
              onClick={() => {
                void trackCta('categories', 'category_select', { categoryKey: category.key })
              }}
            >
              <Card className="hover:border-primary/50 transition-colors cursor-pointer">
                <CardContent className="p-4 text-center">
                  <div className="font-medium">{t(`home.dynamic.categories.${category.key}`)}</div>
                  <div className="text-sm text-muted-foreground">
                    {t('home.dynamic.merchantsCount', { count: category.count })}
                  </div>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      </section>
      ) : null}

      {/* Featured Services */}
      {sectionVisible(CLIENT_SECTION_MAPPING.featured) ? (
        <section className="space-y-4" data-client-home-section="featured">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-2xl font-semibold">{t('home.featured.title')}</h2>
          <Link
            to="/search"
            onClick={() => {
              void trackCta('featured', 'featured_view_all')
            }}
          >
            <Button variant="ghost" className="gap-2">
              {t('home.featured.viewAll')}
              <ArrowRight className="h-4 w-4" />
            </Button>
          </Link>
        </div>
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
          {featuredResources.map((resource) => (
            <Link
              key={resource.id}
              to={`/booking/${resource.id}`}
              onClick={() => {
                void trackCta('featured', 'featured_open_resource', {
                  resourceId: resource.id,
                  category: resource.category,
                })
              }}
            >
              <Card className="hover:shadow-md transition-shadow cursor-pointer h-full">
                <CardHeader className="p-0">
                  <div className="aspect-video bg-muted rounded-t-lg flex items-center justify-center">
                    <span className="text-muted-foreground text-sm">{resource.merchantName}</span>
                  </div>
                </CardHeader>
                <CardContent className="p-4">
                  <Badge variant="secondary" className="mb-2">
                    {resource.category}
                  </Badge>
                  <CardTitle className="text-lg mb-2">{resource.name}</CardTitle>
                  <div className="text-sm text-muted-foreground">
                    ${resource.price}
                  </div>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
        {catalogError ? (
          <AsyncStateBlock
            type="error"
            title={t('common:errors.generic')}
            description={catalogError}
          />
        ) : null}
      </section>
      ) : null}

      {sectionVisible(CLIENT_SECTION_MAPPING.become_merchant) ? (
        <section data-client-home-section="become_merchant">
        <Card>
          <CardHeader>
            <CardTitle>{t('home.becomeMerchant.title')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {loadingState === 'loading' ? (
              <AsyncStateBlock type="loading" description={t('home.becomeMerchant.loading')} />
            ) : null}
            {loadingState === 'error' ? (
              <p className="text-sm text-destructive">{t('home.becomeMerchant.error')}</p>
            ) : null}
            {loadingState === 'forbidden' ? (
              <p className="text-sm text-destructive">{t('home.becomeMerchant.forbidden')}</p>
            ) : null}
            {loadingState === 'ready' && merchantEnabled ? (
              <div className="flex items-center justify-between gap-4">
                <p className="text-sm text-muted-foreground">{t('home.becomeMerchant.success')}</p>
                <Link to="/merchant">
                  <Button
                    onClick={() => {
                      void trackCta('become_merchant', 'become_merchant_go_portal')
                    }}
                  >
                    {t('home.becomeMerchant.goToMerchant')}
                  </Button>
                </Link>
              </div>
            ) : null}
            {loadingState === 'ready' && !merchantEnabled ? (
              <div className="grid gap-3 md:grid-cols-[1fr_1fr_auto]">
                <Input
                  value={merchantName}
                  onChange={(e) => setMerchantName(e.target.value)}
                  placeholder={t('home.becomeMerchant.namePlaceholder')}
                  disabled={creating}
                />
                <Input
                  value={merchantSlug}
                  onChange={(e) => setMerchantSlug(e.target.value)}
                  placeholder={t('home.becomeMerchant.slugPlaceholder')}
                  disabled={creating}
                />
                <Button
                  onClick={() => {
                    void trackCta('become_merchant', 'become_merchant_submit')
                    void onEnableMerchant()
                  }}
                  disabled={!canSubmit}
                >
                  {creating ? t('home.becomeMerchant.submitting') : t('home.becomeMerchant.cta')}
                </Button>
                {createError ? <p className="text-sm text-destructive md:col-span-3">{createError}</p> : null}
              </div>
            ) : null}
          </CardContent>
        </Card>
      </section>
      ) : null}
    </div>
  )
}
