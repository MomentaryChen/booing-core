package com.bookingcore.config;

import com.bookingcore.modules.platform.PlatformPage;
import com.bookingcore.modules.platform.PlatformPageRepository;
import com.bookingcore.modules.platform.RolePageGrant;
import com.bookingcore.modules.platform.RolePageGrantRepository;
import com.bookingcore.security.PlatformUserRole;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PlatformNavigationInitializer implements ApplicationRunner {

  private final PlatformPageRepository platformPageRepository;
  private final RolePageGrantRepository rolePageGrantRepository;

  public PlatformNavigationInitializer(
      PlatformPageRepository platformPageRepository,
      RolePageGrantRepository rolePageGrantRepository) {
    this.platformPageRepository = platformPageRepository;
    this.rolePageGrantRepository = rolePageGrantRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (platformPageRepository.count() > 0) {
      return;
    }

    PlatformPage pSystem = page("nav.system.dashboard", "/system", "navAdmin", 10);
    PlatformPage pMerchant = page("nav.merchant.dashboard", "/merchant", "navMerchant", 20);
    PlatformPage pAppointments = page("nav.merchant.appointments", "/merchant/appointments", "navMerchantAppointments", 30);
    PlatformPage pSchedule = page("nav.merchant.schedule", "/merchant/settings/schedule", "navMerchantSchedule", 40);
    PlatformPage pClient = page("nav.client.todo", "/client", "navClient", 50);
    PlatformPage pStore = page("nav.store.public", "/client/booking/demo-merchant", "navStore", 60);

    platformPageRepository.save(pSystem);
    platformPageRepository.save(pMerchant);
    platformPageRepository.save(pAppointments);
    platformPageRepository.save(pSchedule);
    platformPageRepository.save(pClient);
    platformPageRepository.save(pStore);

    grantAll(PlatformUserRole.SYSTEM_ADMIN, pSystem, pMerchant, pAppointments, pSchedule, pClient, pStore);
    grantAll(PlatformUserRole.MERCHANT, pMerchant, pAppointments, pSchedule, pStore);
    grantAll(PlatformUserRole.SUB_MERCHANT, pMerchant, pAppointments, pSchedule, pStore);
  }

  private static PlatformPage page(String routeKey, String path, String labelKey, int sortOrder) {
    PlatformPage p = new PlatformPage();
    p.setRouteKey(routeKey);
    p.setFrontendPath(path);
    p.setLabelKey(labelKey);
    p.setSortOrder(sortOrder);
    p.setActive(true);
    return p;
  }

  private void grantAll(PlatformUserRole role, PlatformPage... pages) {
    for (PlatformPage page : pages) {
      RolePageGrant g = new RolePageGrant();
      g.setRole(role);
      g.setPage(page);
      rolePageGrantRepository.save(g);
    }
  }
}
