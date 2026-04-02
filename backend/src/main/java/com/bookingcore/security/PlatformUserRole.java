package com.bookingcore.security;

/**
 * Internal platform actor kind carried in JWT ({@code role} claim). End-customer booking flows use the
 * {@code /api/client} namespace; a logged-in storefront account maps to {@link #CLIENT}.
 */
public enum PlatformUserRole {
  SYSTEM_ADMIN,
  MERCHANT,
  SUB_MERCHANT,
  CLIENT;

  public String authority() {
    return "ROLE_" + name();
  }
}
