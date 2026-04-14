/** Static placeholder shipped with the client bundle (`frontend/public`). */
export const DEMO_IMAGE_FALLBACK = '/demo-image-fallback.svg'

export function resolveDemoImageUrl(imageUrl: string | null | undefined): string {
  if (typeof imageUrl === 'string') {
    const trimmed = imageUrl.trim()
    if (trimmed.length > 0) {
      return trimmed
    }
  }
  return DEMO_IMAGE_FALLBACK
}
