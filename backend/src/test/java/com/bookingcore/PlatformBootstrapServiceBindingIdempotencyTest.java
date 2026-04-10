package com.bookingcore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookingcore.modules.merchant.MerchantRepository;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.platform.PlatformUserRepository;
import com.bookingcore.modules.platform.rbac.PlatformRbacBindingStatus;
import com.bookingcore.modules.platform.rbac.PlatformUserRbacBinding;
import com.bookingcore.modules.platform.rbac.PlatformUserRbacBindingRepository;
import com.bookingcore.modules.platform.rbac.RbacRole;
import com.bookingcore.modules.platform.rbac.RbacRoleRepository;
import com.bookingcore.security.PlatformUserRole;
import com.bookingcore.service.PlatformBootstrapService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PlatformBootstrapServiceBindingIdempotencyTest {

  @Autowired private PlatformBootstrapService platformBootstrapService;
  @Autowired private PlatformUserRepository platformUserRepository;
  @Autowired private PlatformUserRbacBindingRepository platformUserRbacBindingRepository;
  @Autowired private RbacRoleRepository rbacRoleRepository;
  @Autowired private MerchantRepository merchantRepository;

  @Test
  void rerunBootstrapDoesNotCreateDuplicateActiveBindingForSameContext() {
    seedCoreRoles();
    platformBootstrapService.run();
    PlatformUser admin = platformUserRepository.findByUsername("admin").orElseThrow();
    RbacRole role =
        rbacRoleRepository
            .findByCode("SYSTEM_ADMIN")
            .orElseGet(
                () -> {
                  RbacRole created = new RbacRole();
                  created.setCode("SYSTEM_ADMIN");
                  return rbacRoleRepository.save(created);
                });
    List<PlatformUserRbacBinding> existing =
        platformUserRbacBindingRepository.findBindingsForUserContext(
            admin.getId(), "SYSTEM_ADMIN", null);
    if (existing.isEmpty()) {
      PlatformUserRbacBinding disabled = new PlatformUserRbacBinding();
      disabled.setPlatformUser(admin);
      disabled.setRbacRole(role);
      disabled.setStatus(PlatformRbacBindingStatus.DISABLED);
      platformUserRbacBindingRepository.save(disabled);
    } else {
      existing.forEach(
          binding -> {
            binding.setStatus(PlatformRbacBindingStatus.DISABLED);
            platformUserRbacBindingRepository.save(binding);
          });
    }

    platformBootstrapService.run();
    long afterFirst =
        platformUserRbacBindingRepository.countActiveBindingsForContext(
            admin.getId(), "SYSTEM_ADMIN", null);
    assertThat(afterFirst).isEqualTo(1L);

    platformBootstrapService.run();
    long afterSecond =
        platformUserRbacBindingRepository.countActiveBindingsForContext(
            admin.getId(), "SYSTEM_ADMIN", null);
    assertThat(afterSecond).isEqualTo(1L);
  }

  @Test
  void rerunBootstrapKeepsSingleMerchantAdminUsersAndDoesNotOverwriteHashes() {
    seedCoreRoles();
    platformBootstrapService.run();
    PlatformUser merchant = platformUserRepository.findByUsername("merchant").orElseThrow();
    PlatformUser admin = platformUserRepository.findByUsername("admin").orElseThrow();
    String merchantHashBefore = merchant.getPasswordHash();
    String adminHashBefore = admin.getPasswordHash();
    Long merchantUserIdBefore = merchant.getId();
    Long adminUserIdBefore = admin.getId();
    Long merchantEntityIdBefore = merchantRepository.findBySlug("demo-merchant").orElseThrow().getId();

    platformBootstrapService.run();

    PlatformUser merchantAfter = platformUserRepository.findByUsername("merchant").orElseThrow();
    PlatformUser adminAfter = platformUserRepository.findByUsername("admin").orElseThrow();
    Long merchantEntityIdAfter = merchantRepository.findBySlug("demo-merchant").orElseThrow().getId();
    long merchantUserCount =
        platformUserRepository.findAll().stream().filter(u -> "merchant".equals(u.getUsername())).count();
    long adminUserCount =
        platformUserRepository.findAll().stream().filter(u -> "admin".equals(u.getUsername())).count();
    long merchantEntityCount =
        merchantRepository.findAll().stream().filter(m -> "demo-merchant".equals(m.getSlug())).count();

    assertThat(merchantAfter.getId()).isEqualTo(merchantUserIdBefore);
    assertThat(adminAfter.getId()).isEqualTo(adminUserIdBefore);
    assertThat(merchantAfter.getPasswordHash()).isEqualTo(merchantHashBefore);
    assertThat(adminAfter.getPasswordHash()).isEqualTo(adminHashBefore);
    assertThat(merchantEntityIdAfter).isEqualTo(merchantEntityIdBefore);
    assertThat(merchantUserCount).isEqualTo(1L);
    assertThat(adminUserCount).isEqualTo(1L);
    assertThat(merchantEntityCount).isEqualTo(1L);
  }

  @Test
  void bootstrapAllowsExistingUsernameWithDifferentPrimaryRoleByAddingBinding() {
    seedCoreRoles();
    PlatformUser admin = platformUserRepository.findByUsername("admin").orElseThrow();
    admin.setRole(PlatformUserRole.MERCHANT);
    admin.setMerchant(merchantRepository.findBySlug("demo-merchant").orElseThrow());
    platformUserRepository.save(admin);

    platformBootstrapService.run();

    long activeSystemAdminBindings =
        platformUserRbacBindingRepository.countActiveBindingsForContext(
            admin.getId(), "SYSTEM_ADMIN", null);
    assertThat(activeSystemAdminBindings).isEqualTo(1L);
  }

  @Test
  void bootstrapFailsFastWhenRoleCodeMissing() {
    RbacRole seeded = new RbacRole();
    seeded.setCode("CLIENT");
    rbacRoleRepository.saveAndFlush(seeded);

    assertThatThrownBy(() -> platformBootstrapService.run())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Bootstrap RBAC role missing");
  }

  private void seedCoreRoles() {
    ensureRole("SYSTEM_ADMIN");
    ensureRole("MERCHANT");
    ensureRole("CLIENT");
  }

  private void ensureRole(String code) {
    if (rbacRoleRepository.findByCode(code).isPresent()) {
      return;
    }
    RbacRole role = new RbacRole();
    role.setCode(code);
    rbacRoleRepository.save(role);
  }
}
