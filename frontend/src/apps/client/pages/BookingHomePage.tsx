"use client"

import React, { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  motion,
  useInView,
  useMotionValueEvent,
  useSpring,
} from 'framer-motion'
import {
  ArrowRight,
  Award,
  Calendar,
  Check,
  ChevronRight,
  Clock,
  Heart,
  MapPin,
  Search,
  Shield,
  Sparkles,
  Star,
  TrendingUp,
  Users,
  Zap,
} from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import {
  fetchClientCategories,
  fetchFeaturedResources,
  isClientCatalogApiError,
  type ClientCatalogResourceDto,
  type ClientCategoryDto,
} from '@/shared/lib/clientCatalogApi'
import { enableMerchant, fetchAuthMe, isApiError } from '@/shared/lib/authContextApi'
import { isCanonicalMerchantRole } from '@/shared/lib/roleCompat'
import {
  fetchHomepageConfig,
  fetchHomepageSeo,
  postHomepageTrackingEvents,
  type HomepageConfigData,
} from '@/shared/lib/homepagePublicApi'
import { useAuth } from '@/shared/hooks/useAuth'

function cn(...classes: (string | undefined | null | boolean)[]): string {
  return classes.filter(Boolean).join(' ')
}

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

const HeroSection: React.FC<{ onSearch: (query: string, location: string) => void; onTrackCta: (sectionId: string, ctaId: string, metadata?: Record<string, unknown>) => void }> = ({
  onSearch,
  onTrackCta,
}) => {
  const { t } = useTranslation(['client', 'common'])
  const [searchQuery, setSearchQuery] = useState('')
  const [location, setLocation] = useState('')

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    onTrackCta('hero', 'hero_search', { hasQuery: searchQuery.trim().length > 0, hasLocation: location.trim().length > 0 })
    onSearch(searchQuery, location)
  }

  return (
    <div className="relative overflow-hidden bg-gradient-to-b from-background via-background to-muted/20">
      <div className="container mx-auto px-4 pb-20 pt-10 sm:px-6 lg:px-8">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="mx-auto max-w-4xl text-center"
        >
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.5, delay: 0.1 }}
            className="mb-6 inline-flex items-center gap-2 rounded-full bg-primary/10 px-4 py-2 text-sm font-medium text-primary"
          >
            <Sparkles className="h-4 w-4" />
            <span>{t('home.hero.subtitle', { ns: 'client' })}</span>
          </motion.div>

          <motion.h1
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.2 }}
            className="mb-6 text-4xl font-bold leading-tight text-foreground sm:text-5xl lg:text-6xl"
          >
            {t('home.hero.title', { ns: 'client' })}
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.3 }}
            className="mx-auto mb-10 max-w-2xl text-lg text-muted-foreground"
          >
            {t('home.hero.subtitle', { ns: 'client' })}
          </motion.p>

          <motion.form
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.4 }}
            onSubmit={handleSearch}
            className="mx-auto max-w-3xl rounded-2xl border border-border bg-card p-2 shadow-lg"
          >
            <div className="flex flex-col gap-2 sm:flex-row">
              <div className="flex flex-1 items-center gap-2 rounded-xl bg-background px-4 py-2">
                <Search className="h-5 w-5 text-muted-foreground" />
                <Input
                  type="text"
                  placeholder={t('search.placeholder', { ns: 'client' })}
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="border-0 bg-transparent focus-visible:ring-0 focus-visible:ring-offset-0"
                />
              </div>
              <div className="flex flex-1 items-center gap-2 rounded-xl bg-background px-4 py-2">
                <MapPin className="h-5 w-5 text-muted-foreground" />
                <Input
                  type="text"
                  placeholder={t('search.filters.location', { ns: 'client' })}
                  value={location}
                  onChange={(e) => setLocation(e.target.value)}
                  className="border-0 bg-transparent focus-visible:ring-0 focus-visible:ring-offset-0"
                />
              </div>
              <Button type="submit" size="lg" className="sm:w-auto">
                {t('common:actions.search')}
              </Button>
            </div>
          </motion.form>
        </motion.div>
      </div>
    </div>
  )
}

const DiscoveryChips: React.FC<{ onTrackCta: (sectionId: string, ctaId: string, metadata?: Record<string, unknown>) => void }> = ({ onTrackCta }) => {
  const chips = [
    { icon: <Sparkles className="h-4 w-4" />, label: 'Popular', color: 'bg-primary/10 text-primary border-primary/20' },
    {
      icon: <TrendingUp className="h-4 w-4" />,
      label: 'Trending',
      color: 'bg-orange-500/10 text-orange-600 border-orange-500/20',
    },
    { icon: <Star className="h-4 w-4" />, label: 'Top Rated', color: 'bg-yellow-500/10 text-yellow-600 border-yellow-500/20' },
    { icon: <Clock className="h-4 w-4" />, label: 'Last Minute', color: 'bg-green-500/10 text-green-600 border-green-500/20' },
    { icon: <Heart className="h-4 w-4" />, label: 'Favorites', color: 'bg-pink-500/10 text-pink-600 border-pink-500/20' },
    { icon: <Award className="h-4 w-4" />, label: 'Premium', color: 'bg-purple-500/10 text-purple-600 border-purple-500/20' },
  ]

  return (
    <section className="bg-background py-8">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="scrollbar-hide flex items-center gap-3 overflow-x-auto pb-2">
          {chips.map((chip, index) => (
            <motion.button
              key={chip.label}
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ duration: 0.3, delay: index * 0.05 }}
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => onTrackCta('discovery', 'discovery_chip_click', { chip: chip.label.toLowerCase() })}
              className={cn(
                'whitespace-nowrap rounded-full border px-4 py-2 text-sm font-medium transition-all',
                'flex items-center gap-2',
                chip.color,
              )}
            >
              {chip.icon}
              <span>{chip.label}</span>
            </motion.button>
          ))}
        </div>
      </div>
    </section>
  )
}

const FeaturedGrid: React.FC<{ featured: ClientCatalogResourceDto[]; isLoading: boolean; error: string | null; onTrackCta: (sectionId: string, ctaId: string, metadata?: Record<string, unknown>) => void }> = ({
  featured,
  isLoading,
  error,
  onTrackCta,
}) => {
  const { t } = useTranslation(['client', 'common'])

  return (
    <section id="explore" className="bg-background py-16">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="mb-12 text-center"
        >
          <h2 className="mb-4 text-3xl font-bold text-foreground sm:text-4xl">{t('home.featured.title', { ns: 'client' })}</h2>
          <p className="mx-auto max-w-2xl text-lg text-muted-foreground">
            Handpicked activities and experiences curated just for you
          </p>
        </motion.div>

        {isLoading ? (
          <p className="text-center text-sm text-muted-foreground">{t('common:status.loading')}</p>
        ) : null}
        {error ? <p className="text-center text-sm text-destructive">{error}</p> : null}
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {featured.map((item, index) => (
            <motion.div
              key={item.id}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.5, delay: index * 0.1 }}
            >
              <Link
                to={`/booking/${item.id}`}
                onClick={() => onTrackCta('featured', 'featured_open_resource', { resourceId: item.id, category: item.category })}
              >
                <Card className="group cursor-pointer overflow-hidden border-border transition-all duration-300 hover:shadow-xl">
                <div className="relative aspect-[4/3] overflow-hidden">
                  <img
                    src={item.imageUrl ?? 'https://images.unsplash.com/photo-1540555700478-4be289fbecef?w=800&q=80'}
                    alt={item.name}
                    className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-110"
                  />
                  {item.availabilityLabel ? (
                    <Badge className="absolute left-3 top-3 bg-primary text-primary-foreground">{item.availabilityLabel}</Badge>
                  ) : null}
                  <button className="absolute right-3 top-3 flex h-10 w-10 items-center justify-center rounded-full bg-background/80 backdrop-blur-sm transition-colors hover:bg-background">
                    <Heart className="h-5 w-5 text-foreground" />
                  </button>
                </div>
                <div className="p-5">
                  <div className="mb-2 flex items-center justify-between">
                    <span className="text-xs font-medium text-primary">{item.category || 'General'}</span>
                    <div className="flex items-center gap-1">
                      <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" />
                      <span className="text-sm font-medium">{item.rating.toFixed(1)}</span>
                    </div>
                  </div>
                  <h3 className="mb-2 text-lg font-semibold text-foreground transition-colors group-hover:text-primary">
                    {item.name}
                  </h3>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-1 text-sm text-muted-foreground">
                      <Clock className="h-4 w-4" />
                      <span>{Math.max(1, Math.round(item.durationMinutes / 60))} hours</span>
                    </div>
                    <div className="text-lg font-bold text-foreground">${item.price}</div>
                  </div>
                </div>
                </Card>
              </Link>
            </motion.div>
          ))}
        </div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6, delay: 0.3 }}
          className="mt-12 text-center"
        >
          <Link to="/search" onClick={() => onTrackCta('featured', 'featured_view_all')}>
            <Button size="lg" variant="outline">
              {t('home.featured.viewAll', { ns: 'client' })}
              <ChevronRight className="ml-2 h-4 w-4" />
            </Button>
          </Link>
        </motion.div>
      </div>
    </section>
  )
}

const CATEGORY_COLORS = [
  'bg-purple-500/10 text-purple-600',
  'bg-orange-500/10 text-orange-600',
  'bg-yellow-500/10 text-yellow-600',
  'bg-blue-500/10 text-blue-600',
  'bg-green-500/10 text-green-600',
  'bg-red-500/10 text-red-600',
]
const CATEGORY_ICONS = [
  <Sparkles className="h-6 w-6" />,
  <TrendingUp className="h-6 w-6" />,
  <Star className="h-6 w-6" />,
  <Users className="h-6 w-6" />,
  <Award className="h-6 w-6" />,
  <Zap className="h-6 w-6" />,
]

const CategoriesSection: React.FC<{ categories: ClientCategoryDto[]; isLoading: boolean; error: string | null; onTrackCta: (sectionId: string, ctaId: string, metadata?: Record<string, unknown>) => void }> = ({
  categories,
  isLoading,
  error,
  onTrackCta,
}) => {
  const { t } = useTranslation(['client', 'common'])

  return (
    <section id="categories" className="bg-muted/30 py-16">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="mb-12 text-center"
        >
          <h2 className="mb-4 text-3xl font-bold text-foreground sm:text-4xl">{t('home.categories.title', { ns: 'client' })}</h2>
          <p className="mx-auto max-w-2xl text-lg text-muted-foreground">Explore experiences across different categories</p>
        </motion.div>

        {isLoading ? <p className="text-center text-sm text-muted-foreground">{t('common:status.loading')}</p> : null}
        {error ? <p className="text-center text-sm text-destructive">{error}</p> : null}
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
          {categories.map((category, index) => (
            <Link
              key={category.key}
              to={`/search?category=${encodeURIComponent(category.key)}`}
              onClick={() => onTrackCta('categories', 'category_select', { categoryKey: category.key })}
            >
              <motion.button
              initial={{ opacity: 0, scale: 0.9 }}
              whileInView={{ opacity: 1, scale: 1 }}
              viewport={{ once: true }}
              transition={{ duration: 0.4, delay: index * 0.05 }}
              whileHover={{ scale: 1.05, y: -5 }}
              whileTap={{ scale: 0.95 }}
              className={cn(
                'flex flex-col items-center gap-3 rounded-2xl border border-border bg-card p-6 transition-all hover:shadow-lg',
                CATEGORY_COLORS[index % CATEGORY_COLORS.length],
              )}
            >
              <div className="flex h-12 w-12 items-center justify-center rounded-full bg-background">
                {CATEGORY_ICONS[index % CATEGORY_ICONS.length]}
              </div>
              <div className="text-center">
                <h3 className="mb-1 text-sm font-semibold text-foreground">{category.label}</h3>
                <p className="text-xs text-muted-foreground">{category.count} experiences</p>
              </div>
              </motion.button>
            </Link>
          ))}
        </div>
      </div>
    </section>
  )
}

const BecomeMerchantCTA: React.FC<{
  loadingState: 'loading' | 'ready' | 'error' | 'forbidden'
  merchantEnabled: boolean
  creating: boolean
  createError: string
  merchantName: string
  merchantSlug: string
  setMerchantName: (value: string) => void
  setMerchantSlug: (value: string) => void
  onEnableMerchant: () => void
  onTrackCta: (sectionId: string, ctaId: string, metadata?: Record<string, unknown>) => void
}> = ({
  loadingState,
  merchantEnabled,
  creating,
  createError,
  merchantName,
  merchantSlug,
  setMerchantName,
  setMerchantSlug,
  onEnableMerchant,
  onTrackCta,
}) => {
  const { t } = useTranslation(['client', 'common'])
  const features = ['Easy Integration', 'Real-time Bookings', 'Customer Management', 'Analytics Dashboard', 'Secure Payments']

  return (
    <section id="merchant" className="bg-gradient-to-br from-primary/5 via-background to-primary/10 py-20">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-5xl">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6 }}
            className="rounded-3xl border border-border bg-card p-8 shadow-xl md:p-12"
          >
            <div className="flex flex-col items-start justify-between gap-8 md:flex-row">
              <div className="flex-1">
                <motion.div
                  initial={{ opacity: 0, x: -20 }}
                  whileInView={{ opacity: 1, x: 0 }}
                  viewport={{ once: true }}
                  transition={{ duration: 0.6, delay: 0.1 }}
                >
                  <Badge className="mb-4 border-primary/20 bg-primary/10 text-primary">For Business</Badge>
                  <h2 className="mb-4 text-3xl font-bold text-foreground md:text-4xl">{t('home.becomeMerchant.title', { ns: 'client' })}</h2>
                  <p className="mb-6 text-lg text-muted-foreground">
                    Join thousands of businesses offering amazing experiences. Grow your bookings and reach new customers
                    with our platform.
                  </p>
                  {loadingState === 'loading' ? <p className="text-sm text-muted-foreground">{t('home.becomeMerchant.loading', { ns: 'client' })}</p> : null}
                  {loadingState === 'forbidden' ? (
                    <p className="text-sm text-destructive">{t('home.becomeMerchant.forbidden', { ns: 'client' })}</p>
                  ) : null}
                  {loadingState === 'error' ? <p className="text-sm text-destructive">{t('home.becomeMerchant.error', { ns: 'client' })}</p> : null}
                  {loadingState === 'ready' && merchantEnabled ? (
                    <Link to="/merchant" onClick={() => onTrackCta('become_merchant', 'become_merchant_go_portal')}>
                      <Button size="lg" className="group">
                        {t('home.becomeMerchant.goToMerchant', { ns: 'client' })}
                        <ArrowRight className="ml-2 h-4 w-4 transition-transform group-hover:translate-x-1" />
                      </Button>
                    </Link>
                  ) : null}
                  {loadingState === 'ready' && !merchantEnabled ? (
                    <div className="space-y-3">
                      <Input
                        value={merchantName}
                        onChange={(e) => setMerchantName(e.target.value)}
                        placeholder={t('home.becomeMerchant.namePlaceholder', { ns: 'client' })}
                        disabled={creating}
                      />
                      <Input
                        value={merchantSlug}
                        onChange={(e) => setMerchantSlug(e.target.value)}
                        placeholder={t('home.becomeMerchant.slugPlaceholder', { ns: 'client' })}
                        disabled={creating}
                      />
                      <Button
                        size="lg"
                        className="group"
                        onClick={() => {
                          onTrackCta('become_merchant', 'become_merchant_submit')
                          onEnableMerchant()
                        }}
                        disabled={creating || merchantName.trim().length === 0 || merchantSlug.trim().length === 0}
                      >
                        {creating ? t('home.becomeMerchant.submitting', { ns: 'client' }) : t('home.becomeMerchant.cta', { ns: 'client' })}
                        <ArrowRight className="ml-2 h-4 w-4 transition-transform group-hover:translate-x-1" />
                      </Button>
                      {createError ? <p className="text-sm text-destructive">{createError}</p> : null}
                    </div>
                  ) : null}
                </motion.div>
              </div>

              <motion.div
                initial={{ opacity: 0, x: 20 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.6, delay: 0.2 }}
                className="md:w-1/3"
              >
                <ul className="flex flex-col space-y-3">
                  {features.map((feature, idx) => (
                    <motion.li
                      key={feature}
                      initial={{ opacity: 0, x: 20 }}
                      whileInView={{ opacity: 1, x: 0 }}
                      viewport={{ once: true }}
                      transition={{ duration: 0.4, delay: 0.3 + idx * 0.1 }}
                      className="flex items-center text-sm font-medium text-foreground"
                    >
                      <Check className="mr-3 h-5 w-5 shrink-0 text-primary" />
                      {feature}
                    </motion.li>
                  ))}
                </ul>
              </motion.div>
            </div>
          </motion.div>
        </div>
      </div>
    </section>
  )
}

const StatsSection: React.FC = () => {
  const statsRef = useRef<HTMLDivElement>(null)
  const isInView = useInView(statsRef, { once: false, amount: 0.3 })

  const stats = [
    { icon: <Award />, value: 10000, label: 'Experiences', suffix: '+' },
    { icon: <Users />, value: 50000, label: 'Happy Customers', suffix: '+' },
    { icon: <Star />, value: 4.9, label: 'Average Rating', suffix: '' },
    { icon: <Shield />, value: 100, label: 'Secure Bookings', suffix: '%' },
  ]

  return (
    <section ref={statsRef} className="bg-background py-16">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 gap-8 lg:grid-cols-4">
          {stats.map((stat, index) => (
            <StatCounter
              key={stat.label}
              icon={stat.icon}
              value={stat.value}
              label={stat.label}
              suffix={stat.suffix}
              delay={index * 0.1}
              isInView={isInView}
            />
          ))}
        </div>
      </div>
    </section>
  )
}

interface StatCounterProps {
  icon: React.ReactNode
  value: number
  label: string
  suffix: string
  delay: number
  isInView: boolean
}

const StatCounter: React.FC<StatCounterProps> = ({ icon, value, label, suffix, delay, isInView }) => {
  const [hasAnimated, setHasAnimated] = useState(false)
  const [displayText, setDisplayText] = useState(value < 10 ? '0.0' : '0')

  const springValue = useSpring(0, {
    stiffness: 50,
    damping: 10,
  })

  useEffect(() => {
    if (isInView && !hasAnimated) {
      springValue.set(value)
      setHasAnimated(true)
    } else if (!isInView && hasAnimated) {
      springValue.set(0)
      setHasAnimated(false)
    }
  }, [isInView, value, springValue, hasAnimated])

  useMotionValueEvent(springValue, 'change', (latest) => {
    setDisplayText(value < 10 ? latest.toFixed(1) : Math.floor(latest).toString())
  })

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      transition={{ duration: 0.6, delay }}
      className="text-center"
    >
      <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-primary/10 text-primary">{icon}</div>
      <motion.div className="flex items-center justify-center text-3xl font-bold text-foreground">
        <span>{displayText}</span>
        <span>{suffix}</span>
      </motion.div>
      <p className="mt-1 text-sm text-muted-foreground">{label}</p>
    </motion.div>
  )
}

const Footer: React.FC = () => {
  return (
    <footer className="border-t border-border bg-muted/30 py-12">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="mb-8 grid grid-cols-1 gap-8 md:grid-cols-4">
          <div>
            <div className="mb-4 flex items-center gap-2">
              <Calendar className="h-6 w-6 text-primary" />
              <span className="text-xl font-bold text-foreground">BookNow</span>
            </div>
            <p className="text-sm text-muted-foreground">Discover and book amazing experiences around the world.</p>
          </div>
          <div>
            <h3 className="mb-4 font-semibold text-foreground">Explore</h3>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li><a href="#" className="transition-colors hover:text-foreground">All Experiences</a></li>
              <li><a href="#" className="transition-colors hover:text-foreground">Categories</a></li>
              <li><a href="#" className="transition-colors hover:text-foreground">Popular</a></li>
            </ul>
          </div>
          <div>
            <h3 className="mb-4 font-semibold text-foreground">Company</h3>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li><a href="#" className="transition-colors hover:text-foreground">About Us</a></li>
              <li><a href="#" className="transition-colors hover:text-foreground">Careers</a></li>
              <li><a href="#" className="transition-colors hover:text-foreground">Contact</a></li>
            </ul>
          </div>
          <div>
            <h3 className="mb-4 font-semibold text-foreground">Support</h3>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li><a href="#" className="transition-colors hover:text-foreground">Help Center</a></li>
              <li><a href="#" className="transition-colors hover:text-foreground">Terms of Service</a></li>
              <li><a href="#" className="transition-colors hover:text-foreground">Privacy Policy</a></li>
            </ul>
          </div>
        </div>
        <div className="border-t border-border pt-8 text-center text-sm text-muted-foreground">
          <p>&copy; 2024 BookNow. All rights reserved.</p>
        </div>
      </div>
    </footer>
  )
}

export function BookingHomePage() {
  const { i18n } = useTranslation(['client', 'common'])
  const { user } = useAuth()
  const navigate = useNavigate()
  const locale = i18n.language === 'zh-TW' ? 'zh-TW' : 'en-US'
  const tenantId = user?.tenantId ?? 'default'
  const [featured, setFeatured] = useState<ClientCatalogResourceDto[]>([])
  const [categories, setCategories] = useState<ClientCategoryDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [homepageConfig, setHomepageConfig] = useState<HomepageConfigData | null>(null)
  const [loadingState, setLoadingState] = useState<'loading' | 'ready' | 'error' | 'forbidden'>('loading')
  const [merchantEnabled, setMerchantEnabled] = useState(false)
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState('')
  const [merchantName, setMerchantName] = useState('')
  const [merchantSlug, setMerchantSlug] = useState('')
  const viewedSections = useRef(new Set<string>())

  useEffect(() => {
    let mounted = true
    const load = async () => {
      setLoading(true)
      setError(null)
      try {
        const [featuredList, categoryList, config, seo] = await Promise.all([
          fetchFeaturedResources(6),
          fetchClientCategories(),
          fetchHomepageConfig({ tenantId, locale, pageVariant: 'default' }),
          fetchHomepageSeo({ tenantId, locale, variant: 'default' }),
        ])
        if (!mounted) return
        setFeatured(featuredList)
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
        setError(isClientCatalogApiError(err) ? err.message : 'Failed to load homepage content')
      } finally {
        if (mounted) setLoading(false)
      }
    }
    void load()
    return () => {
      mounted = false
    }
  }, [locale, tenantId])

  useEffect(() => {
    let mounted = true
    const loadMe = async () => {
      try {
        setLoadingState('loading')
        const me = await fetchAuthMe()
        if (!mounted) return
        const hasMerchantContext = (me.availableContexts ?? []).some((ctx) =>
          isCanonicalMerchantRole(ctx.canonicalRole ?? ctx.role),
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
      void trackEvent('cta_click', sectionId, { ctaId, surface: 'client_home', ...(metadata ?? {}) }),
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
  }, [trackEvent, homepageConfig, featured.length, categories.length, loadingState])

  const handleSearch = (query: string, location: string) => {
    const params = new URLSearchParams()
    const normalizedQuery = query.trim()
    const normalizedLocation = location.trim()
    if (normalizedQuery) params.set('q', normalizedQuery)
    if (normalizedLocation) params.set('location', normalizedLocation)
    const search = params.toString()
    navigate(search ? `/search?${search}` : '/search')
  }

  const handleEnableMerchant = async () => {
    if (merchantName.trim().length === 0 || merchantSlug.trim().length === 0 || creating) return
    setCreating(true)
    setCreateError('')
    try {
      await enableMerchant({ name: merchantName.trim(), slug: merchantSlug.trim() })
      setMerchantEnabled(true)
    } catch (err) {
      if (isApiError(err) && err.status === 403) {
        setCreateError('You do not have permission to enable merchant mode.')
      } else {
        setCreateError('Unable to enable merchant mode. Please try again.')
      }
    } finally {
      setCreating(false)
    }
  }

  return (
    <div className="min-h-screen bg-background text-foreground">
      {sectionVisible(CLIENT_SECTION_MAPPING.hero) ? (
        <section data-client-home-section="hero">
          <HeroSection onSearch={handleSearch} onTrackCta={trackCta} />
        </section>
      ) : null}
      {sectionVisible(CLIENT_SECTION_MAPPING.discovery) ? (
        <section data-client-home-section="discovery">
          <DiscoveryChips onTrackCta={trackCta} />
        </section>
      ) : null}
      {sectionVisible(CLIENT_SECTION_MAPPING.featured) ? (
        <section data-client-home-section="featured">
          <FeaturedGrid featured={featured} isLoading={loading} error={error} onTrackCta={trackCta} />
        </section>
      ) : null}
      {sectionVisible(CLIENT_SECTION_MAPPING.categories) ? (
        <section data-client-home-section="categories">
          <CategoriesSection categories={categories} isLoading={loading} error={error} onTrackCta={trackCta} />
        </section>
      ) : null}
      <StatsSection />
      {sectionVisible(CLIENT_SECTION_MAPPING.become_merchant) ? (
        <section data-client-home-section="become_merchant">
          <BecomeMerchantCTA
            loadingState={loadingState}
            merchantEnabled={merchantEnabled}
            creating={creating}
            createError={createError}
            merchantName={merchantName}
            merchantSlug={merchantSlug}
            setMerchantName={setMerchantName}
            setMerchantSlug={setMerchantSlug}
            onEnableMerchant={handleEnableMerchant}
            onTrackCta={trackCta}
          />
        </section>
      ) : null}
      <Footer />
    </div>
  )
}
