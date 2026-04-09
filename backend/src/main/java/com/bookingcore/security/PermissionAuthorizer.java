package com.bookingcore.security;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component("permissionAuthorizer")
public class PermissionAuthorizer {

  private final RolePermissionCatalog rolePermissionCatalog;
  private final EffectivePermissionService effectivePermissionService;

  /** For unit tests without Spring: catalog-only, no DB resolution. */
  public PermissionAuthorizer(RolePermissionCatalog rolePermissionCatalog) {
    this.rolePermissionCatalog = rolePermissionCatalog;
    this.effectivePermissionService = null;
  }

  @Autowired
  public PermissionAuthorizer(
      RolePermissionCatalog rolePermissionCatalog,
      ObjectProvider<EffectivePermissionService> effectivePermissionService) {
    this.rolePermissionCatalog = rolePermissionCatalog;
    this.effectivePermissionService = effectivePermissionService.getIfAvailable();
  }

  public boolean hasPermission(Authentication authentication, String permission) {
    if (effectivePermissionService != null
        && authentication != null
        && authentication.getPrincipal() instanceof PlatformPrincipal p) {
      return effectivePermissionService.hasPermission(p, permission);
    }
    for (PlatformUserRole role : resolveRoles(authentication)) {
      if (rolePermissionCatalog.hasPermission(role, permission)) {
        return true;
      }
    }
    return false;
  }

  public boolean hasAnyPermission(Authentication authentication, Collection<String> permissions) {
    if (permissions == null || permissions.isEmpty()) {
      return false;
    }
    if (effectivePermissionService != null
        && authentication != null
        && authentication.getPrincipal() instanceof PlatformPrincipal p) {
      Set<String> effective = effectivePermissionService.permissionSetFor(p);
      for (String perm : permissions) {
        if (effective.contains(perm)) {
          return true;
        }
      }
      return false;
    }
    for (PlatformUserRole role : resolveRoles(authentication)) {
      for (String permission : permissions) {
        if (rolePermissionCatalog.hasPermission(role, permission)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Any permission that may touch {@code /api/system/**} per {@link RolePermissionCatalog} baseline.
   */
  public boolean hasSystemNamespacePermission(Authentication authentication) {
    return hasAnyPermission(
        authentication,
        List.of(
            "system.dashboard.read",
            "system.settings.write",
            "merchant.registry.manage"));
  }

  private Set<PlatformUserRole> resolveRoles(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return Set.of();
    }
    Set<PlatformUserRole> roles = EnumSet.noneOf(PlatformUserRole.class);
    for (GrantedAuthority authority : authentication.getAuthorities()) {
      String value = authority.getAuthority();
      if (value != null && value.startsWith("ROLE_")) {
        try {
          roles.add(PlatformUserRole.valueOf(value.substring("ROLE_".length())));
        } catch (IllegalArgumentException ignored) {
          // Unknown role values are denied by default.
        }
      }
    }
    return roles;
  }
}
