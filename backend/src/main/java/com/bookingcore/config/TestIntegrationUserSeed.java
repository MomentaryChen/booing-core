package com.bookingcore.config;

import com.bookingcore.api.ApiDtos.CreateMerchantRequest;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.MerchantMembership;
import com.bookingcore.modules.merchant.MerchantMembershipRepository;
import com.bookingcore.modules.merchant.MerchantMembershipStatus;
import com.bookingcore.modules.merchant.MerchantRepository;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.platform.PlatformUserRepository;
import com.bookingcore.security.PlatformUserRole;
import com.bookingcore.service.MerchantProvisioningService;
import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deterministic portal users for {@code test} profile (H2 create-drop, no Flyway). Keeps
 * {@code TestJwtHelper} integration tests reproducible without manual SQL.
 */
@Component
@Profile("test")
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class TestIntegrationUserSeed implements ApplicationRunner {

  public static final String INTEGRATION_MERCHANT_SLUG = "integration-test-merchant";
  public static final String MERCHANT_LOGIN_EMAIL = "merchant@example.com";
  public static final String MERCHANT_LOGIN_PASSWORD = "merchant";
  public static final String CLIENT_LOGIN_EMAIL = "client@example.com";
  public static final String CLIENT_LOGIN_PASSWORD = "client";

  private final MerchantRepository merchantRepository;
  private final MerchantProvisioningService merchantProvisioningService;
  private final PlatformUserRepository platformUserRepository;
  private final MerchantMembershipRepository merchantMembershipRepository;
  private final PasswordEncoder passwordEncoder;

  public TestIntegrationUserSeed(
      MerchantRepository merchantRepository,
      MerchantProvisioningService merchantProvisioningService,
      PlatformUserRepository platformUserRepository,
      MerchantMembershipRepository merchantMembershipRepository,
      PasswordEncoder passwordEncoder) {
    this.merchantRepository = merchantRepository;
    this.merchantProvisioningService = merchantProvisioningService;
    this.platformUserRepository = platformUserRepository;
    this.merchantMembershipRepository = merchantMembershipRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    Merchant merchant =
        merchantRepository
            .findBySlug(INTEGRATION_MERCHANT_SLUG)
            .orElseGet(
                () ->
                    merchantProvisioningService.createMerchant(
                        new CreateMerchantRequest("Integration Test Merchant", INTEGRATION_MERCHANT_SLUG)));

    ensureMerchantPortalUser(merchant);
    ensureClientUser();
  }

  private void ensureMerchantPortalUser(Merchant merchant) {
    String username = MERCHANT_LOGIN_EMAIL.toLowerCase(Locale.ROOT);
    PlatformUser user =
        platformUserRepository
            .findByUsernameIgnoreCase(username)
            .orElseGet(
                () -> {
                  PlatformUser u = new PlatformUser();
                  u.setUsername(username);
                  u.setPasswordHash(passwordEncoder.encode(MERCHANT_LOGIN_PASSWORD));
                  u.setRole(PlatformUserRole.MERCHANT);
                  u.setMerchant(merchant);
                  u.setEnabled(true);
                  return platformUserRepository.save(u);
                });

    if (user.getMerchant() == null || !merchant.getId().equals(user.getMerchant().getId())) {
      user.setMerchant(merchant);
      platformUserRepository.save(user);
    }

    if (merchantMembershipRepository.findByMerchantIdAndPlatformUserId(merchant.getId(), user.getId()).isEmpty()) {
      MerchantMembership m = new MerchantMembership();
      m.setMerchant(merchant);
      m.setPlatformUser(user);
      m.setMembershipStatus(MerchantMembershipStatus.ACTIVE);
      merchantMembershipRepository.save(m);
    }
  }

  private void ensureClientUser() {
    String username = CLIENT_LOGIN_EMAIL.toLowerCase(Locale.ROOT);
    if (platformUserRepository.findByUsernameIgnoreCase(username).isPresent()) {
      return;
    }
    PlatformUser u = new PlatformUser();
    u.setUsername(username);
    u.setPasswordHash(passwordEncoder.encode(CLIENT_LOGIN_PASSWORD));
    u.setRole(PlatformUserRole.CLIENT);
    u.setMerchant(null);
    u.setEnabled(true);
    platformUserRepository.save(u);
  }
}
