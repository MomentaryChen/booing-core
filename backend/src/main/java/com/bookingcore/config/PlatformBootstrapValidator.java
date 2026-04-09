package com.bookingcore.config;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

/**
 * Fail-fast checks for optional platform bootstrap. Credential logging is allowed only for the
 * {@code dev} profile.
 */
public final class PlatformBootstrapValidator {

  private PlatformBootstrapValidator() {}

  public static void validate(BookingPlatformProperties properties, Environment environment) {
    BookingPlatformProperties.Auth auth = properties.getAuth();
    if (auth.isLogDevBootstrapCredentials() && !environment.acceptsProfiles(Profiles.of("dev"))) {
      throw new IllegalStateException(
          "booking.platform.auth.log-dev-bootstrap-credentials is only allowed when spring profile "
              + "'dev' is active (refusing to print secrets in non-dev environments).");
    }

    boolean prod = environment.acceptsProfiles(Profiles.of("prod"));

    if (auth.getBootstrapDefaultMerchant().isEnabled()) {
      requireNonBlank(
          auth.getBootstrapDefaultMerchant().getSlug(), "booking.platform.auth.bootstrap-default-merchant.slug");
      requireNonBlank(
          auth.getBootstrapDefaultMerchant().getName(), "booking.platform.auth.bootstrap-default-merchant.name");
    }

    if (auth.getBootstrapDefaultMerchantUser().isEnabled()) {
      var u = auth.getBootstrapDefaultMerchantUser();
      requireNonBlank(u.getUsername(), "booking.platform.auth.bootstrap-default-merchant-user.username");
      requireNonBlank(u.getPassword(), "booking.platform.auth.bootstrap-default-merchant-user.password");
      boolean merchantBootstrapOn = auth.getBootstrapDefaultMerchant().isEnabled();
      if (!merchantBootstrapOn && !StringUtils.hasText(u.getMerchantSlug())) {
        throw new IllegalStateException(
            "When bootstrap-default-merchant is disabled, bootstrap-default-merchant-user.merchant-slug is required");
      }
      String slug =
          StringUtils.hasText(u.getMerchantSlug())
              ? u.getMerchantSlug()
              : auth.getBootstrapDefaultMerchant().getSlug();
      if (!StringUtils.hasText(slug)) {
        throw new IllegalStateException(
            "bootstrap-default-merchant-user is enabled but merchant slug is blank; set "
                + "bootstrap-default-merchant.slug or bootstrap-default-merchant-user.merchant-slug");
      }
      if (prod && looksLikeDevOnlyPassword(u.getPassword())) {
        throw new IllegalStateException(
            "bootstrap-default-merchant-user.password must not use dev-only literals in prod profile");
      }
    }

    if (auth.getBootstrapSystemAdmin().isEnabled()) {
      var a = auth.getBootstrapSystemAdmin();
      requireNonBlank(a.getUsername(), "booking.platform.auth.bootstrap-system-admin.username");
      requireNonBlank(a.getPassword(), "booking.platform.auth.bootstrap-system-admin.password");
      if (prod && looksLikeDevOnlyPassword(a.getPassword())) {
        throw new IllegalStateException(
            "bootstrap-system-admin.password must not use dev-only literals in prod profile");
      }
    }

    if (auth.getBootstrapDefaultClient().isEnabled()) {
      var c = auth.getBootstrapDefaultClient();
      requireNonBlank(c.getUsername(), "booking.platform.auth.bootstrap-default-client.username");
      requireNonBlank(c.getPassword(), "booking.platform.auth.bootstrap-default-client.password");
      if (prod && looksLikeDevOnlyPassword(c.getPassword())) {
        throw new IllegalStateException(
            "bootstrap-default-client.password must not use dev-only literals in prod profile");
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
