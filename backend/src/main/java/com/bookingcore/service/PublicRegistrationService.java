package com.bookingcore.service;

import com.bookingcore.api.ApiDtos.CreateMerchantRequest;
import com.bookingcore.api.ApiDtos.PublicRegisterRequest;
import com.bookingcore.api.ApiDtos.PublicRegisterResponse;
import com.bookingcore.api.PublicRegisterType;
import com.bookingcore.common.ApiException;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.platform.PlatformUserRepository;
import com.bookingcore.modules.platform.rbac.PlatformRbacBindingStatus;
import com.bookingcore.modules.platform.rbac.PlatformUserRbacBinding;
import com.bookingcore.modules.platform.rbac.PlatformUserRbacBindingRepository;
import com.bookingcore.modules.platform.rbac.RbacRoleRepository;
import com.bookingcore.security.PlatformUserRole;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PublicRegistrationService {

  /** Types permitted for anonymous public registration (no privilege elevation). */
  private static final Set<PublicRegisterType> ALLOWED =
      EnumSet.of(PublicRegisterType.MERCHANT, PublicRegisterType.CLIENT);

  /**
   * Fixed post-signup routes; must not reflect user input (open redirect safety).
   */
  public static final String NEXT_DESTINATION_MERCHANT_LOGIN =
      "/?auth=login&intent=merchant&registered=1";

  public static final String NEXT_DESTINATION_CLIENT_LOGIN =
      "/?auth=login&intent=client&registered=1";

  private static final int USERNAME_MAX = 120;
  private static final Pattern SIMPLE_EMAIL_PATTERN =
      Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

  private final MerchantProvisioningService merchantProvisioningService;
  private final PlatformAuditService platformAuditService;
  private final PlatformUserRepository platformUserRepository;
  private final PasswordEncoder passwordEncoder;
  private final RbacRoleRepository rbacRoleRepository;
  private final PlatformUserRbacBindingRepository platformUserRbacBindingRepository;

  public PublicRegistrationService(
      MerchantProvisioningService merchantProvisioningService,
      PlatformAuditService platformAuditService,
      PlatformUserRepository platformUserRepository,
      PasswordEncoder passwordEncoder,
      RbacRoleRepository rbacRoleRepository,
      PlatformUserRbacBindingRepository platformUserRbacBindingRepository) {
    this.merchantProvisioningService = merchantProvisioningService;
    this.platformAuditService = platformAuditService;
    this.platformUserRepository = platformUserRepository;
    this.passwordEncoder = passwordEncoder;
    this.rbacRoleRepository = rbacRoleRepository;
    this.platformUserRbacBindingRepository = platformUserRbacBindingRepository;
  }

  @Transactional
  public PublicRegisterResponse register(PublicRegisterRequest request) {
    PublicRegisterType type = request.registerType();
    if (!ALLOWED.contains(type)) {
      if (type == PublicRegisterType.SYSTEM_ADMIN || type == PublicRegisterType.SUB_MERCHANT) {
        throw new ApiException("Public registration cannot create this account type", HttpStatus.FORBIDDEN);
      }
      throw new ApiException("Unsupported registerType", HttpStatus.BAD_REQUEST);
    }
    if (type == PublicRegisterType.MERCHANT) {
      return registerMerchant(request);
    }
    return registerClient(request);
  }

  private PublicRegisterResponse registerMerchant(PublicRegisterRequest request) {
    String name = request.name() == null ? "" : request.name().trim();
    String slug = request.slug() == null ? "" : request.slug().trim();
    if (!StringUtils.hasText(name) || !StringUtils.hasText(slug)) {
      throw new ApiException("Name and slug are required for merchant registration", HttpStatus.BAD_REQUEST);
    }

    Merchant merchant = merchantProvisioningService.createMerchant(new CreateMerchantRequest(name, slug));
    platformAuditService.record(
        "merchant.self_registered",
        "merchant",
        merchant.getId(),
        "name=" + name + " slug=" + slug,
        "anonymous");

    return new PublicRegisterResponse(
        merchant.getId(),
        merchant.getName(),
        merchant.getSlug(),
        merchant.getActive(),
        NEXT_DESTINATION_MERCHANT_LOGIN);
  }

  private PublicRegisterResponse registerClient(PublicRegisterRequest request) {
    String username = request.username() == null ? "" : request.username().trim();
    String password = request.password() == null ? "" : request.password();
    if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
      throw new ApiException("Email and password are required for user registration", HttpStatus.BAD_REQUEST);
    }
    if (!looksLikeEmail(username)) {
      throw new ApiException("A valid email is required for user registration", HttpStatus.BAD_REQUEST);
    }
    String normalizedEmail = username.toLowerCase(Locale.ROOT);
    if (username.length() > USERNAME_MAX) {
      throw new ApiException("Username is too long", HttpStatus.BAD_REQUEST);
    }
    if (password.length() < 6) {
      throw new ApiException("Password must be at least 6 characters", HttpStatus.BAD_REQUEST);
    }
    if (platformUserRepository.findByUsernameIgnoreCase(normalizedEmail).isPresent()) {
      throw new ApiException("Email already registered", HttpStatus.CONFLICT);
    }

    PlatformUser user = new PlatformUser();
    user.setUsername(normalizedEmail);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setRole(PlatformUserRole.CLIENT);
    user.setEnabled(true);
    user.setMerchant(null);
    platformUserRepository.save(user);

    ensureRbacBinding(user, PlatformUserRole.CLIENT, null);

    platformAuditService.record(
        "client.self_registered", "platform_user", user.getId(), "username=" + normalizedEmail, "anonymous");

    return new PublicRegisterResponse(user.getId(), normalizedEmail, "-", true, NEXT_DESTINATION_CLIENT_LOGIN);
  }

  private static boolean looksLikeEmail(String value) {
    return StringUtils.hasText(value) && SIMPLE_EMAIL_PATTERN.matcher(value.trim()).matches();
  }

  /** Mirrors bootstrap binding logic so self-registered CLIENT users get baseline permissions. */
  private void ensureRbacBinding(PlatformUser user, PlatformUserRole role, Merchant merchant) {
    if (rbacRoleRepository.count() == 0L) {
      return;
    }
    var rbacRole =
        rbacRoleRepository
            .findByCode(role.name())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Public registration: RBAC role missing for code '" + role.name() + "'"));
    Long merchantId = merchant == null ? null : merchant.getId();
    var existing =
        platformUserRbacBindingRepository.findBindingsForUserContext(user.getId(), role.name(), merchantId);
    if (existing.stream().anyMatch(b -> b.getStatus() == PlatformRbacBindingStatus.ACTIVE)) {
      return;
    }
    Optional<PlatformUserRbacBinding> reusable =
        existing.stream().filter(b -> b.getStatus() != PlatformRbacBindingStatus.ACTIVE).findFirst();
    if (reusable.isPresent()) {
      PlatformUserRbacBinding binding = reusable.get();
      binding.setStatus(PlatformRbacBindingStatus.ACTIVE);
      if (!Objects.equals(binding.getRbacRole().getId(), rbacRole.getId())) {
        binding.setRbacRole(rbacRole);
      }
      if (!Objects.equals(
          binding.getMerchant() == null ? null : binding.getMerchant().getId(), merchantId)) {
        binding.setMerchant(merchant);
      }
      platformUserRbacBindingRepository.save(binding);
      return;
    }
    PlatformUserRbacBinding binding = new PlatformUserRbacBinding();
    binding.setPlatformUser(user);
    binding.setRbacRole(rbacRole);
    binding.setMerchant(merchant);
    binding.setStatus(PlatformRbacBindingStatus.ACTIVE);
    platformUserRbacBindingRepository.save(binding);
  }
}
