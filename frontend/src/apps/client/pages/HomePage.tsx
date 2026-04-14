import { Link } from 'react-router-dom'
import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Search, ArrowRight } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { enableMerchant, fetchAuthMe, isApiError } from '@/shared/lib/authContextApi'
import { isCanonicalMerchantRole } from '@/shared/lib/roleCompat'
import {
  fetchVisibleMerchants,
  isClientCatalogApiError,
  type ClientMerchantCardDto,
} from '@/shared/lib/clientCatalogApi'

export function HomePage() {
  const { t } = useTranslation(['client', 'common'])
  const [loadingState, setLoadingState] = useState<'loading' | 'ready' | 'error' | 'forbidden'>('loading')
  const [merchantEnabled, setMerchantEnabled] = useState(false)
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState('')
  const [merchantName, setMerchantName] = useState('')
  const [merchantSlug, setMerchantSlug] = useState('')
  const [merchants, setMerchants] = useState<ClientMerchantCardDto[]>([])
  const [catalogError, setCatalogError] = useState('')

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
    const loadMerchants = async () => {
      setCatalogError('')
      try {
        const data = await fetchVisibleMerchants()
        if (!mounted) return
        setMerchants(data)
      } catch (err) {
        if (!mounted) return
        setCatalogError(
          isClientCatalogApiError(err) ? err.message : t('common:errors.generic'),
        )
      }
    }
    void loadMerchants()
    return () => {
      mounted = false
    }
  }, [t])

  const categories = useMemo(() => {
    const grouped = merchants.reduce(
      (acc, merchant) => {
        const key = merchant.visibility.toLowerCase()
        acc[key] = (acc[key] ?? 0) + 1
        return acc
      },
      {} as Record<string, number>,
    )
    return [
      { key: 'public', count: grouped.public ?? 0 },
      { key: 'private', count: grouped.private ?? 0 },
      { key: 'unlisted', count: grouped.unlisted ?? 0 },
    ]
  }, [merchants])

  const featuredMerchants = useMemo(() => merchants.slice(0, 4), [merchants])

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

  return (
    <div className="container mx-auto px-4 py-8">
      {/* Hero Section */}
      <section className="mb-12 text-center">
        <h1 className="text-4xl font-bold tracking-tight mb-4 text-balance">
          {t('home.hero.title')}
        </h1>
        <p className="text-lg text-muted-foreground mb-8 max-w-2xl mx-auto">
          {t('home.hero.subtitle')}
        </p>
        <Link to="/search">
          <Button size="lg" className="gap-2">
            <Search className="h-5 w-5" />
            {t('home.hero.cta')}
          </Button>
        </Link>
      </section>

      {/* Categories */}
      <section className="mb-12">
        <h2 className="text-2xl font-semibold mb-6">{t('home.categories.title')}</h2>
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
          {categories.map((category) => (
            <Link key={category.key} to={`/search?category=${category.key}`}>
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

      {/* Featured Services */}
      <section>
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-2xl font-semibold">{t('home.featured.title')}</h2>
          <Link to="/search">
            <Button variant="ghost" className="gap-2">
              {t('home.featured.viewAll')}
              <ArrowRight className="h-4 w-4" />
            </Button>
          </Link>
        </div>
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
          {featuredMerchants.map((merchant) => (
            <Link key={merchant.merchantId} to={`/search?q=${encodeURIComponent(merchant.name)}`}>
              <Card className="hover:shadow-md transition-shadow cursor-pointer h-full">
                <CardHeader className="p-0">
                  <div className="aspect-video bg-muted rounded-t-lg flex items-center justify-center">
                    <span className="text-muted-foreground text-sm">{merchant.slug}</span>
                  </div>
                </CardHeader>
                <CardContent className="p-4">
                  <Badge variant="secondary" className="mb-2">
                    {t(`home.dynamic.categories.${merchant.visibility.toLowerCase()}`)}
                  </Badge>
                  <CardTitle className="text-lg mb-2">{merchant.name}</CardTitle>
                  <div className="text-sm text-muted-foreground">
                    {t(`home.dynamic.joinState.${merchant.joinState.toLowerCase()}`, {
                      defaultValue: merchant.joinState,
                    })}
                  </div>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
        {catalogError ? <p className="mt-4 text-sm text-destructive">{catalogError}</p> : null}
      </section>

      <section className="mt-12">
        <Card>
          <CardHeader>
            <CardTitle>{t('home.becomeMerchant.title')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {loadingState === 'loading' ? (
              <p className="text-sm text-muted-foreground">{t('home.becomeMerchant.loading')}</p>
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
                  <Button>{t('home.becomeMerchant.goToMerchant')}</Button>
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
                <Button onClick={onEnableMerchant} disabled={!canSubmit}>
                  {creating ? t('home.becomeMerchant.submitting') : t('home.becomeMerchant.cta')}
                </Button>
                {createError ? <p className="text-sm text-destructive md:col-span-3">{createError}</p> : null}
              </div>
            ) : null}
          </CardContent>
        </Card>
      </section>
    </div>
  )
}
