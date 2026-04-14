package com.bookingcore.security;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Internal platform actor kind carried in JWT ({@code role} claim). End-customer booking flows use the
 * {@code /api/client} namespace; a logged-in storefront account maps to {@link #CLIENT}.
 */
public enum PlatformUserRole {
  SYSTEM_ADMIN,
  MERCHANT,
  SUB_MERCHANT,
  CLIENT;

  /**
   * Canonical cross-domain role code used by RBAC-facing integrations while preserving existing enum values
   * and ROLE_* authorities for backward compatibility.
   */
  public String canonicalCode() {
    return switch (this) {
      case SYSTEM_ADMIN -> "SYSTEM_ADMIN";
      case MERCHANT -> "MERCHANT_OWNER";
      case SUB_MERCHANT -> "MERCHANT_STAFF";
      case CLIENT -> "CLIENT_USER";
    };
  }

  public Set<String> aliases() {
    LinkedHashSet<String> out = new LinkedHashSet<>();
    out.add(name());
    out.add(canonicalCode());
    return Set.copyOf(out);
  }

  public Set<String> authorityAliases() {
    LinkedHashSet<String> out = new LinkedHashSet<>();
    for (String code : aliases()) {
      out.add("ROLE_" + code);
    }
    return Set.copyOf(out);
  }

  public static Optional<PlatformUserRole> parse(String roleCode) {
    if (roleCode == null || roleCode.isBlank()) {
      return Optional.empty();
    }
    String normalized = roleCode.trim().toUpperCase(Locale.ROOT);
    return switch (normalized) {
      case "SYSTEM_ADMIN" -> Optional.of(SYSTEM_ADMIN);
      case "MERCHANT", "MERCHANT_OWNER" -> Optional.of(MERCHANT);
      case "SUB_MERCHANT", "MERCHANT_STAFF" -> Optional.of(SUB_MERCHANT);
      case "CLIENT", "CLIENT_USER" -> Optional.of(CLIENT);
      default -> Optional.empty();
    };
  }

  public static Set<String> acceptedRoleCodes() {
    LinkedHashSet<String> out = new LinkedHashSet<>();
    for (PlatformUserRole role : values()) {
      out.addAll(role.aliases());
    }
    return Set.copyOf(out);
  }

  public String authority() {
    return "ROLE_" + name();
  }
}
