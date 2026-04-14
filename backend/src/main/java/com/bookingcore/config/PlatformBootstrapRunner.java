package com.bookingcore.config;

import com.bookingcore.service.PlatformBootstrapService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PlatformBootstrapRunner implements ApplicationRunner {

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
    platformBootstrapService.provisionInternalSystemAdminIfAbsent();
  }
}
