package com.bookingcore.security;

import com.bookingcore.modules.platform.PlatformUserRepository;
import com.bookingcore.modules.platform.rbac.PlatformUserRbacBindingRepository;
import com.bookingcore.modules.platform.rbac.RbacRoleRepository;
import java.util.HashSet;
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
      Long userId = dbUser.get().getId();
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

  public List<String> sortedPermissionCodesFor(PlatformPrincipal principal) {
    return permissionSetFor(principal).stream().sorted().toList();
  }

  public boolean hasPermission(PlatformPrincipal principal, String permission) {
    return permissionSetFor(principal).contains(permission);
  }
}
