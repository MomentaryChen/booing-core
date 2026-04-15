import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { ToastAction } from '@/components/ui/toast'
import { useToast } from '@/components/ui/use-toast'
import { enableMerchant, isApiError } from '@/shared/lib/authContextApi'

interface BootstrapGatePageProps {
  onEnabled: () => void
}

export function BootstrapGatePage({ onEnabled }: BootstrapGatePageProps) {
  const { t } = useTranslation('merchant')
  const { toast } = useToast()
  const [name, setName] = useState('')
  const [slug, setSlug] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const canSubmit = name.trim().length > 0 && slug.trim().length > 0 && !submitting

  const handleEnable = async () => {
    if (!canSubmit) return
    setSubmitting(true)
    setError('')
    try {
      await enableMerchant({ name: name.trim(), slug: slug.trim() })
      toast({ title: t('common:status.success', { ns: 'common' }) })
      onEnabled()
    } catch (err) {
      const message =
        isApiError(err) && err.status === 403 ? t('bootstrap.forbidden') : t('bootstrap.error')
      if (isApiError(err) && err.status === 403) {
        setError(message)
      } else {
        setError(message)
      }
      toast({
        variant: 'destructive',
        title: t('common:errors.generic', { ns: 'common' }),
        description: message,
        action: (
          <ToastAction altText={t('common:actions.retry', { ns: 'common', defaultValue: 'Retry' })} onClick={() => void handleEnable()}>
            {t('common:actions.retry', { ns: 'common', defaultValue: 'Retry' })}
          </ToastAction>
        ),
      })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-background">
      <Card className="w-full max-w-xl">
        <CardHeader>
          <CardTitle>{t('bootstrap.title')}</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm text-muted-foreground">{t('bootstrap.subtitle')}</p>
          <Input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder={t('bootstrap.namePlaceholder')}
            disabled={submitting}
          />
          <Input
            value={slug}
            onChange={(e) => setSlug(e.target.value)}
            placeholder={t('bootstrap.slugPlaceholder')}
            disabled={submitting}
          />
          {error ? <p className="text-sm text-destructive">{error}</p> : null}
          <div className="flex items-center justify-between">
            <Link to="/client" className="text-sm text-primary hover:underline">
              {t('bootstrap.backToClient')}
            </Link>
            <Button onClick={handleEnable} disabled={!canSubmit}>
              {submitting ? t('bootstrap.submitting') : t('bootstrap.cta')}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
