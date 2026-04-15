import i18n from '@/i18n'

export type HomepageConfigSection = {
  id: string
  enabled: boolean
  order: number
  contentKey: string
}

export type HomepageConfigData = {
  tenantId: string
  locale: string
  pageVariant: string
  sections: HomepageConfigSection[]
  fallbackPolicy: { allowTenantDefault: boolean; crossTenantFallback: boolean }
  stateLegalityNotice: string
}

export type HomepageSeoData = {
  tenantId: string
  locale: string
  variant: string
  title: string
  description: string
  canonicalUrl: string
  robots: string
  structuredData: Record<string, unknown>
}

type ApiEnvelope<T> = { code: number; message: string; data: T }

async function parseJson<T>(response: Response): Promise<T> {
  const body = (await response.json()) as ApiEnvelope<T> & { data?: { errorCode?: string } }
  if (!response.ok || body.code !== 0) {
    const err = new Error(body.message || `HTTP ${response.status}`)
    ;(err as Error & { status?: number; errorCode?: string }).status = response.status
    ;(err as Error & { errorCode?: string }).errorCode =
      (body.data as { errorCode?: string } | undefined)?.errorCode
    throw err
  }
  return body.data as T
}

function cacheKey(kind: 'config' | 'seo', tenantId: string, locale: string, variant: string) {
  return `homepage-${kind}:${tenantId}:${locale}:${variant}`
}

export async function fetchHomepageConfig(params: {
  tenantId: string
  locale: string
  pageVariant?: string
}): Promise<HomepageConfigData> {
  const pageVariant = params.pageVariant ?? 'default'
  const sessionKey = cacheKey('config', params.tenantId, params.locale, pageVariant)
  const cached = sessionStorage.getItem(sessionKey)
  if (cached) {
    try {
      return JSON.parse(cached) as HomepageConfigData
    } catch {
      sessionStorage.removeItem(sessionKey)
    }
  }
  const qs = new URLSearchParams({
    tenantId: params.tenantId,
    locale: params.locale,
    pageVariant,
  })
  const response = await fetch(`/api/public/homepage-config?${qs.toString()}`)
  const data = await parseJson<HomepageConfigData>(response)
  sessionStorage.setItem(sessionKey, JSON.stringify(data))
  return data
}

export async function fetchHomepageSeo(params: {
  tenantId: string
  locale: string
  variant?: string
}): Promise<HomepageSeoData> {
  const variant = params.variant ?? 'default'
  const sessionKey = cacheKey('seo', params.tenantId, params.locale, variant)
  const cached = sessionStorage.getItem(sessionKey)
  if (cached) {
    try {
      return JSON.parse(cached) as HomepageSeoData
    } catch {
      sessionStorage.removeItem(sessionKey)
    }
  }
  const qs = new URLSearchParams({
    tenantId: params.tenantId,
    locale: params.locale,
    variant,
  })
  const response = await fetch(`/api/public/homepage-seo?${qs.toString()}`)
  const data = await parseJson<HomepageSeoData>(response)
  sessionStorage.setItem(sessionKey, JSON.stringify(data))
  return data
}

export type TrackingEventInput = {
  eventType: 'cta_click' | 'section_view'
  tenantId: string
  locale: string
  sectionId: string
  campaign: string
  pageVariant?: string
  metadata?: Record<string, unknown>
}

export async function postHomepageTrackingEvents(events: TrackingEventInput[]): Promise<{ accepted: number; rejected: number }> {
  const pageVariant = 'default'
  const body = {
    events: events.map((e) => ({
      eventType: e.eventType,
      tenantId: e.tenantId,
      locale: e.locale,
      sectionId: e.sectionId,
      campaign: e.campaign,
      pageVariant: e.pageVariant ?? pageVariant,
      occurredAt: new Date().toISOString(),
      metadata: e.metadata ?? {},
    })),
  }
  const response = await fetch('/api/public/tracking/events', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  return parseJson<{ accepted: number; rejected: number }>(response)
}

export function translateHomepageContent(contentKey: string, leafKey: string, fallback: string): string {
  const relative = contentKey.startsWith('homepage.') ? contentKey.slice('homepage.'.length) : contentKey
  const keyPath = `${relative}.${leafKey}`
  if (i18n.exists(keyPath, { ns: 'homepage' })) {
    return i18n.t(keyPath, { ns: 'homepage' })
  }
  return fallback
}

export function clearHomepageSessionCache(tenantId: string, locale: string) {
  const prefixes = [`homepage-config:${tenantId}:${locale}:`, `homepage-seo:${tenantId}:${locale}:`]
  for (let i = sessionStorage.length - 1; i >= 0; i--) {
    const k = sessionStorage.key(i)
    if (!k) continue
    if (prefixes.some((p) => k.startsWith(p))) sessionStorage.removeItem(k)
  }
}
