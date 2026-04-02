/**
 * When `false`, merchant UI routes stay reachable without JWT (for local dev with empty jwt.secret).
 * Default: login required.
 */
export function isMerchantAuthRequired() {
  return import.meta.env.VITE_MERCHANT_AUTH_REQUIRED !== "false";
}
