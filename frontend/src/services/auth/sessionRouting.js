import { ROUTE_KEYS } from "../../navigation/routeKeys";

const INTENT_DEFAULT_PATH = {
  merchant: "/merchant",
  system: "/system",
  client: "/client",
};

const INTENT_ALLOWED_PREFIXES = {
  merchant: ["/merchant"],
  system: ["/system"],
  client: ["/client"],
};

function isSafeInternalPath(path) {
  return typeof path === "string" && path.startsWith("/") && !path.startsWith("//") && !path.startsWith("/\\");
}

export const AUTH_OVERLAY_LOGIN = "login";
export const AUTH_OVERLAY_REGISTER = "register";

function authOverlayModeFromSearch(search) {
  if (typeof search !== "string" || !search) return null;
  const q = search.startsWith("?") ? search.slice(1) : search;
  const mode = new URLSearchParams(q).get("auth");
  return mode === AUTH_OVERLAY_LOGIN || mode === AUTH_OVERLAY_REGISTER ? mode : null;
}

function isLoginPath(path) {
  if (typeof path !== "string") return false;
  const q = path.indexOf("?");
  const pathname = q >= 0 ? path.slice(0, q) : path;
  const search = q >= 0 ? path.slice(q) : "";
  if (authOverlayModeFromSearch(search)) return true;

  return (
    pathname === "/login" ||
    pathname.startsWith("/login/") ||
    pathname === "/merchant/login" ||
    pathname === "/system/login" ||
    pathname === "/client/login"
  );
}

function isUnderPrefix(path, prefix) {
  return path === prefix || path.startsWith(`${prefix}/`);
}

export function inferLoginIntentFromPath(pathname) {
  if (typeof pathname !== "string") return "merchant";
  if (pathname.startsWith("/system")) return "system";
  if (pathname.startsWith("/client")) return "client";
  return "merchant";
}

export function buildReturnUrlFromLocation(location) {
  if (!location) return "/";
  return `${location.pathname || "/"}${location.search || ""}${location.hash || ""}`;
}

export function getDefaultPathForIntent(intent) {
  return INTENT_DEFAULT_PATH[intent] || INTENT_DEFAULT_PATH.merchant;
}

export function sanitizeReturnPath(raw, intent) {
  const fallback = getDefaultPathForIntent(intent);
  if (!isSafeInternalPath(raw) || isLoginPath(raw)) return fallback;

  const allowedPrefixes = INTENT_ALLOWED_PREFIXES[intent] || ["/"];
  return allowedPrefixes.some((prefix) => isUnderPrefix(raw, prefix)) ? raw : fallback;
}

const ROLE_ALLOWED_PREFIXES = {
  SYSTEM_ADMIN: ["/system"],
  MERCHANT: ["/merchant"],
  SUB_MERCHANT: ["/merchant"],
  CLIENT: ["/client"],
};

const ROLE_DEFAULT_PATH = {
  SYSTEM_ADMIN: "/system",
  MERCHANT: "/merchant",
  SUB_MERCHANT: "/merchant",
  CLIENT: "/client",
};

/** Defaults after login when a recent public registration chose a persona (registerType). */
const REGISTER_TYPE_DEFAULT_PATH = {
  MERCHANT: "/merchant",
  CLIENT: "/client",
  SYSTEM_ADMIN: "/system",
  SUB_MERCHANT: "/merchant",
};

const REGISTER_USER_TYPE_STORAGE_KEY = "booking.postRegisterRegisterType";

export function setPendingRegisterUserType(registerType) {
  try {
    if (registerType) sessionStorage.setItem(REGISTER_USER_TYPE_STORAGE_KEY, registerType);
  } catch {
    // ignore
  }
}

export function peekPendingRegisterUserType() {
  try {
    return sessionStorage.getItem(REGISTER_USER_TYPE_STORAGE_KEY);
  } catch {
    return null;
  }
}

export function consumePendingRegisterUserType() {
  try {
    const v = sessionStorage.getItem(REGISTER_USER_TYPE_STORAGE_KEY);
    sessionStorage.removeItem(REGISTER_USER_TYPE_STORAGE_KEY);
    return v;
  } catch {
    return null;
  }
}

function pathOnly(pathWithQuery) {
  const q = pathWithQuery.indexOf("?");
  return q >= 0 ? pathWithQuery.slice(0, q) : pathWithQuery;
}

/**
 * User-provided returnUrl: must match current role namespace (and never be login-like).
 * @returns {string | null}
 */
export function trySanitizeReturnUrlForRole(raw, role) {
  if (raw == null || String(raw).trim() === "") return null;
  const s = String(raw).trim();
  if (!isSafeInternalPath(s) || isLoginPath(s)) return null;
  const allowed = ROLE_ALLOWED_PREFIXES[role];
  if (!allowed) return null;
  const base = pathOnly(s);
  return allowed.some((prefix) => isUnderPrefix(base, prefix)) ? s : null;
}

/**
 * Server-suggested path (login/register responses): strict internal allowlist, no open redirect.
 * @returns {string | null}
 */
export function trySanitizeServerNextDestination(raw) {
  if (raw == null || String(raw).trim() === "") return null;
  const s = String(raw).trim();
  if (!isSafeInternalPath(s)) return null;
  const pathname = pathOnly(s);
  if (pathname === "/") return s;
  const roots = ["/merchant", "/client", "/system", "/login", "/403", "/demo"];
  return roots.some((r) => pathname === r || pathname.startsWith(`${r}/`)) ? s : null;
}

/**
 * Post-login navigation: safeReturnUrl > backend nextDestination > defaultByRegisterType > role default.
 * @param {{ returnUrlRaw: string | null | undefined, nextDestination?: string | null | undefined, role: string, intent?: string, registerUserType?: string | null | undefined }} args
 */
export function resolvePostLoginDestination({
  returnUrlRaw,
  nextDestination,
  role,
  intent = "merchant",
  registerUserType = null,
}) {
  const roleDefault = ROLE_DEFAULT_PATH[role] || getDefaultPathForIntent(intent);
  const typeDefault =
    registerUserType && REGISTER_TYPE_DEFAULT_PATH[registerUserType]
      ? REGISTER_TYPE_DEFAULT_PATH[registerUserType]
      : roleDefault;

  const fromReturn = trySanitizeReturnUrlForRole(returnUrlRaw, role);
  if (fromReturn) return fromReturn;

  const fromServer = trySanitizeServerNextDestination(nextDestination);
  if (fromServer) return fromServer;

  return typeDefault;
}

export function hasNamespaceCapability({ intent, role, routeKeys }) {
  if (intent === "system") {
    return role === "SYSTEM_ADMIN" || (Array.isArray(routeKeys) && routeKeys.includes(ROUTE_KEYS.SYSTEM_DASHBOARD));
  }
  if (intent === "merchant") {
    return (
      role === "MERCHANT" ||
      role === "SUB_MERCHANT" ||
      (Array.isArray(routeKeys) && routeKeys.some((key) => key.startsWith("nav.merchant")))
    );
  }
  if (intent === "client") {
    return role === "CLIENT" || (Array.isArray(routeKeys) && routeKeys.includes(ROUTE_KEYS.CLIENT_TODO));
  }
  return false;
}

export function getUnauthorizedRedirect({ location, intent }) {
  const ret = buildReturnUrlFromLocation(location);
  const params = new URLSearchParams();
  params.set("auth", AUTH_OVERLAY_LOGIN);
  params.set("intent", intent);
  params.set("returnUrl", ret);
  return `/?${params.toString()}`;
}

/** Merge overlay query params into a pathname (preserves unrelated search keys). */
export function hrefAuthOverlay(pathname, currentSearch, { mode, intent = "merchant", returnUrl, registered }) {
  const p = new URLSearchParams((currentSearch || "").replace(/^\?/, ""));
  p.set("auth", mode);
  p.set("intent", intent);
  if (returnUrl != null && returnUrl !== "") p.set("returnUrl", returnUrl);
  else p.delete("returnUrl");
  if (registered) p.set("registered", "1");
  else p.delete("registered");
  const qs = p.toString();
  return { pathname, search: qs ? `?${qs}` : "" };
}

export function stripAuthOverlayParams(search) {
  const raw = (search || "").replace(/^\?/, "");
  const p = new URLSearchParams(raw);
  p.delete("auth");
  p.delete("intent");
  p.delete("returnUrl");
  p.delete("registered");
  const qs = p.toString();
  return qs ? `?${qs}` : "";
}

/**
 * @returns {{ mode: string, intent: string, returnUrl: string | null, registered: boolean } | null}
 */
export function parseAuthOverlayFromSearch(search) {
  const raw = (search || "").replace(/^\?/, "");
  const p = new URLSearchParams(raw);
  const mode = p.get("auth");
  if (mode !== AUTH_OVERLAY_LOGIN && mode !== AUTH_OVERLAY_REGISTER) return null;
  const rawIntent = p.get("intent");
  const intent = rawIntent === "system" || rawIntent === "client" ? rawIntent : "merchant";
  return {
    mode,
    intent,
    returnUrl: p.get("returnUrl"),
    registered: p.get("registered") === "1",
  };
}
