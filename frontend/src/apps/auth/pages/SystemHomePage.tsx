import { Fragment, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  CalendarClock,
  ChevronRight,
  Database,
  FileJson,
  GitBranch,
  GraduationCap,
  HelpCircle,
  Info,
  Map,
  MessageSquare,
  Settings2,
  Share2,
  UserPlus,
  Users,
  Wrench,
  Zap,
} from 'lucide-react'
import { LanguageSwitcher } from '@/shared/components/LanguageSwitcher'
import { Button } from '@/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Spinner } from '@/components/ui/spinner'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion'
import { cn } from '@/shared/lib/utils'
import { PUBLIC_HOMEPAGE_TENANT_KEY } from '@/shared/lib/constants'
import {
  clearHomepageSessionCache,
  fetchHomepageConfig,
  fetchHomepageSeo,
  postHomepageTrackingEvents,
  translateHomepageContent,
  type HomepageConfigData,
  type HomepageConfigSection,
  type HomepageSeoData,
} from '@/shared/lib/homepagePublicApi'

const CAMPAIGN = 'homepage_system_intro'

function useResolvedLocale(): string {
  const { i18n } = useTranslation()
  const lang = i18n.language || 'en-US'
  return lang === 'zh-TW' ? 'zh-TW' : 'en-US'
}

function SectionShell({
  id,
  trackId,
  children,
  className,
}: {
  id: string
  trackId?: string
  children: React.ReactNode
  className?: string
}) {
  return (
    <section
      id={id}
      data-homepage-section={trackId ?? id}
      className={className ?? 'scroll-mt-20'}
    >
      {children}
    </section>
  )
}

export function SystemHomePage() {
  const { t } = useTranslation(['homepage', 'common'])
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const locale = useResolvedLocale()
  const prevLocale = useRef(locale)
  const [navScrolled, setNavScrolled] = useState(false)

  const initialTenant = useMemo(() => {
    const q = searchParams.get('tenantId')?.trim()
    if (q) return q
    try {
      return localStorage.getItem(PUBLIC_HOMEPAGE_TENANT_KEY) || 'default'
    } catch {
      return 'default'
    }
  }, [searchParams])

  const [tenantId, setTenantId] = useState(initialTenant)
  const [config, setConfig] = useState<HomepageConfigData | null>(null)
  const [seo, setSeo] = useState<HomepageSeoData | null>(null)
  const [loadState, setLoadState] = useState<'loading' | 'ready' | 'error'>('loading')
  const [errorMessage, setErrorMessage] = useState('')
  const viewed = useRef(new Set<string>())

  useEffect(() => {
    viewed.current.clear()
  }, [tenantId, locale])

  useEffect(() => {
    const onScroll = () => setNavScrolled(window.scrollY > 24)
    onScroll()
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  const tx = useCallback(
    (contentKey: string, leaf: string, fallback: string) => translateHomepageContent(contentKey, leaf, fallback),
    [],
  )

  const load = useCallback(async () => {
    setLoadState('loading')
    setErrorMessage('')
    try {
      const [cfg, seoData] = await Promise.all([
        fetchHomepageConfig({ tenantId, locale, pageVariant: 'default' }),
        fetchHomepageSeo({ tenantId, locale, variant: 'default' }),
      ])
      setConfig(cfg)
      setSeo(seoData)
      document.title = seoData.title
      let meta = document.querySelector('meta[name="description"]') as HTMLMetaElement | null
      if (!meta) {
        meta = document.createElement('meta')
        meta.name = 'description'
        document.head.appendChild(meta)
      }
      meta.content = seoData.description
      setLoadState('ready')
    } catch (e) {
      setLoadState('error')
      setErrorMessage(e instanceof Error ? e.message : t('common:errors.generic'))
    }
  }, [locale, t, tenantId])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    if (prevLocale.current !== locale) {
      clearHomepageSessionCache(tenantId, prevLocale.current)
      prevLocale.current = locale
    }
  }, [locale, tenantId])

  useEffect(() => {
    const q = searchParams.get('tenantId')?.trim()
    if (q && q !== tenantId) {
      setTenantId(q)
    }
  }, [searchParams, tenantId])

  const reportSectionView = useCallback(
    async (sectionId: string) => {
      if (viewed.current.has(sectionId)) return
      viewed.current.add(sectionId)
      try {
        await postHomepageTrackingEvents([
          {
            eventType: 'section_view',
            tenantId,
            locale,
            sectionId,
            campaign: CAMPAIGN,
            pageVariant: 'default',
          },
        ])
      } catch {
        /* non-blocking */
      }
    },
    [locale, tenantId],
  )

  const onTenantChange = (value: string) => {
    try {
      localStorage.setItem(PUBLIC_HOMEPAGE_TENANT_KEY, value)
    } catch {
      /* ignore */
    }
    clearHomepageSessionCache(tenantId, locale)
    setTenantId(value)
    setSearchParams({ tenantId: value })
  }

  const onCta = async (sectionId: string, ctaId: string, href: string, sameWindow: boolean) => {
    try {
      await postHomepageTrackingEvents([
        {
          eventType: 'cta_click',
          tenantId,
          locale,
          sectionId,
          campaign: CAMPAIGN,
          metadata: { ctaId },
        },
      ])
    } catch {
      /* still navigate */
    }
    if (href.startsWith('#')) {
      window.location.hash = href.slice(1)
      return
    }
    if (sameWindow) {
      navigate(href)
    } else {
      window.location.href = href
    }
  }

  const sections = useMemo(() => {
    if (!config) return [] as HomepageConfigSection[]
    return config.sections.filter((s) => s.enabled).sort((a, b) => a.order - b.order)
  }, [config])

  useEffect(() => {
    if (loadState !== 'ready') return
    const nodes = document.querySelectorAll<HTMLElement>('[data-homepage-section]')
    if (!nodes.length) return
    const obs = new IntersectionObserver(
      (entries) => {
        for (const en of entries) {
          if (!en.isIntersecting) continue
          const sid = (en.target as HTMLElement).dataset.homepageSection
          if (sid) void reportSectionView(sid)
        }
      },
      { threshold: 0.22 },
    )
    nodes.forEach((n) => obs.observe(n))
    return () => obs.disconnect()
  }, [loadState, reportSectionView, config])

  useEffect(() => {
    if (!seo?.canonicalUrl) return
    let link = document.querySelector('link[data-homepage-canonical]') as HTMLLinkElement | null
    if (!link) {
      link = document.createElement('link')
      link.rel = 'canonical'
      link.setAttribute('data-homepage-canonical', '1')
      document.head.appendChild(link)
    }
    link.href = seo.canonicalUrl
  }, [seo])

  const renderNav = (s: HomepageConfigSection) => (
    <header
      key={s.id}
      data-homepage-section="nav"
      className={cn(
        'sticky top-0 z-40 transition-all duration-300',
        navScrolled
          ? 'border-b border-border/80 bg-background/95 shadow-sm backdrop-blur-md supports-[backdrop-filter]:bg-background/90'
          : 'border-b border-transparent bg-transparent',
      )}
    >
      <div className="container mx-auto grid grid-cols-[1fr_auto] items-center gap-3 px-4 py-3 md:grid-cols-[auto_1fr_auto]">
        <div className="flex min-w-0 items-center gap-2">
          <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary text-sm font-bold text-primary-foreground font-mono-technical">
            BC
          </div>
          <span className="truncate font-bold tracking-tight">{tx(s.contentKey, 'brand', 'Booking Core')}</span>
        </div>
        <nav className="col-span-2 hidden items-center justify-center gap-8 text-sm font-medium text-muted-foreground md:col-span-1 md:flex">
          <a className="transition-colors hover:text-foreground" href="#features">
            {tx(s.contentKey, 'linkFeatures', '')}
          </a>
          <a className="transition-colors hover:text-foreground" href="#process">
            {tx(s.contentKey, 'linkSolutions', '')}
          </a>
          <a className="transition-colors hover:text-foreground" href="#faq">
            {tx(s.contentKey, 'linkFaq', '')}
          </a>
          <a className="transition-colors hover:text-foreground" href="#footer">
            {tx(s.contentKey, 'linkContact', '')}
          </a>
        </nav>
        <div className="flex flex-wrap items-center justify-end gap-2 md:gap-2">
          <Select value={tenantId} onValueChange={onTenantChange}>
            <SelectTrigger
              className="h-9 w-[min(100%,11rem)] gap-2 border-dashed md:w-[12.5rem]"
              aria-label="Tenant"
            >
              <Database className="size-4 shrink-0 text-muted-foreground" aria-hidden />
              <span className="text-muted-foreground text-xs font-normal">
                {tx(s.contentKey, 'tenantLabel', 'Tenant')}:
              </span>
              <SelectValue placeholder="default" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="default">default</SelectItem>
              <SelectItem value="tnt_a">tnt_a</SelectItem>
              <SelectItem value="tnt_b">tnt_b</SelectItem>
            </SelectContent>
          </Select>
          <LanguageSwitcher variant="globe" />
          <Button variant="ghost" size="sm" asChild>
            <Link
              to="/login"
              onClick={(e) => {
                e.preventDefault()
                void onCta('nav', 'nav_login', '/login', true)
              }}
            >
              {tx(s.contentKey, 'ctaLogin', 'Login')}
            </Link>
          </Button>
          <Button size="sm" className="shadow-sm" asChild>
            <Link
              to="/register"
              onClick={(e) => {
                e.preventDefault()
                void onCta('nav', 'nav_register', '/register', true)
              }}
            >
              {tx(s.contentKey, 'ctaStart', 'Get started')}
            </Link>
          </Button>
        </div>
      </div>
    </header>
  )

  const renderHero = (s: HomepageConfigSection) => (
    <SectionShell
      key={s.id}
      id="hero"
      trackId="hero"
      className="scroll-mt-20 border-b border-border/60 bg-gradient-to-b from-primary/[0.06] to-background"
    >
      <div className="container mx-auto grid items-center gap-12 px-4 py-16 md:grid-cols-2 md:py-24">
        <div className="text-center md:text-left">
          <h1 className="text-balance text-3xl font-bold tracking-tight md:text-4xl lg:text-5xl">
            {tx(s.contentKey, 'title', '')}
          </h1>
          <p className="mt-4 text-lg text-muted-foreground md:max-w-xl">{tx(s.contentKey, 'subtitle', '')}</p>
          <div className="mt-8 flex flex-wrap justify-center gap-3 md:justify-start">
            <Button
              size="lg"
              className="shadow-md"
              asChild
            >
              <Link
                to="/register"
                onClick={(e) => {
                  e.preventDefault()
                  void onCta('hero', 'hero_primary_register', '/register', true)
                }}
              >
                {tx(s.contentKey, 'primaryCta', '')}
              </Link>
            </Button>
            <Button
              size="lg"
              variant="outline"
              type="button"
              className="border-primary/30 bg-background/80"
              onClick={() => void onCta('hero', 'hero_secondary_demo', '#process', true)}
            >
              {tx(s.contentKey, 'secondaryCta', '')}
            </Button>
          </div>
          <div className="mx-auto mt-6 flex max-w-xl items-start gap-2 text-left text-xs text-muted-foreground md:mx-0">
            <Info className="mt-0.5 size-4 shrink-0 text-primary" aria-hidden />
            <p>{tx(s.contentKey, 'statusNote', '')}</p>
          </div>
          {config?.stateLegalityNotice ? (
            <p className="mx-auto mt-3 max-w-xl text-xs text-muted-foreground/90 md:mx-0">
              {config.stateLegalityNotice}
            </p>
          ) : null}
        </div>
        <div className="mx-auto w-full max-w-md md:mx-0 md:max-w-none">
          <Card className="border-primary/20 shadow-lg shadow-primary/10">
            <CardHeader className="pb-2">
              <CardTitle className="font-mono-technical text-xs font-medium uppercase tracking-wider text-muted-foreground">
                {tx(s.contentKey, 'previewTitle', 'Resource / Slot — preview')}
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center justify-between text-xs font-mono-technical text-muted-foreground">
                <span>{tx(s.contentKey, 'mockTenantKeyLabel', 'tenant_id')}</span>
                <span className="text-foreground">{tenantId}</span>
              </div>
              <div className="grid grid-cols-3 gap-2 font-mono-technical text-xs">
                {['09:00', '10:00', '11:00'].map((slot) => (
                  <div
                    key={slot}
                    className="rounded-md border border-border bg-muted/50 px-2 py-2 text-center text-muted-foreground"
                  >
                    {slot}
                  </div>
                ))}
                {['13:00', '14:00', '15:00'].map((slot) => (
                  <div
                    key={slot}
                    className="rounded-md border border-primary/40 bg-primary/5 px-2 py-2 text-center font-medium text-foreground"
                  >
                    {slot}
                  </div>
                ))}
              </div>
              <p className="text-xs text-muted-foreground">{tx(s.contentKey, 'mockDisclaimer', '')}</p>
            </CardContent>
          </Card>
        </div>
      </div>
    </SectionShell>
  )

  const featureBlocks = [
    { key: 'c1' as const, Icon: Settings2 },
    { key: 'c2' as const, Icon: GitBranch },
    { key: 'c3' as const, Icon: CalendarClock },
    { key: 'c4' as const, Icon: Users },
  ]

  const renderFeatures = (s: HomepageConfigSection) => (
    <SectionShell key={s.id} id="features" trackId="features">
      <div className="container mx-auto px-4 py-16 md:py-20">
        <h2 className="text-center text-2xl font-semibold tracking-tight md:text-3xl">
          {tx(s.contentKey, 'title', '')}
        </h2>
        <p className="mx-auto mt-2 max-w-2xl text-center text-muted-foreground">{tx(s.contentKey, 'intro', '')}</p>
        <div className="mx-auto mt-12 grid max-w-5xl gap-6 sm:grid-cols-2">
          {featureBlocks.map(({ key, Icon }) => (
            <Card
              key={key}
              className="group border-t-4 border-t-primary shadow-sm transition-shadow duration-200 hover:shadow-lg"
            >
              <CardHeader className="flex flex-row items-start gap-4 space-y-0">
                <div className="rounded-lg bg-primary/10 p-2 text-primary">
                  <Icon className="size-6" aria-hidden />
                </div>
                <div>
                  <CardTitle className="text-lg">{tx(s.contentKey, `${key}t`, '')}</CardTitle>
                </div>
              </CardHeader>
              <CardContent className="text-sm text-muted-foreground">{tx(s.contentKey, `${key}b`, '')}</CardContent>
            </Card>
          ))}
        </div>
      </div>
    </SectionShell>
  )

  const processSteps = [
    { n: 1, Icon: UserPlus },
    { n: 2, Icon: FileJson },
    { n: 3, Icon: Share2 },
    { n: 4, Icon: Zap },
  ]

  const renderProcess = (s: HomepageConfigSection) => (
    <SectionShell key={s.id} id="process" trackId="process" className="border-y border-border/80 bg-muted/30">
      <div className="container mx-auto px-4 py-16 md:py-20">
        <h2 className="text-center text-2xl font-semibold md:text-3xl">{tx(s.contentKey, 'title', '')}</h2>
        <p className="mx-auto mt-2 max-w-2xl text-center text-sm text-muted-foreground">
          {tx(s.contentKey, 'notice', '')}
        </p>

        <div className="mx-auto mt-12 max-w-lg space-y-8 lg:hidden">
          {processSteps.map(({ n, Icon }) => (
            <div key={n} className="flex gap-4">
              <div className="flex size-11 shrink-0 items-center justify-center rounded-full bg-primary/15 text-primary">
                <Icon className="size-5" />
              </div>
              <div>
                <div className="font-semibold">{tx(s.contentKey, `s${n}t`, '')}</div>
                <p className="mt-1 text-sm text-muted-foreground">{tx(s.contentKey, `s${n}b`, '')}</p>
              </div>
            </div>
          ))}
        </div>

        <div className="mx-auto mt-12 hidden max-w-6xl items-stretch justify-center gap-2 lg:flex">
          {processSteps.map(({ n, Icon }, idx) => (
            <Fragment key={n}>
              <div className="flex min-w-0 flex-1 flex-col items-center rounded-xl border border-border bg-background px-4 py-6 text-center shadow-sm">
                <div className="mb-3 flex size-12 items-center justify-center rounded-full bg-primary/15 text-primary">
                  <Icon className="size-6" />
                </div>
                <div className="font-semibold">{tx(s.contentKey, `s${n}t`, '')}</div>
                <p className="mt-2 text-sm text-muted-foreground">{tx(s.contentKey, `s${n}b`, '')}</p>
              </div>
              {idx < processSteps.length - 1 ? (
                <div className="flex w-8 shrink-0 items-center justify-center text-muted-foreground">
                  <ChevronRight className="size-5" aria-hidden />
                </div>
              ) : null}
            </Fragment>
          ))}
        </div>

        <div className="mx-auto mt-12 max-w-3xl rounded-lg border border-amber-500/25 bg-amber-500/[0.06] px-4 py-4 text-sm text-foreground">
          <div className="flex gap-2">
            <Info className="mt-0.5 size-4 shrink-0 text-amber-700 dark:text-amber-400" aria-hidden />
            <p className="text-muted-foreground">{config?.stateLegalityNotice}</p>
          </div>
        </div>
      </div>
    </SectionShell>
  )

  const renderUseCases = (s: HomepageConfigSection) => {
    const tabs = [
      { value: 'u1', Icon: GraduationCap, tabKey: 'tab1' as const },
      { value: 'u2', Icon: Map, tabKey: 'tab2' as const },
      { value: 'u3', Icon: MessageSquare, tabKey: 'tab3' as const },
      { value: 'u4', Icon: Wrench, tabKey: 'tab4' as const },
    ] as const
    return (
      <SectionShell key={s.id} id="use_cases" trackId="use_cases">
        <div className="container mx-auto px-4 py-16 md:py-20">
          <h2 className="text-center text-2xl font-semibold md:text-3xl">{tx(s.contentKey, 'title', '')}</h2>
          <p className="mx-auto mt-2 max-w-2xl text-center text-muted-foreground">{tx(s.contentKey, 'intro', '')}</p>
          <Tabs defaultValue="u1" className="mx-auto mt-10 max-w-4xl">
            <TabsList className="grid h-auto w-full grid-cols-2 gap-1 p-1 md:grid-cols-4">
              {tabs.map(({ value, Icon, tabKey }) => (
                <TabsTrigger key={value} value={value} className="gap-2 py-2.5 text-xs sm:text-sm">
                  <Icon className="size-4 shrink-0" aria-hidden />
                  {tx(s.contentKey, tabKey, '')}
                </TabsTrigger>
              ))}
            </TabsList>
            {tabs.map(({ value }) => {
              const n = Number(value.replace('u', ''))
              return (
                <TabsContent key={value} value={value} className="mt-6 rounded-xl border bg-card p-6 shadow-sm">
                  <h3 className="text-lg font-semibold">{tx(s.contentKey, `u${n}t`, '')}</h3>
                  <p className="mt-2 text-sm text-muted-foreground">{tx(s.contentKey, `u${n}b`, '')}</p>
                </TabsContent>
              )
            })}
          </Tabs>
        </div>
      </SectionShell>
    )
  }

  const renderFaq = (s: HomepageConfigSection) => (
    <SectionShell key={s.id} id="faq" trackId="faq">
      <div className="container mx-auto max-w-2xl px-4 py-16 md:py-20">
        <h2 className="text-center text-2xl font-semibold md:text-3xl">{tx(s.contentKey, 'title', '')}</h2>
        <Accordion type="single" collapsible className="mt-10 w-full">
          {[1, 2, 3, 4].map((n) => (
            <AccordionItem key={n} value={`faq-${n}`}>
              <AccordionTrigger className="hover:no-underline">
                <span className="flex items-start gap-3 text-left">
                  <HelpCircle className="mt-0.5 size-4 shrink-0 text-primary" aria-hidden />
                  <span>{tx(s.contentKey, `q${n}`, '')}</span>
                </span>
              </AccordionTrigger>
              <AccordionContent className="pl-7 text-muted-foreground">{tx(s.contentKey, `a${n}`, '')}</AccordionContent>
            </AccordionItem>
          ))}
        </Accordion>
      </div>
    </SectionShell>
  )

  const renderCta = (s: HomepageConfigSection) => (
    <SectionShell key={s.id} id="cta" trackId="cta" className="border-t border-border/80">
      <div className="bg-foreground text-background">
        <div className="container mx-auto flex flex-col items-center justify-between gap-8 px-4 py-14 md:flex-row md:py-16">
          <div className="max-w-xl text-center md:text-left">
            <h2 className="text-2xl font-semibold tracking-tight text-background md:text-3xl">
              {tx(s.contentKey, 'title', '')}
            </h2>
            <p className="mt-2 text-sm text-background/80">{tx(s.contentKey, 'body', '')}</p>
          </div>
          <div className="flex w-full flex-col gap-3 sm:w-auto sm:flex-row sm:items-center">
            <Button
              size="lg"
              variant="outline"
              className="order-2 border-background/40 bg-transparent text-background hover:bg-background/10 hover:text-background sm:order-1"
              asChild
            >
              <Link
                to="/login"
                onClick={(e) => {
                  e.preventDefault()
                  void onCta('cta', 'cta_merchant_login', '/login', true)
                }}
              >
                {tx(s.contentKey, 'secondary', '')}
              </Link>
            </Button>
            <Button
              size="lg"
              className="order-1 bg-primary text-primary-foreground shadow-lg hover:bg-primary/90 sm:order-2"
              onClick={() => void onCta('cta', 'cta_client_app', '/client.html', false)}
            >
              {tx(s.contentKey, 'primary', '')}
            </Button>
          </div>
        </div>
      </div>
    </SectionShell>
  )

  const renderFooter = (s: HomepageConfigSection) => (
    <footer
      key={s.id}
      id="footer"
      data-homepage-section="footer"
      className="border-t border-border/80 bg-muted/20 py-12 text-sm text-muted-foreground"
    >
      <div className="container mx-auto px-4">
        <div className="grid grid-cols-2 gap-8 md:grid-cols-4">
          {([1, 2, 3, 4] as const).map((col) => (
            <div key={col}>
              <div className="font-semibold text-foreground">
                {tx(s.contentKey, `col${col}Title`, '')}
              </div>
              <ul className="mt-3 space-y-2">
                {(['a', 'b', 'c'] as const).map((row) => (
                  <li key={row}>
                    <span className="cursor-default hover:text-foreground">
                      {tx(s.contentKey, `col${col}${row}`, '')}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
        <div className="mt-10 flex flex-col items-center justify-between gap-4 border-t border-border/60 pt-8 text-xs md:flex-row">
          <span>
            &copy; {new Date().getFullYear()} {tx('homepage.sections.nav', 'brand', 'Booking Core')}
          </span>
          <span>{tx(s.contentKey, 'rights', '')}</span>
        </div>
      </div>
    </footer>
  )

  const renderSection = (sec: HomepageConfigSection) => {
    switch (sec.id) {
      case 'nav':
        return renderNav(sec)
      case 'hero':
        return renderHero(sec)
      case 'features':
        return renderFeatures(sec)
      case 'process':
        return renderProcess(sec)
      case 'use_cases':
        return renderUseCases(sec)
      case 'faq':
        return renderFaq(sec)
      case 'cta':
        return renderCta(sec)
      case 'footer':
        return renderFooter(sec)
      default:
        return null
    }
  }

  if (loadState === 'loading') {
    return (
      <div className="flex flex-1 items-center justify-center py-24">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }

  if (loadState === 'error') {
    return (
      <div className="container mx-auto max-w-lg px-4 py-16">
        <div className="space-y-3 rounded-lg border border-destructive/50 bg-destructive/5 p-4">
          <h2 className="font-semibold text-destructive">{t('common:errors.generic')}</h2>
          <p className="text-sm text-muted-foreground">{errorMessage}</p>
          <Button variant="outline" size="sm" onClick={() => void load()}>
            Retry
          </Button>
        </div>
      </div>
    )
  }

  return <div className="flex min-h-0 flex-1 flex-col">{sections.map((sec) => renderSection(sec))}</div>
}
