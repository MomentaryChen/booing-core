package com.bookingcore.service;

import com.bookingcore.api.ApiDtos.NavigationItem;
import com.bookingcore.api.ApiDtos.NavigationResponse;
import com.bookingcore.modules.platform.PlatformPage;
import com.bookingcore.modules.platform.PlatformPageRepository;
import com.bookingcore.modules.platform.RolePageGrantRepository;
import com.bookingcore.security.PlatformUserRole;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformNavigationService {

  private final PlatformPageRepository platformPageRepository;
  private final RolePageGrantRepository rolePageGrantRepository;

  public PlatformNavigationService(
      PlatformPageRepository platformPageRepository,
      RolePageGrantRepository rolePageGrantRepository) {
    this.platformPageRepository = platformPageRepository;
    this.rolePageGrantRepository = rolePageGrantRepository;
  }

  @Transactional(readOnly = true)
  public NavigationResponse navigationFor(PlatformUserRole role) {
    List<PlatformPage> pages = rolePageGrantRepository.findPagesForRole(role);
    return toResponse(pages);
  }

  /** When JWT is off: expose all active pages so local UI can mirror production rules from DB. */
  @Transactional(readOnly = true)
  public NavigationResponse allActivePages() {
    List<PlatformPage> pages = platformPageRepository.findByActiveTrueOrderBySortOrderAsc();
    return toResponse(pages);
  }

  private static NavigationResponse toResponse(List<PlatformPage> pages) {
    List<String> routeKeys = pages.stream().map(PlatformPage::getRouteKey).toList();
    List<NavigationItem> items =
        pages.stream()
            .map(
                p ->
                    new NavigationItem(
                        p.getRouteKey(),
                        p.getFrontendPath(),
                        p.getLabelKey(),
                        p.getSortOrder()))
            .toList();
    return new NavigationResponse(routeKeys, items);
  }
}
