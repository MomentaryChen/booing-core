package com.bookingcore.security;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Minimal RBAC permission catalog used by unified-login responses and future method-level checks.
 */
@Component
public class RolePermissionCatalog {

  private final Map<PlatformUserRole, Set<String>> permissionsByRole =
      new EnumMap<>(PlatformUserRole.class);
  private final Map<String, Set<String>> permissionsByCanonicalRole = new LinkedHashMap<>();

  public RolePermissionCatalog() {
    register(
        PlatformUserRole.SYSTEM_ADMIN,
        Set.of(
            "system.dashboard.read",
            "system.settings.write",
            "system.users.read",
            "system.users.write",
            "merchant.registry.manage",
            "merchant.portal.access",
            "me.navigation.read"));
    register(PlatformUserRole.MERCHANT, Set.of("merchant.portal.access", "me.navigation.read"));
    register(PlatformUserRole.SUB_MERCHANT, Set.of("merchant.portal.access", "me.navigation.read"));
    register(PlatformUserRole.CLIENT, Set.of("client.portal.access", "me.navigation.read"));
  }

  private void register(PlatformUserRole role, Set<String> permissions) {
    permissionsByRole.put(role, permissions);
    permissionsByCanonicalRole.put(role.canonicalCode(), permissions);
  }

  public Set<String> permissionsFor(PlatformUserRole role) {
    return permissionsByRole.getOrDefault(role, Set.of());
  }

  public Set<String> permissionsForCanonicalRole(String canonicalRoleCode) {
    return permissionsByCanonicalRole.getOrDefault(canonicalRoleCode, Set.of());
  }

  public boolean hasPermission(PlatformUserRole role, String permission) {
    return permissionsFor(role).contains(permission);
  }
}
