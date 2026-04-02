import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import { getStoredAccessToken } from "./api/client";

function decodeJwtPayload(token) {
  try {
    const parts = token.split(".");
    if (parts.length !== 3) return null;
    const base64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    let padded = base64;
    while (padded.length % 4) padded += "=";
    const json = typeof atob !== "undefined" ? atob(padded) : null;
    if (!json) return null;
    return JSON.parse(json);
  } catch {
    return null;
  }
}

/** Reads {@code role} claim from platform JWT (SYSTEM_ADMIN, MERCHANT, SUB_MERCHANT, CLIENT). */
export function getRoleFromAccessToken(token) {
  if (!token) return null;
  const payload = decodeJwtPayload(token);
  return payload?.role ?? null;
}

/** Reads optional {@code merchantId} claim from platform JWT. */
export function getMerchantIdFromAccessToken(token) {
  if (!token) return null;
  const payload = decodeJwtPayload(token);
  const n = Number(payload?.merchantId);
  return Number.isFinite(n) && n > 0 ? n : null;
}

export function isSystemAdminRole(role) {
  return role === "SYSTEM_ADMIN";
}

export function useStoredPlatformRole() {
  const location = useLocation();
  const [role, setRole] = useState(() => getRoleFromAccessToken(getStoredAccessToken()));

  useEffect(() => {
    setRole(getRoleFromAccessToken(getStoredAccessToken()));
  }, [location.pathname]);

  return role;
}
