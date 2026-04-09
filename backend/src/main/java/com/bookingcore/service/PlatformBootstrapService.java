package com.bookingcore.service;

import com.bookingcore.api.ApiDtos.CreateMerchantRequest;
import com.bookingcore.config.BookingPlatformProperties;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.MerchantRepository;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.platform.PlatformUserRepository;
import com.bookingcore.modules.platform.rbac.PlatformRbacBindingStatus;
import com.bookingcore.modules.platform.rbac.PlatformUserRbacBinding;
import com.bookingcore.modules.platform.rbac.PlatformUserRbacBindingRepository;
import com.bookingcore.modules.platform.rbac.RbacRoleRepository;
import com.bookingcore.security.PlatformUserRole;
import java.util.Locale;
import java.util.Optional;
import java.util.Objects;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PlatformBootstrapService {

  private final BookingPlatformProperties properties;
  private final MerchantRepository merchantRepository;
  private final MerchantProvisioningService merchantProvisioningService;
  private final PlatformUserRepository platformUserRepository;
  private final PasswordEncoder passwordEncoder;
  private final RbacRoleRepository rbacRoleRepository;
  private final PlatformUserRbacBindingRepository platformUserRbacBindingRepository;

  public PlatformBootstrapService(
      BookingPlatformProperties properties,
      MerchantRepository merchantRepository,
      MerchantProvisioningService merchantProvisioningService,
      PlatformUserRepository platformUserRepository,
      PasswordEncoder passwordEncoder,
      RbacRoleRepository rbacRoleRepository,
      PlatformUserRbacBindingRepository platformUserRbacBindingRepository) {
    this.properties = properties;
    this.merchantRepository = merchantRepository;
    this.merchantProvisioningService = merchantProvisioningService;
    this.platformUserRepository = platformUserRepository;
    this.passwordEncoder = passwordEncoder;
    this.rbacRoleRepository = rbacRoleRepository;
    this.platformUserRbacBindingRepository = platformUserRbacBindingRepository;
  }

  @Transactional
  public void run() {
    var auth = properties.getAuth();
    if (auth.getBootstrapDefaultMerchant().isEnabled()) {
      ensureDefaultMerchant(auth.getBootstrapDefaultMerchant());
    }
    if (auth.getBootstrapDefaultMerchantUser().isEnabled()) {
      String slug = resolveMerchantSlugForMerchantUser(auth);
      Merchant merchant =
          merchantRepository
              .findBySlug(slug)
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Bootstrap: merchant with slug '" + slug + "' not found; enable default-merchant bootstrap or create the merchant first."));
      ensurePlatformUser(
          PlatformUserRole.MERCHANT,
          auth.getBootstrapDefaultMerchantUser().getUsername(),
          auth.getBootstrapDefaultMerchantUser().getPassword(),
          merchant);
    }
    if (auth.getBootstrapSystemAdmin().isEnabled()) {
      ensurePlatformUser(
          PlatformUserRole.SYSTEM_ADMIN,
          auth.getBootstrapSystemAdmin().getUsername(),
          auth.getBootstrapSystemAdmin().getPassword(),
          null);
    }
    if (auth.getBootstrapDefaultClient().isEnabled()) {
      ensurePlatformUser(
          PlatformUserRole.CLIENT,
          auth.getBootstrapDefaultClient().getUsername(),
          auth.getBootstrapDefaultClient().getPassword(),
          null);
    }
  }

  private String resolveMerchantSlugForMerchantUser(BookingPlatformProperties.Auth auth) {
    var u = auth.getBootstrapDefaultMerchantUser();
    if (StringUtils.hasText(u.getMerchantSlug())) {
      return u.getMerchantSlug().trim().toLowerCase(Locale.ROOT);
    }
    return auth.getBootstrapDefaultMerchant().getSlug().trim().toLowerCase(Locale.ROOT);
  }

  private Merchant ensureDefaultMerchant(BookingPlatformProperties.Auth.BootstrapDefaultMerchant cfg) {
    String slug = cfg.getSlug().trim().toLowerCase(Locale.ROOT);
    Optional<Merchant> existing = merchantRepository.findBySlug(slug);
    if (existing.isPresent()) {
      return existing.get();
    }
    return merchantProvisioningService.createMerchant(new CreateMerchantRequest(cfg.getName().trim(), slug));
  }

  private void ensurePlatformUser(PlatformUserRole role, String username, String password, Merchant merchant) {
    Optional<PlatformUser> existing = platformUserRepository.findByUsername(username);
    if (existing.isPresent()) {
      PlatformUser user = existing.get();
      validateExistingUserMapping(user, role, merchant);
      ensureRbacBinding(user, role, merchant);
      return;
    }
    PlatformUser user = new PlatformUser();
    user.setUsername(username);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setRole(role);
    user.setEnabled(true);
    user.setMerchant(
        role == PlatformUserRole.MERCHANT || role == PlatformUserRole.SUB_MERCHANT ? merchant : null);
    platformUserRepository.save(user);
    ensureRbacBinding(user, role, merchant);
  }

  private void validateExistingUserMapping(PlatformUser user, PlatformUserRole expectedRole, Merchant expectedMerchant) {
    if (user.getRole() != expectedRole) {
      throw new IllegalStateException(
          "Bootstrap user conflict: username '"
              + user.getUsername()
              + "' already exists with role="
              + user.getRole().name()
              + ", expected role="
              + expectedRole.name());
    }
    Long expectedMerchantId =
        (expectedRole == PlatformUserRole.MERCHANT || expectedRole == PlatformUserRole.SUB_MERCHANT)
            ? (expectedMerchant == null ? null : expectedMerchant.getId())
            : null;
    Long actualMerchantId = user.getMerchant() == null ? null : user.getMerchant().getId();
    if (!Objects.equals(actualMerchantId, expectedMerchantId)) {
      throw new IllegalStateException(
          "Bootstrap user conflict: username '"
              + user.getUsername()
              + "' merchant mismatch (actual="
              + actualMerchantId
              + ", expected="
              + expectedMerchantId
              + ")");
    }
  }

  private void ensureRbacBinding(PlatformUser user, PlatformUserRole role, Merchant merchant) {
    if (rbacRoleRepository.count() == 0L) {
      // Legacy/test compatibility: RBAC catalog not initialized yet, so skip binding creation.
      return;
    }
    var rbacRole =
        rbacRoleRepository
            .findByCode(role.name())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Bootstrap RBAC role missing for code '" + role.name() + "'"));
    Long merchantId = merchant == null ? null : merchant.getId();
    var existing =
        platformUserRbacBindingRepository.findBindingsForUserContext(
            user.getId(), role.name(), merchantId);
    if (existing.stream()
        .anyMatch(b -> b.getStatus() == PlatformRbacBindingStatus.ACTIVE)) {
      return;
    }
    Optional<PlatformUserRbacBinding> reusable =
        existing.stream()
            .filter(b -> b.getStatus() != PlatformRbacBindingStatus.ACTIVE)
            .findFirst();
    if (reusable.isPresent()) {
      PlatformUserRbacBinding binding = reusable.get();
      binding.setStatus(PlatformRbacBindingStatus.ACTIVE);
      if (!Objects.equals(binding.getRbacRole().getId(), rbacRole.getId())) {
        binding.setRbacRole(rbacRole);
      }
      if (!Objects.equals(
          binding.getMerchant() == null ? null : binding.getMerchant().getId(),
          merchantId)) {
        binding.setMerchant(merchant);
      }
      platformUserRbacBindingRepository.save(binding);
      return;
    }
    PlatformUserRbacBinding binding = new PlatformUserRbacBinding();
    binding.setPlatformUser(user);
    binding.setRbacRole(rbacRole);
    binding.setMerchant(merchant);
    binding.setStatus(PlatformRbacBindingStatus.ACTIVE);
    platformUserRbacBindingRepository.save(binding);
  }
}
