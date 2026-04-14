package com.bookingcore.config;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

/** Fail-fast checks for platform startup provisioning. */
public final class PlatformBootstrapValidator {

  private PlatformBootstrapValidator() {}

  public static void validate(BookingPlatformProperties properties, Environment environment) {
    BookingPlatformProperties.Auth auth = properties.getAuth();

    if (auth.getInternalSystemAdmin().isAutoProvision()) {
      requireNonBlank(
          auth.getInternalSystemAdmin().getUsername(),
          "booking.platform.auth.internal-system-admin.username");
      requireNonBlank(
          auth.getInternalSystemAdmin().getPassword(),
          "booking.platform.auth.internal-system-admin.password");
      if (environment.acceptsProfiles(Profiles.of("prod"))
          && looksLikeDevOnlyPassword(auth.getInternalSystemAdmin().getPassword())) {
        throw new IllegalStateException(
            "internal-system-admin.password must not use dev-only literals in prod profile");
      }
    }
  }

  private static void requireNonBlank(String value, String key) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalStateException("Missing required configuration: " + key);
    }
  }

  /** Blocks obvious demo literals when profile {@code prod} is active. */
  private static boolean looksLikeDevOnlyPassword(String password) {
    String p = password.trim().toLowerCase();
    return p.equals("admin") || p.equals("merchant") || p.equals("client");
  }
}
