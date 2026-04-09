package com.bookingcore.service;

import com.bookingcore.api.ApiDtos.AuthContextOption;
import com.bookingcore.api.ApiDtos.AuthMeResponse;
import com.bookingcore.api.ApiDtos.ContextSelectRequest;
import com.bookingcore.api.ApiDtos.LoginRequest;
import com.bookingcore.api.ApiDtos.TokenResponse;
import com.bookingcore.common.ApiException;
import com.bookingcore.config.BookingPlatformProperties;
import com.bookingcore.config.BookingPlatformProperties.DevUser;
import com.bookingcore.modules.merchant.MerchantRepository;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.platform.PlatformUserRepository;
import com.bookingcore.security.JwtService;
import com.bookingcore.security.EffectivePermissionService;
import com.bookingcore.security.PlatformPrincipal;
import com.bookingcore.security.PlatformUserRole;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final BookingPlatformProperties properties;
  private final JwtService jwtService;
  private final MerchantRepository merchantRepository;
  private final PlatformUserRepository platformUserRepository;
  private final PasswordEncoder passwordEncoder;
  private final EffectivePermissionService effectivePermissionService;
  private final Environment environment;

  public AuthService(
      BookingPlatformProperties properties,
      JwtService jwtService,
      MerchantRepository merchantRepository,
      PlatformUserRepository platformUserRepository,
      PasswordEncoder passwordEncoder,
      EffectivePermissionService effectivePermissionService,
      Environment environment) {
    this.properties = properties;
    this.jwtService = jwtService;
    this.merchantRepository = merchantRepository;
    this.platformUserRepository = platformUserRepository;
    this.passwordEncoder = passwordEncoder;
    this.effectivePermissionService = effectivePermissionService;
    this.environment = environment;
  }

  public TokenResponse login(LoginRequest request) {
    if (!StringUtils.hasText(properties.getJwt().getSecret())) {
      throw new ApiException("JWT auth is not enabled (set booking.platform.jwt.secret)", HttpStatus.BAD_REQUEST);
    }

    PlatformUser dbUser = platformUserRepository.findByUsername(request.username()).orElse(null);
    if (dbUser != null) {
      clearExpiredLockout(dbUser);
      if (isLockedOut(dbUser)) {
        log.warn("auth_login_failed reason=locked_out username={}", request.username());
        throw new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED);
      }
      if (!Boolean.TRUE.equals(dbUser.getEnabled())) {
        log.warn("auth_login_failed reason=disabled_user username={}", request.username());
        throw new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED);
      }
      if (passwordEncoder.matches(request.password(), dbUser.getPasswordHash())) {
        resetLoginFailureState(dbUser);
        Long merchantId = dbUser.getMerchant() == null ? null : dbUser.getMerchant().getId();
        String token =
            jwtService.createAccessToken(
                dbUser.getUsername(), dbUser.getRole(), merchantId, dbUser.getCredentialVersion());
        dbUser.setLastLoginAt(LocalDateTime.now());
        platformUserRepository.save(dbUser);
        return buildTokenResponse(
            token, new PlatformPrincipal(dbUser.getUsername(), dbUser.getRole(), merchantId));
      }
      recordFailedPasswordAttempt(dbUser);
      log.warn("auth_login_failed reason=wrong_password username={}", request.username());
      throw new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED);
    }

    // Backward-compatible fallback for config-driven dev accounts.
    if (environment.acceptsProfiles(Profiles.of("dev"))) {
      for (DevUser u : properties.getDevUsers()) {
        if (request.username().equals(u.getUsername()) && request.password().equals(u.getPassword())) {
          Long merchantId = resolveMerchantId(u);
          String token = jwtService.createAccessToken(u.getUsername(), u.getRole(), merchantId);
          return buildTokenResponse(token, new PlatformPrincipal(u.getUsername(), u.getRole(), merchantId));
        }
      }
    }
    log.warn("auth_login_failed reason=user_not_found username={}", request.username());
    throw new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED);
  }

  private void clearExpiredLockout(PlatformUser user) {
    var login = properties.getAuth().getLogin();
    if (!login.isLockoutEnabled()) {
      return;
    }
    LocalDateTime until = user.getLockedUntil();
    if (until != null && !until.isAfter(LocalDateTime.now())) {
      resetLoginFailureState(user);
      platformUserRepository.save(user);
    }
  }

  private boolean isLockedOut(PlatformUser user) {
    var login = properties.getAuth().getLogin();
    if (!login.isLockoutEnabled()) {
      return false;
    }
    LocalDateTime until = user.getLockedUntil();
    return until != null && until.isAfter(LocalDateTime.now());
  }

  private void resetLoginFailureState(PlatformUser user) {
    user.setFailedLoginCount(0);
    user.setFailedLoginWindowStartedAt(null);
    user.setLockedUntil(null);
  }

  private void recordFailedPasswordAttempt(PlatformUser user) {
    var login = properties.getAuth().getLogin();
    if (!login.isLockoutEnabled()) {
      return;
    }
    LocalDateTime now = LocalDateTime.now();
    int windowMin = Math.max(1, login.getFailureWindowMinutes());
    LocalDateTime windowStart = user.getFailedLoginWindowStartedAt();
    if (windowStart == null || windowStart.plusMinutes(windowMin).isBefore(now)) {
      user.setFailedLoginCount(1);
      user.setFailedLoginWindowStartedAt(now);
    } else {
      user.setFailedLoginCount(user.getFailedLoginCount() + 1);
    }
    int max = Math.max(1, login.getMaxFailedAttemptsPerUser());
    if (user.getFailedLoginCount() >= max) {
      int lockMin = Math.max(1, login.getLockoutDurationMinutes());
      user.setLockedUntil(now.plusMinutes(lockMin));
    }
    platformUserRepository.save(user);
  }

  private Long resolveMerchantId(DevUser user) {
    if (user.getRole() != PlatformUserRole.MERCHANT && user.getRole() != PlatformUserRole.SUB_MERCHANT) {
      return null;
    }
    if (user.getMerchantId() != null) {
      return user.getMerchantId();
    }
    return merchantRepository.findFirstByOrderByIdAsc()
        .map(m -> m.getId())
        .orElseThrow(() -> new ApiException(
            "No merchant is configured yet. Please register a merchant first (/merchant/register).",
            HttpStatus.BAD_REQUEST));
  }

  private TokenResponse buildTokenResponse(String token, PlatformPrincipal principal) {
    PlatformUserRole role = principal.role();
    List<String> roleNames = List.of(role.name());
    List<String> permissions = effectivePermissionService.sortedPermissionCodesFor(principal);
    return new TokenResponse(
        token,
        "Bearer",
        properties.getJwt().getExpirationSeconds(),
        role.name(),
        roleNames,
        permissions);
  }

  public AuthMeResponse me(Authentication authentication) {
    requireJwtEnabled();
    PlatformPrincipal principal = requirePlatformPrincipal(authentication);
    PlatformUserRole role = principal.role();
    List<String> perms = effectivePermissionService.sortedPermissionCodesFor(principal);
    List<AuthContextOption> available = buildAvailableContexts(principal);
    AuthContextOption active =
        available.stream()
            .filter(
                o ->
                    Objects.equals(o.role(), role.name())
                        && Objects.equals(o.merchantId(), principal.merchantId()))
            .findFirst()
            .orElse(available.isEmpty() ? null : available.get(0));
    String sessionState = available.isEmpty() ? "AUTHENTICATED" : "CONTEXT_SET";
    return new AuthMeResponse(
        principal.username(),
        role.name(),
        List.of(role.name()),
        perms,
        principal.merchantId(),
        sessionState,
        available,
        active);
  }

  public TokenResponse selectContext(Authentication authentication, ContextSelectRequest request) {
    requireJwtEnabled();
    PlatformPrincipal principal = requirePlatformPrincipal(authentication);
    if (request == null || !StringUtils.hasText(request.role())) {
      return refresh(authentication);
    }
    PlatformUserRole requestedRole;
    try {
      requestedRole = PlatformUserRole.valueOf(request.role().trim());
    } catch (IllegalArgumentException ex) {
      throw new ApiException("Invalid role", HttpStatus.BAD_REQUEST);
    }
    List<AuthContextOption> allowed = buildAvailableContexts(principal);
    AuthContextOption match =
        allowed.stream()
            .filter(
                o ->
                    Objects.equals(o.role(), requestedRole.name())
                        && Objects.equals(o.merchantId(), request.merchantId()))
            .findFirst()
            .orElse(null);
    if (match == null) {
      throw new ApiException("Context not allowed for this principal", HttpStatus.FORBIDDEN);
    }
    String token =
        jwtService.createAccessToken(
            principal.username(),
            requestedRole,
            request.merchantId(),
            credentialVersionForSubject(principal.username()));
    return buildTokenResponse(
        token, new PlatformPrincipal(principal.username(), requestedRole, request.merchantId()));
  }

  private List<AuthContextOption> buildAvailableContexts(PlatformPrincipal principal) {
    List<AuthContextOption> out = new ArrayList<>();
    out.add(new AuthContextOption("PLATFORM", principal.merchantId(), principal.role().name()));
    if (principal.role() == PlatformUserRole.SYSTEM_ADMIN) {
      merchantRepository
          .findFirstByOrderByIdAsc()
          .ifPresent(
              m ->
                  out.add(
                      new AuthContextOption(
                          "MERCHANT_SCOPED", m.getId(), PlatformUserRole.MERCHANT.name())));
    }
    return List.copyOf(out);
  }

  public TokenResponse refresh(Authentication authentication) {
    requireJwtEnabled();
    PlatformPrincipal principal = requirePlatformPrincipal(authentication);
    String token =
        jwtService.createAccessToken(
            principal.username(),
            principal.role(),
            principal.merchantId(),
            credentialVersionForSubject(principal.username()));
    return buildTokenResponse(token, principal);
  }

  /**
   * Bumps {@link PlatformUser#getCredentialVersion()} so outstanding access JWTs for this user fail
   * validation (stateless revocation).
   */
  public void logout(Authentication authentication) {
    if (!StringUtils.hasText(properties.getJwt().getSecret())) {
      return;
    }
    if (authentication == null || !authentication.isAuthenticated()) {
      return;
    }
    Object p = authentication.getPrincipal();
    if (!(p instanceof PlatformPrincipal principal)) {
      return;
    }
    platformUserRepository
        .findByUsername(principal.username())
        .ifPresent(
            u -> {
              u.setCredentialVersion(u.getCredentialVersion() + 1);
              platformUserRepository.save(u);
            });
  }

  private Integer credentialVersionForSubject(String username) {
    return platformUserRepository.findByUsername(username).map(PlatformUser::getCredentialVersion).orElse(null);
  }

  private void requireJwtEnabled() {
    if (!StringUtils.hasText(properties.getJwt().getSecret())) {
      throw new ApiException("JWT auth is not enabled", HttpStatus.BAD_REQUEST);
    }
  }

  private static PlatformPrincipal requirePlatformPrincipal(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
      throw new ApiException("Unauthorized", HttpStatus.UNAUTHORIZED);
    }
    Object p = authentication.getPrincipal();
    if (!(p instanceof PlatformPrincipal principal)) {
      throw new ApiException("Unsupported principal type", HttpStatus.FORBIDDEN);
    }
    return principal;
  }
}
