import {
  getUnauthorizedRedirect,
  inferLoginIntentFromPath,
  parseAuthOverlayFromSearch,
} from "../auth/sessionRouting";

const API_BASE =
  (typeof import.meta !== "undefined" ? import.meta.env?.VITE_API_BASE : undefined) ||
  "http://localhost:28080/api";

const JWT_STORAGE_KEY = "booking_core_access_token";

export function getStoredAccessToken() {
  if (typeof localStorage === "undefined") {
    return null;
  }
  return localStorage.getItem(JWT_STORAGE_KEY);
}

export function clearStoredAccessToken() {
  if (typeof localStorage === "undefined") {
    return;
  }
  localStorage.removeItem(JWT_STORAGE_KEY);
}

const SYSTEM_ADMIN_TOKEN =
  typeof import.meta !== "undefined" ? import.meta.env?.VITE_SYSTEM_ADMIN_TOKEN : undefined;

const ENV_ACCESS_TOKEN =
  typeof import.meta !== "undefined" ? import.meta.env?.VITE_API_ACCESS_TOKEN : undefined;

function isAnyLoginPath(fullPath) {
  if (typeof fullPath !== "string") return false;
  const q = fullPath.indexOf("?");
  const pathname = q >= 0 ? fullPath.slice(0, q) : fullPath;
  const search = q >= 0 ? fullPath.slice(q) : "";
  if (parseAuthOverlayFromSearch(search)) return true;
  return (
    pathname.startsWith("/login") ||
    pathname.startsWith("/merchant/login") ||
    pathname.startsWith("/system/login") ||
    pathname.startsWith("/client/login")
  );
}

function storedOrEnvAccessToken() {
  if (typeof localStorage === "undefined") {
    return ENV_ACCESS_TOKEN;
  }
  return getStoredAccessToken() || ENV_ACCESS_TOKEN || undefined;
}

function mergeHeaders(path, optionHeaders, forceAuth) {
  const headers = { "Content-Type": "application/json", ...(optionHeaders || {}) };
  if (SYSTEM_ADMIN_TOKEN && path.startsWith("/system")) {
    headers["X-System-Admin-Token"] = SYSTEM_ADMIN_TOKEN;
  }
  const access = storedOrEnvAccessToken();
  const authPathNeedsBearer =
    path.startsWith("/auth/") && path !== "/auth/login" && !path.startsWith("/auth/login?");
  if (
    access &&
    (forceAuth ||
      path.startsWith("/merchant") ||
      path.startsWith("/system") ||
      path.startsWith("/me") ||
      authPathNeedsBearer)
  ) {
    headers.Authorization = `Bearer ${access}`;
  }
  return headers;
}

/** Persist JWT when using booking.platform.jwt.secret enforcement (merchant / system APIs). */
export function setStoredAccessToken(token) {
  if (typeof localStorage === "undefined") {
    return;
  }
  if (token) {
    localStorage.setItem(JWT_STORAGE_KEY, token);
  } else {
    clearStoredAccessToken();
  }
}

export async function api(path, options = {}) {
  const { withAuth = false, ...fetchOptions } = options;
  const res = await fetch(`${API_BASE}${path}`, {
    headers: mergeHeaders(path, fetchOptions.headers, withAuth),
    ...fetchOptions,
  });
  if (!res.ok) {
    if (
      res.status === 401 &&
      (path.startsWith("/merchant") ||
        path.startsWith("/system") ||
        path.startsWith("/me") ||
        (path.startsWith("/auth/") && path !== "/auth/login" && !path.startsWith("/auth/login?")))
    ) {
      clearStoredAccessToken();
      try {
        const { clearStoredMerchantId } = await import("../merchant/merchantStorage");
        clearStoredMerchantId();
      } catch {
        // ignore
      }
      if (typeof window !== "undefined") {
        const pathname = window.location.pathname || "/";
        const search = window.location.search || "";
        const hash = window.location.hash || "";
        const currentPath = `${pathname}${search}${hash}`;
        const intent = inferLoginIntentFromPath(pathname);
        const loc = { pathname, search, hash };
        if (!isAnyLoginPath(currentPath)) {
          window.location.assign(getUnauthorizedRedirect({ location: loc, intent }));
        } else {
          window.location.assign(getUnauthorizedRedirect({ location: { pathname: "/", search: "", hash: "" }, intent }));
        }
      }
    }
    let message = "Request failed";
    try {
      const body = await res.json();
      message = body.message || message;
    } catch (e) {
      // no-op
    }
    throw new Error(message);
  }
  if (res.status === 204) return null;
  return res.json();
}
