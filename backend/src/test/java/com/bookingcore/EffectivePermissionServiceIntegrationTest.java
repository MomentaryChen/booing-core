package com.bookingcore;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.platform.rbac.PlatformRbacBindingStatus;
import com.bookingcore.modules.platform.rbac.PlatformUserRbacBinding;
import com.bookingcore.modules.platform.rbac.RbacPermission;
import com.bookingcore.modules.platform.rbac.RbacRole;
import com.bookingcore.security.EffectivePermissionService;
import com.bookingcore.security.PlatformPrincipal;
import com.bookingcore.security.PlatformUserRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class EffectivePermissionServiceIntegrationTest {

  @PersistenceContext private EntityManager entityManager;

  @Autowired private EffectivePermissionService effectivePermissionService;

  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void whenActiveBindingExists_dbPermissionsOverrideCatalog() {
    RbacPermission nav = new RbacPermission();
    nav.setCode("me.navigation.read");
    entityManager.persist(nav);

    RbacRole merchantRole = new RbacRole();
    merchantRole.setCode("MERCHANT");
    merchantRole.getPermissions().add(nav);
    entityManager.persist(merchantRole);

    Merchant merchant = new Merchant();
    merchant.setName("Rbac Test Merchant");
    merchant.setSlug("rbac-test-merchant-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setServiceLimit(5);
    entityManager.persist(merchant);

    PlatformUser user = new PlatformUser();
    user.setUsername("rbac-db-merchant-" + System.nanoTime());
    user.setPasswordHash(passwordEncoder.encode("secret"));
    user.setRole(PlatformUserRole.MERCHANT);
    user.setMerchant(merchant);
    user.setEnabled(true);
    entityManager.persist(user);

    PlatformUserRbacBinding binding = new PlatformUserRbacBinding();
    binding.setPlatformUser(user);
    binding.setRbacRole(merchantRole);
    binding.setMerchant(merchant);
    binding.setStatus(PlatformRbacBindingStatus.ACTIVE);
    entityManager.persist(binding);

    entityManager.flush();

    PlatformPrincipal principal =
        new PlatformPrincipal(user.getUsername(), PlatformUserRole.MERCHANT, merchant.getId());

    assertThat(effectivePermissionService.hasPermission(principal, "me.navigation.read")).isTrue();
    assertThat(effectivePermissionService.hasPermission(principal, "merchant.portal.access"))
        .isFalse();
  }

  @Test
  void dbUserWithoutAnyBinding_isFailClosed() {
    RbacRole rbacInitialized = new RbacRole();
    rbacInitialized.setCode("z" + System.nanoTime());
    entityManager.persist(rbacInitialized);
    entityManager.flush();

    Merchant merchant = new Merchant();
    merchant.setName("No Binding Merchant");
    merchant.setSlug("no-binding-merchant-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setServiceLimit(5);
    entityManager.persist(merchant);

    PlatformUser user = new PlatformUser();
    user.setUsername("rbac-no-binding-" + System.nanoTime());
    user.setPasswordHash(passwordEncoder.encode("secret"));
    user.setRole(PlatformUserRole.MERCHANT);
    user.setMerchant(merchant);
    user.setEnabled(true);
    entityManager.persist(user);
    entityManager.flush();

    PlatformPrincipal principal =
        new PlatformPrincipal(user.getUsername(), PlatformUserRole.MERCHANT, merchant.getId());
    assertThat(effectivePermissionService.permissionSetFor(principal)).isEmpty();
  }

  @Test
  void dbUserWithInactiveBinding_isFailClosed() {
    RbacPermission nav = new RbacPermission();
    nav.setCode("me.navigation.read");
    entityManager.persist(nav);

    RbacRole merchantRole = new RbacRole();
    merchantRole.setCode("MERCHANT");
    merchantRole.getPermissions().add(nav);
    entityManager.persist(merchantRole);

    Merchant merchant = new Merchant();
    merchant.setName("Inactive Binding Merchant");
    merchant.setSlug("inactive-binding-merchant-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setServiceLimit(5);
    entityManager.persist(merchant);

    PlatformUser user = new PlatformUser();
    user.setUsername("rbac-inactive-binding-" + System.nanoTime());
    user.setPasswordHash(passwordEncoder.encode("secret"));
    user.setRole(PlatformUserRole.MERCHANT);
    user.setMerchant(merchant);
    user.setEnabled(true);
    entityManager.persist(user);

    PlatformUserRbacBinding binding = new PlatformUserRbacBinding();
    binding.setPlatformUser(user);
    binding.setRbacRole(merchantRole);
    binding.setMerchant(merchant);
    binding.setStatus(PlatformRbacBindingStatus.DISABLED);
    entityManager.persist(binding);
    entityManager.flush();

    PlatformPrincipal principal =
        new PlatformPrincipal(user.getUsername(), PlatformUserRole.MERCHANT, merchant.getId());
    assertThat(effectivePermissionService.permissionSetFor(principal)).isEmpty();
  }

  @Test
  void systemAdminWithoutRbacBinding_stillGetsCatalogPermissions_includingMerchantPortal() {
    RbacRole rbacInitialized = new RbacRole();
    rbacInitialized.setCode("z" + System.nanoTime());
    entityManager.persist(rbacInitialized);
    entityManager.flush();

    Merchant merchant = new Merchant();
    merchant.setName("Admin Impersonation Merchant");
    merchant.setSlug("admin-impersonation-merchant-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setServiceLimit(5);
    entityManager.persist(merchant);

    PlatformUser admin = new PlatformUser();
    admin.setUsername("rbac-admin-no-binding-" + System.nanoTime());
    admin.setPasswordHash(passwordEncoder.encode("secret"));
    admin.setRole(PlatformUserRole.SYSTEM_ADMIN);
    admin.setMerchant(null);
    admin.setEnabled(true);
    entityManager.persist(admin);
    entityManager.flush();

    PlatformPrincipal merchantScoped =
        new PlatformPrincipal(admin.getUsername(), PlatformUserRole.MERCHANT, merchant.getId());

    assertThat(effectivePermissionService.hasPermission(merchantScoped, "merchant.portal.access"))
        .isTrue();
    assertThat(effectivePermissionService.hasPermission(merchantScoped, "system.dashboard.read"))
        .isTrue();
  }
}
