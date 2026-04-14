package com.bookingcore.service;

import com.bookingcore.api.ApiDtos.AuthContextOption;
import com.bookingcore.api.ApiDtos.CreateMerchantRequest;
import com.bookingcore.api.ApiDtos.AuthMeResponse;
import com.bookingcore.api.ApiDtos.ContextSelectRequest;
import com.bookingcore.api.ApiDtos.LoginRequest;
import com.bookingcore.api.ApiDtos.MerchantEnableRequest;
import com.bookingcore.api.ApiDtos.MerchantEnableResponse;
import com.bookingcore.api.ApiDtos.TokenResponse;
import com.bookingcore.common.ApiException;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.MerchantMembership;
import com.bookingcore.modules.merchant.MerchantMembershipRepository;
import com.bookingcore.modules.merchant.MerchantMembershipStatus;
import com.bookingcore.config.BookingPlatformProperties;
import com.bookingcore.modules.merchant.MerchantRepository;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.platform.PlatformUserRepository;
import com.bookingcore.modules.platform.rbac.ActiveAuthContext;
import com.bookingcore.modules.platform.rbac.PlatformRbacBindingStatus;
import com.bookingcore.modules.platform.rbac.PlatformUserRbacBinding;
import com.bookingcore.modules.platform.rbac.PlatformUserRbacBindingRepository;
import com.bookingcore.modules.platform.rbac.RbacRole;
import com.bookingcore.modules.platform.rbac.RbacRoleRepository;
import com.bookingcore.security.JwtService;
import com.bookingcore.security.EffectivePermissionService;
import com.bookingcore.security.PlatformPrincipal;
import com.bookingcore.security.PlatformUserRole;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);
  private static final Pattern SIMPLE_EMAIL_PATTERN =
      Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

  private final BookingPlatformProperties properties;
  private final JwtService jwtService;
  private final MerchantRepository merchantRepository;
  private final PlatformUserRepository platformUserRepository;
  private final PasswordEncoder passwordEncoder;
  private final EffectivePermissionService effectivePermissionService;
  private final RbacRoleRepository rbacRoleRepository;
  private final PlatformUserRbacBindingRepository platformUserRbacBindingRepository;
  private final MerchantMembershipRepository merchantMembershipRepository;
  private final MerchantProvisioningService merchantProvisioningService;
  private final PlatformAuditService platformAuditService;

  private static final Comparator<ActiveAuthContext> ACTIVE_CONTEXT_ORDER =
      Comparator.comparingInt((ActiveAuthContext c) -> safeRoleOrdinal(c.roleCode()))
          .thenComparing(c -> c.merchantId() == null ? Long.MIN_VALUE : c.merchantId());

  public AuthService(
      BookingPlatformProperties properties,
      JwtService jwtService,
      MerchantRepository merchantRepository,
      PlatformUserRepository platformUserRepository,
      PasswordEncoder passwordEncoder,
      EffectivePermissionService effectivePermissionService,
      RbacRoleRepository rbacRoleRepository,
      PlatformUserRbacBindingRepository platformUserRbacBindingRepository,
      MerchantMembershipRepository merchantMembershipRepository,
      MerchantProvisioningService merchantProvisioningService,
      PlatformAuditService platformAuditService) {
    this.properties = properties;
    this.jwtService = jwtService;
    this.merchantRepository = merchantRepository;
    this.platformUserRepository = platformUserRepository;
    this.passwordEncoder = passwordEncoder;
    this.effectivePermissionService = effectivePermissionService;
    this.rbacRoleRepository = rbacRoleRepository;
    this.platformUserRbacBindingRepository = platformUserRbacBindingRepository;
    this.merchantMembershipRepository = merchantMembershipRepository;
    this.merchantProvisioningService = merchantProvisioningService;
    this.platformAuditService = platformAuditService;
  }

  public TokenResponse login(LoginRequest request) {
    if (!StringUtils.hasText(properties.getJwt().getSecret())) {
      throw new ApiException("JWT auth is not enabled (set booking.platform.jwt.secret)", HttpStatus.BAD_REQUEST);
    }

    String loginId = request.username() == null ? "" : request.username().trim();
    PlatformUser dbUser = resolveUserByLoginId(loginId);
    if (dbUser != null) {
      if (!isLoginIdentifierAllowedForRole(loginId, dbUser.getRole())) {
        log.warn("auth_login_failed reason=identifier_policy loginId={}", loginId);
        throw new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED);
      }
      clearExpiredLockout(dbUser);
      if (isLockedOut(dbUser)) {
        log.warn("auth_login_failed reason=locked_out username={}", loginId);
        throw new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED);
      }
      if (!Boolean.TRUE.equals(dbUser.getEnabled())) {
        log.warn("auth_login_failed reason=disabled_user username={}", loginId);
        throw new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED);
      }
      if (passwordEncoder.matches(request.password(), dbUser.getPasswordHash())) {
        resetLoginFailureState(dbUser);
        ActiveAuthContext initial = resolveInitialContext(dbUser);
        PlatformUserRole role = parseRoleCode(initial.roleCode()).orElse(dbUser.getRole());
        Long merchantId = initial.merchantId();
        String token =
            jwtService.createAccessToken(
                dbUser.getUsername(), role, merchantId, dbUser.getCredentialVersion());
        dbUser.setLastLoginAt(LocalDateTime.now());
        platformUserRepository.save(dbUser);
        return buildTokenResponse(
            token, new PlatformPrincipal(dbUser.getUsername(), role, merchantId));
      }
      recordFailedPasswordAttempt(dbUser);
      log.warn("auth_login_failed reason=wrong_password username={}", loginId);
      throw new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED);
    }

    log.warn("auth_login_failed reason=user_not_found username={}", loginId);
    throw new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED);
  }

  private PlatformUser resolveUserByLoginId(String loginId) {
    if (!StringUtils.hasText(loginId)) {
      return null;
    }
    if (looksLikeEmail(loginId)) {
      String normalized = loginId.toLowerCase(Locale.ROOT);
      return platformUserRepository.findByUsernameIgnoreCase(normalized).orElse(null);
    }
    return platformUserRepository.findByUsername(loginId).orElse(null);
  }

  private boolean isLoginIdentifierAllowedForRole(String loginId, PlatformUserRole role) {
    if (role == PlatformUserRole.SYSTEM_ADMIN) {
      return StringUtils.hasText(loginId);
    }
    return looksLikeEmail(loginId);
  }

  private static boolean looksLikeEmail(String value) {
    if (!StringUtils.hasText(value)) {
      return false;
    }
    return SIMPLE_EMAIL_PATTERN.matcher(value.trim()).matches();
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

  private TokenResponse buildTokenResponse(String token, PlatformPrincipal principal) {
    PlatformUserRole role = principal.role();
    List<String> roleNames = resolveAllRoleCodes(principal.username(), principal.role());
    List<String> canonicalRoleNames = roleNames.stream().map(this::toCanonicalRoleCode).distinct().toList();
    List<String> roleAliases = resolveRoleAliases(roleNames);
    List<String> permissions = effectivePermissionService.sortedPermissionCodesFor(principal);
    return new TokenResponse(
        token,
        "Bearer",
        properties.getJwt().getExpirationSeconds(),
        role.name(),
        roleNames,
        role.canonicalCode(),
        canonicalRoleNames,
        roleAliases,
        permissions);
  }

  public AuthMeResponse me(Authentication authentication) {
    requireJwtEnabled();
    PlatformPrincipal principal = requirePlatformPrincipal(authentication);
    PlatformUserRole role = principal.role();
    List<String> perms = effectivePermissionService.sortedPermissionCodesFor(principal);
    List<String> allRoles = resolveAllRoleCodes(principal.username(), role);
    List<String> canonicalRoles = allRoles.stream().map(this::toCanonicalRoleCode).distinct().toList();
    List<String> roleAliases = resolveRoleAliases(allRoles);
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
        allRoles,
        role.canonicalCode(),
        canonicalRoles,
        roleAliases,
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
      requestedRole =
          PlatformUserRole.parse(request.role().trim())
              .orElseThrow(IllegalArgumentException::new);
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
    assertContextMembershipAllowed(principal.username(), requestedRole, request.merchantId());
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
    var userOpt = platformUserRepository.findByUsername(principal.username());
    if (userOpt.isEmpty()) {
      return buildContextsWithoutDbUser(principal);
    }
    PlatformUser user = userOpt.get();
    if (rbacRoleRepository.count() == 0L) {
      List<AuthContextOption> single =
          List.of(authOptionForRoleAndMerchant(principal.role(), principal.merchantId()));
      return appendAdminMerchantPreview(single, principal);
    }
    List<ActiveAuthContext> raw = platformUserRbacBindingRepository.findActiveAuthContexts(user.getId());
    List<AuthContextOption> fromBindings;
    if (raw.isEmpty()) {
      Long mid = user.getMerchant() == null ? null : user.getMerchant().getId();
      fromBindings = List.of(authOptionForRoleAndMerchant(user.getRole(), mid));
    } else {
      fromBindings =
          dedupeActiveContexts(raw).stream()
              .filter(c -> parseRoleCode(c.roleCode()).isPresent())
              .filter(
                  c ->
                      isContextAllowedByMembership(
                          user, parseRoleCode(c.roleCode()).orElseThrow(), c.merchantId()))
              .sorted(ACTIVE_CONTEXT_ORDER)
              .map(c -> authOptionForRoleAndMerchant(parseRoleCode(c.roleCode()).orElseThrow(), c.merchantId()))
              .toList();
      if (fromBindings.isEmpty()) {
        Long mid = user.getMerchant() == null ? null : user.getMerchant().getId();
        fromBindings = List.of(authOptionForRoleAndMerchant(user.getRole(), mid));
      }
    }
    return appendAdminMerchantPreview(fromBindings, principal);
  }

  @Transactional
  public MerchantEnableResponse enableMerchant(
      Authentication authentication, MerchantEnableRequest request) {
    requireJwtEnabled();
    PlatformPrincipal principal = requirePlatformPrincipal(authentication);
    PlatformUser user =
        platformUserRepository
            .findByUsername(principal.username())
            .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    Merchant merchant =
        merchantProvisioningService.createMerchant(new CreateMerchantRequest(request.name(), request.slug()));

    MerchantMembership membership = new MerchantMembership();
    membership.setMerchant(merchant);
    membership.setPlatformUser(user);
    membership.setMembershipStatus(MerchantMembershipStatus.ACTIVE);
    merchantMembershipRepository.save(membership);

    RbacRole merchantRole =
        rbacRoleRepository
            .findByCode(PlatformUserRole.MERCHANT.name())
            .orElseThrow(
                () ->
                    new ApiException(
                        "RBAC role missing: MERCHANT", HttpStatus.INTERNAL_SERVER_ERROR));
    List<PlatformUserRbacBinding> bindings =
        platformUserRbacBindingRepository.findBindingsForUserContext(
            user.getId(), PlatformUserRole.MERCHANT.name(), merchant.getId());
    PlatformUserRbacBinding binding;
    if (bindings.isEmpty()) {
      binding = new PlatformUserRbacBinding();
      binding.setPlatformUser(user);
      binding.setRbacRole(merchantRole);
      binding.setMerchant(merchant);
    } else {
      binding = bindings.get(0);
    }
    binding.setStatus(PlatformRbacBindingStatus.ACTIVE);
    platformUserRbacBindingRepository.save(binding);
    user.setCredentialVersion(user.getCredentialVersion() + 1);
    platformUserRepository.save(user);
    platformAuditService.recordForCurrentUser(
        "auth.merchant.enable",
        "merchant",
        merchant.getId(),
        null,
        "{\"membership\":\"ACTIVE\",\"role\":\"MERCHANT\"}",
        "createdBy=" + principal.username());
    return new MerchantEnableResponse(
        merchant.getId(),
        merchant.getName(),
        merchant.getSlug(),
        PlatformUserRole.MERCHANT.canonicalCode(),
        MerchantMembershipStatus.ACTIVE.name());
  }

  private List<AuthContextOption> buildContextsWithoutDbUser(PlatformPrincipal principal) {
    List<AuthContextOption> single =
        List.of(authOptionForRoleAndMerchant(principal.role(), principal.merchantId()));
    return appendAdminMerchantPreview(single, principal);
  }

  private static AuthContextOption authOptionForRoleAndMerchant(PlatformUserRole role, Long merchantId) {
    boolean scoped =
        (role == PlatformUserRole.MERCHANT || role == PlatformUserRole.SUB_MERCHANT)
            && merchantId != null;
    String kind = scoped ? "MERCHANT_SCOPED" : "PLATFORM";
    return new AuthContextOption(kind, merchantId, role.name());
  }

  private List<AuthContextOption> appendAdminMerchantPreview(
      List<AuthContextOption> base, PlatformPrincipal principal) {
    if (principal.role() != PlatformUserRole.SYSTEM_ADMIN) {
      return List.copyOf(base);
    }
    return merchantRepository
        .findFirstByOrderByIdAsc()
        .map(
            m -> {
              long mId = m.getId();
              boolean already =
                  base.stream()
                      .anyMatch(
                          o ->
                              PlatformUserRole.MERCHANT.name().equals(o.role())
                                  && Objects.equals(o.merchantId(), mId));
              if (already) {
                return List.copyOf(base);
              }
              List<AuthContextOption> out = new ArrayList<>(base);
              out.add(
                  new AuthContextOption(
                      "MERCHANT_SCOPED", mId, PlatformUserRole.MERCHANT.name()));
              return List.copyOf(out);
            })
        .orElseGet(() -> List.copyOf(base));
  }

  private static List<ActiveAuthContext> dedupeActiveContexts(List<ActiveAuthContext> raw) {
    var seen = new LinkedHashSet<String>();
    List<ActiveAuthContext> out = new ArrayList<>();
    for (ActiveAuthContext c : raw) {
      String k = c.roleCode() + ":" + (c.merchantId() == null ? "null" : c.merchantId());
      if (seen.add(k)) {
        out.add(c);
      }
    }
    return out;
  }

  private ActiveAuthContext resolveInitialContext(PlatformUser user) {
    if (rbacRoleRepository.count() == 0L) {
      Long mid = user.getMerchant() == null ? null : user.getMerchant().getId();
      return new ActiveAuthContext(user.getRole().name(), mid);
    }
    List<ActiveAuthContext> raw = platformUserRbacBindingRepository.findActiveAuthContexts(user.getId());
    if (raw.isEmpty()) {
      Long mid = user.getMerchant() == null ? null : user.getMerchant().getId();
      return new ActiveAuthContext(user.getRole().name(), mid);
    }
    List<ActiveAuthContext> deduped = dedupeActiveContexts(raw);
    List<ActiveAuthContext> supported =
        deduped.stream().filter(c -> parseRoleCode(c.roleCode()).isPresent()).toList();
    if (supported.isEmpty()) {
      Long mid = user.getMerchant() == null ? null : user.getMerchant().getId();
      return new ActiveAuthContext(user.getRole().name(), mid);
    }
    Long userMid = user.getMerchant() == null ? null : user.getMerchant().getId();
    String primaryRole = user.getRole().name();
    for (ActiveAuthContext c : supported) {
      if (primaryRole.equals(c.roleCode()) && Objects.equals(userMid, c.merchantId())) {
        return c;
      }
    }
    return supported.stream().min(ACTIVE_CONTEXT_ORDER).orElseThrow();
  }

  private List<String> resolveAllRoleCodes(String username, PlatformUserRole activeFallback) {
    if (rbacRoleRepository.count() == 0L) {
      return List.of(activeFallback.name());
    }
    return platformUserRepository
        .findByUsername(username)
        .map(
            u -> {
              List<ActiveAuthContext> ctxs = platformUserRbacBindingRepository.findActiveAuthContexts(u.getId());
              if (ctxs.isEmpty()) {
                return List.of(u.getRole().name());
              }
              return dedupeActiveContexts(ctxs).stream()
                  .filter(c -> parseRoleCode(c.roleCode()).isPresent())
                  .map(ActiveAuthContext::roleCode)
                  .distinct()
                  .sorted()
                  .toList();
            })
        .orElse(List.of(activeFallback.name()));
  }

  private static Optional<PlatformUserRole> parseRoleCode(String roleCode) {
    return PlatformUserRole.parse(roleCode);
  }

  private static int safeRoleOrdinal(String roleCode) {
    return parseRoleCode(roleCode).map(Enum::ordinal).orElse(Integer.MAX_VALUE);
  }

  private String toCanonicalRoleCode(String roleCode) {
    return parseRoleCode(roleCode).map(PlatformUserRole::canonicalCode).orElse(roleCode);
  }

  private List<String> resolveRoleAliases(List<String> roleCodes) {
    if (roleCodes == null || roleCodes.isEmpty()) {
      return List.of();
    }
    LinkedHashSet<String> aliases = new LinkedHashSet<>();
    for (String roleCode : roleCodes) {
      Optional<PlatformUserRole> parsed = parseRoleCode(roleCode);
      if (parsed.isPresent()) {
        aliases.addAll(parsed.get().aliases());
      } else if (StringUtils.hasText(roleCode)) {
        aliases.add(roleCode);
      }
    }
    return List.copyOf(aliases);
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

  private void assertContextMembershipAllowed(
      String username, PlatformUserRole requestedRole, Long merchantId) {
    PlatformUser user = platformUserRepository.findByUsername(username).orElse(null);
    if (user == null) {
      return;
    }
    if (!isContextAllowedByMembership(user, requestedRole, merchantId)) {
      throw new ApiException("Context not allowed for this principal", HttpStatus.FORBIDDEN);
    }
  }

  private boolean isContextAllowedByMembership(
      PlatformUser user, PlatformUserRole requestedRole, Long merchantId) {
    if (user.getRole() == PlatformUserRole.SYSTEM_ADMIN) {
      return true;
    }
    if (requestedRole != PlatformUserRole.MERCHANT && requestedRole != PlatformUserRole.SUB_MERCHANT) {
      return true;
    }
    if (merchantId == null) {
      return false;
    }
    var membership =
        merchantMembershipRepository.findByMerchantIdAndPlatformUserId(merchantId, user.getId());
    if (membership.isPresent()) {
      return membership.get().getMembershipStatus() == MerchantMembershipStatus.ACTIVE;
    }
    // Backward-compatibility for pre-membership users seeded with primary merchant.
    if (user.getMerchant() != null && merchantId.equals(user.getMerchant().getId())) {
      return true;
    }
    return false;
  }
}
