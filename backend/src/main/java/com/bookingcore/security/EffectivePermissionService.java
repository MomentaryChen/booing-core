package com.bookingcore.security;

import java.util.UUID;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.platform.PlatformUserRepository;
import com.bookingcore.modules.platform.rbac.PlatformUserRbacBindingRepository;
import com.bookingcore.modules.platform.rbac.RbacRoleRepository;
import java.util.HashSet;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

/**
 * Resolves effective permission codes for a {@link PlatformPrincipal}. If the user exists in
 * {@code platform_users} and {@code rbac_roles} has at least one row (RBAC initialized), an active
 * {@code platform_user_rbac_bindings} row is required and permission codes come from the bound role.
 * If {@code rbac_roles} is empty, falls back to {@link RolePermissionCatalog} for that user, aligned
 * with {@link com.bookingcore.service.PlatformBootstrapService} which skips binding creation when
 * RBAC is not seeded. Users not present in {@code platform_users} get catalog permissions only under
 * the {@code dev} profile; otherwise none.
 */
@Service
public class EffectivePermissionService {

  private final RolePermissionCatalog rolePermissionCatalog;
  private final PlatformUserRepository platformUserRepository;
  private final PlatformUserRbacBindingRepository platformUserRbacBindingRepository;
  private final RbacRoleRepository rbacRoleRepository;
  private final Environment environment;

  public EffectivePermissionService(
      RolePermissionCatalog rolePermissionCatalog,
      PlatformUserRepository platformUserRepository,
      PlatformUserRbacBindingRepository platformUserRbacBindingRepository,
      RbacRoleRepository rbacRoleRepository,
      Environment environment) {
    this.rolePermissionCatalog = rolePermissionCatalog;
    this.platformUserRepository = platformUserRepository;
    this.platformUserRbacBindingRepository = platformUserRbacBindingRepository;
    this.rbacRoleRepository = rbacRoleRepository;
    this.environment = environment;
  }

  public Set<String> permissionSetFor(PlatformPrincipal principal) {
    var dbUser = platformUserRepository.findByUsername(principal.username());
    if (dbUser.isPresent()) {
      if (isSystemOperator(dbUser.get())) {
        // SYSTEM_ADMIN accounts may impersonate merchant contexts for support/debugging.
        // RBAC bindings are optional for operators; treat catalog permissions as authoritative.
        return new HashSet<>(catalogPermissionsForOperator(principal.role()));
      }
      UUID userId = dbUser.get().getId();
      if (rbacRoleRepository.count() == 0L) {
        return new HashSet<>(rolePermissionCatalog.permissionsFor(principal.role()));
      }
      if (!platformUserRbacBindingRepository.existsActiveBindingForContext(
          userId, principal.role().name(), principal.merchantId())) {
        return Set.of();
      }
      return new HashSet<>(
          platformUserRbacBindingRepository.findPermissionCodesForUserContext(
              userId, principal.role().name(), principal.merchantId()));
    }
    if (!environment.acceptsProfiles(Profiles.of("dev"))) {
      return Set.of();
    }
    return new HashSet<>(rolePermissionCatalog.permissionsFor(principal.role()));
  }

  private static boolean isSystemOperator(PlatformUser user) {
    return user.getRole() == PlatformUserRole.SYSTEM_ADMIN;
  }

  /**
   * Union of catalog permissions for support impersonation contexts.
   *
   * <p>When an operator switches JWT context to {@link PlatformUserRole#MERCHANT} /
   * {@link PlatformUserRole#SUB_MERCHANT}, we still want merchant portal access without requiring
   * merchant RBAC bindings on the operator account.
   */
  private Set<String> catalogPermissionsForOperator(PlatformUserRole activeContextRole) {
    if (activeContextRole == null || activeContextRole == PlatformUserRole.SYSTEM_ADMIN) {
      return rolePermissionCatalog.permissionsFor(PlatformUserRole.SYSTEM_ADMIN);
    }
    Set<String> merged = new HashSet<>();
    merged.addAll(rolePermissionCatalog.permissionsFor(PlatformUserRole.SYSTEM_ADMIN));
    merged.addAll(rolePermissionCatalog.permissionsFor(activeContextRole));
    return merged;
  }

  public List<String> sortedPermissionCodesFor(PlatformPrincipal principal) {
    return permissionSetFor(principal).stream().sorted().toList();
  }

  public boolean hasPermission(PlatformPrincipal principal, String permission) {
    return permissionSetFor(principal).contains(permission);
  }
}
