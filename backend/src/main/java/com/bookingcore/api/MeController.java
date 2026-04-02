package com.bookingcore.api;

import com.bookingcore.api.ApiDtos.NavigationResponse;
import com.bookingcore.config.BookingPlatformProperties;
import com.bookingcore.security.PlatformUserRole;
import com.bookingcore.service.PlatformNavigationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/me")
public class MeController {

  private final BookingPlatformProperties properties;
  private final PlatformNavigationService platformNavigationService;

  public MeController(
      BookingPlatformProperties properties, PlatformNavigationService platformNavigationService) {
    this.properties = properties;
    this.platformNavigationService = platformNavigationService;
  }

  /**
   * Returns page/route keys allowed for the authenticated principal from DB. When JWT is disabled,
   * returns all active pages (local dev: same catalog as production DB rules).
   */
  @GetMapping("/navigation")
  public NavigationResponse navigation() {
    if (!StringUtils.hasText(properties.getJwt().getSecret())) {
      return platformNavigationService.allActivePages();
    }
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null
        || !auth.isAuthenticated()
        || "anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    PlatformUserRole role = resolveRole(auth);
    return platformNavigationService.navigationFor(role);
  }

  private static PlatformUserRole resolveRole(Authentication auth) {
    return auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(a -> a.startsWith("ROLE_"))
        .map(a -> PlatformUserRole.valueOf(a.substring(5)))
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
  }
}
