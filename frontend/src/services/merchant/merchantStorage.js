const MERCHANT_ID_KEY = "booking-core-merchant-id";

export function getStoredMerchantId() {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(MERCHANT_ID_KEY);
  const n = Number(raw);
  return Number.isFinite(n) && n > 0 ? n : null;
}

export function setStoredMerchantId(id) {
  if (typeof window === "undefined") return;
  const n = Number(id);
  if (Number.isFinite(n) && n > 0) {
    window.localStorage.setItem(MERCHANT_ID_KEY, String(Math.trunc(n)));
  } else {
    clearStoredMerchantId();
  }
}

export function clearStoredMerchantId() {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(MERCHANT_ID_KEY);
}
