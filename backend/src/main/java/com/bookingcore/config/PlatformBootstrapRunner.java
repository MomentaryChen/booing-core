package com.bookingcore.config;

import com.bookingcore.service.PlatformBootstrapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PlatformBootstrapRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(PlatformBootstrapRunner.class);

  private final BookingPlatformProperties properties;
  private final Environment environment;
  private final PlatformBootstrapService platformBootstrapService;

  public PlatformBootstrapRunner(
      BookingPlatformProperties properties,
      Environment environment,
      PlatformBootstrapService platformBootstrapService) {
    this.properties = properties;
    this.environment = environment;
    this.platformBootstrapService = platformBootstrapService;
  }

  @Override
  public void run(ApplicationArguments args) {
    PlatformBootstrapValidator.validate(properties, environment);
    if (!anyBootstrapEnabled()) {
      return;
    }
    platformBootstrapService.run();
    logConfiguredCredentialsIfDevOptIn();
  }

  private boolean anyBootstrapEnabled() {
    var a = properties.getAuth();
    return a.getBootstrapDefaultMerchant().isEnabled()
        || a.getBootstrapDefaultMerchantUser().isEnabled()
        || a.getBootstrapSystemAdmin().isEnabled()
        || a.getBootstrapDefaultClient().isEnabled();
  }

  private void logConfiguredCredentialsIfDevOptIn() {
    if (!environment.acceptsProfiles(Profiles.of("dev"))
        || !properties.getAuth().isLogDevBootstrapCredentials()) {
      return;
    }
    log.warn("DEV ONLY: printing bootstrap credentials from configuration (disable after debugging).");
    var a = properties.getAuth();
    if (a.getBootstrapDefaultMerchantUser().isEnabled()) {
      var u = a.getBootstrapDefaultMerchantUser();
      log.warn(
          "DEV DEFAULT MERCHANT USER => username='{}' password='{}' role='MERCHANT'",
          u.getUsername(),
          u.getPassword());
    }
    if (a.getBootstrapSystemAdmin().isEnabled()) {
      var s = a.getBootstrapSystemAdmin();
      log.warn(
          "DEV DEFAULT SYSTEM_ADMIN => username='{}' password='{}' role='SYSTEM_ADMIN'",
          s.getUsername(),
          s.getPassword());
    }
    if (a.getBootstrapDefaultClient().isEnabled()) {
      var c = a.getBootstrapDefaultClient();
      log.warn(
          "DEV DEFAULT CLIENT => username='{}' password='{}' role='CLIENT'",
          c.getUsername(),
          c.getPassword());
    }
  }
}
