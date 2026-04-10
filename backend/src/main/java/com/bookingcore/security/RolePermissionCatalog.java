package com.bookingcore.security;

import java.util.EnumMap;
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

  public RolePermissionCatalog() {
    permissionsByRole.put(
        PlatformUserRole.SYSTEM_ADMIN,
        Set.of(
            "system.dashboard.read",
            "system.settings.write",
            "system.users.read",
            "system.users.write",
            "merchant.registry.manage",
            "merchant.portal.access",
            "me.navigation.read"));
    permissionsByRole.put(
        PlatformUserRole.MERCHANT, Set.of("merchant.portal.access", "me.navigation.read"));
    permissionsByRole.put(
        PlatformUserRole.SUB_MERCHANT, Set.of("merchant.portal.access", "me.navigation.read"));
    permissionsByRole.put(
        PlatformUserRole.CLIENT, Set.of("client.portal.access", "me.navigation.read"));
  }

  public Set<String> permissionsFor(PlatformUserRole role) {
    return permissionsByRole.getOrDefault(role, Set.of());
  }

  public boolean hasPermission(PlatformUserRole role, String permission) {
    return permissionsFor(role).contains(permission);
  }
}
