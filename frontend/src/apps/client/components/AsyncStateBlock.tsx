import { Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'

interface AsyncStateBlockProps {
  type: 'loading' | 'error' | 'empty'
  title?: string
  description: string
  actionLabel?: string
  onAction?: () => void
}

export function AsyncStateBlock({
  type,
  title,
  description,
  actionLabel,
  onAction,
}: AsyncStateBlockProps) {
  if (type === 'loading') {
    return (
      <div className="flex items-center justify-center rounded-lg border bg-card py-14">
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Loader2 className="h-4 w-4 animate-spin" />
          <span>{description}</span>
        </div>
      </div>
    )
  }

  if (type === 'error') {
    return (
      <Alert variant="destructive">
        <AlertTitle>{title}</AlertTitle>
        <AlertDescription>
          <p>{description}</p>
          {actionLabel && onAction ? (
            <Button type="button" variant="outline" size="sm" onClick={onAction} className="mt-2">
              {actionLabel}
            </Button>
          ) : null}
        </AlertDescription>
      </Alert>
    )
  }

  return (
    <div className="rounded-lg border bg-card px-4 py-10 text-center">
      {title ? <h3 className="text-base font-semibold">{title}</h3> : null}
      <p className="mt-2 text-sm text-muted-foreground">{description}</p>
      {actionLabel && onAction ? (
        <Button type="button" variant="outline" className="mt-5" onClick={onAction}>
          {actionLabel}
        </Button>
      ) : null}
    </div>
  )
}
