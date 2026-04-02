package com.bookingcore.config;

import com.bookingcore.api.ApiDtos.CreateMerchantRequest;
import com.bookingcore.config.BookingPlatformProperties.DevUser;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.MerchantRepository;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.platform.PlatformUserRepository;
import com.bookingcore.security.PlatformUserRole;
import com.bookingcore.service.MerchantProvisioningService;
import java.util.Locale;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
@Profile("dev")
public class DevBootstrap {

  private final BookingPlatformProperties properties;
  private final MerchantProvisioningService merchantProvisioningService;
  private final MerchantRepository merchantRepository;
  private final PlatformUserRepository platformUserRepository;
  private final PasswordEncoder passwordEncoder;

  public DevBootstrap(
      BookingPlatformProperties properties,
      MerchantProvisioningService merchantProvisioningService,
      MerchantRepository merchantRepository,
      PlatformUserRepository platformUserRepository,
      PasswordEncoder passwordEncoder) {
    this.properties = properties;
    this.merchantProvisioningService = merchantProvisioningService;
    this.merchantRepository = merchantRepository;
    this.platformUserRepository = platformUserRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void ensureDevMerchantExists() {
    boolean needsMerchant =
        properties.getDevUsers().stream().anyMatch(u ->
            u.getRole() == PlatformUserRole.MERCHANT || u.getRole() == PlatformUserRole.SUB_MERCHANT);

    if (!needsMerchant) {
      return;
    }

    // AuthService resolves merchantId from the first merchant when dev user does not specify merchantId.
    try {
      merchantProvisioningService.createMerchant(new CreateMerchantRequest(
          "Demo Merchant",
          "demo-merchant".toLowerCase(Locale.ROOT)
      ));
    } catch (Exception ignored) {
      // best-effort bootstrap: avoid failing startup if already seeded
    }
  }

  @EventListener(ApplicationReadyEvent.class)
  public void seedDevUsersIntoDb() {
    for (DevUser u : properties.getDevUsers()) {
      PlatformUser user = platformUserRepository.findByUsername(u.getUsername()).orElseGet(PlatformUser::new);
      user.setUsername(u.getUsername());
      user.setPasswordHash(passwordEncoder.encode(u.getPassword()));
      user.setRole(u.getRole());
      user.setEnabled(true);

      if (u.getRole() == PlatformUserRole.MERCHANT || u.getRole() == PlatformUserRole.SUB_MERCHANT) {
        Merchant merchant = null;
        if (u.getMerchantId() != null) {
          merchant = merchantRepository.findById(u.getMerchantId()).orElse(null);
        }
        if (merchant == null) {
          merchant = merchantRepository.findFirstByOrderByIdAsc().orElse(null);
        }
        user.setMerchant(merchant);
      } else {
        user.setMerchant(null);
      }

      platformUserRepository.save(user);
    }
  }
}

