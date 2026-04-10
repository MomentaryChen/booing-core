package com.bookingcore.api;

import com.bookingcore.api.ApiDtos.NavigationResponse;
import com.bookingcore.security.PlatformUserRole;
import com.bookingcore.service.PlatformNavigationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/me")
public class MeController {

  private final PlatformNavigationService platformNavigationService;

  public MeController(PlatformNavigationService platformNavigationService) {
    this.platformNavigationService = platformNavigationService;
  }

  /**
   * Returns page/route keys allowed for the authenticated principal from DB.
   */
  @GetMapping("/navigation")
  @PreAuthorize("@permissionAuthorizer.hasPermission(authentication, 'me.navigation.read')")
  public NavigationResponse navigation() {
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
