package com.bookingcore.service;

import com.bookingcore.api.ApiDtos.SystemRbacRoleResponse;
import com.bookingcore.api.ApiDtos.SystemUserBindingSummary;
import com.bookingcore.api.ApiDtos.SystemUserBindingUpsertRequest;
import com.bookingcore.api.ApiDtos.SystemUserBindingsUpdateRequest;
import com.bookingcore.api.ApiDtos.SystemUserDetailResponse;
import com.bookingcore.api.ApiDtos.SystemUserSummary;
import com.bookingcore.api.ApiDtos.SystemUserStatusUpdateRequest;
import com.bookingcore.common.ApiException;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.MerchantRepository;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.platform.PlatformUserRepository;
import com.bookingcore.modules.platform.rbac.PlatformRbacBindingStatus;
import com.bookingcore.modules.platform.rbac.PlatformUserRbacBinding;
import com.bookingcore.modules.platform.rbac.PlatformUserRbacBindingRepository;
import com.bookingcore.modules.platform.rbac.RbacRole;
import com.bookingcore.modules.platform.rbac.RbacRoleRepository;
import com.bookingcore.security.PlatformUserRole;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SystemUserManagementService {

  private final PlatformUserRepository platformUserRepository;
  private final PlatformUserRbacBindingRepository platformUserRbacBindingRepository;
  private final RbacRoleRepository rbacRoleRepository;
  private final MerchantRepository merchantRepository;
  private final PlatformAuditService platformAuditService;

  public SystemUserManagementService(
      PlatformUserRepository platformUserRepository,
      PlatformUserRbacBindingRepository platformUserRbacBindingRepository,
      RbacRoleRepository rbacRoleRepository,
      MerchantRepository merchantRepository,
      PlatformAuditService platformAuditService) {
    this.platformUserRepository = platformUserRepository;
    this.platformUserRbacBindingRepository = platformUserRbacBindingRepository;
    this.rbacRoleRepository = rbacRoleRepository;
    this.merchantRepository = merchantRepository;
    this.platformAuditService = platformAuditService;
  }

  @Transactional(readOnly = true)
  public List<SystemUserSummary> listUsers() {
    List<PlatformUser> users = platformUserRepository.findAll(Sort.by("id").ascending());
    List<SystemUserSummary> out = new ArrayList<>();
    for (PlatformUser user : users) {
      List<PlatformUserRbacBinding> bindings = bindingsForUser(user.getId());
      List<PlatformUserRbacBinding> active =
          bindings.stream().filter(b -> b.getStatus() == PlatformRbacBindingStatus.ACTIVE).toList();
      Set<String> roleCodes = new LinkedHashSet<>();
      for (PlatformUserRbacBinding b : active) {
        roleCodes.add(b.getRbacRole().getCode());
      }
      out.add(
          new SystemUserSummary(
              user.getId(),
              user.getUsername(),
              user.getEnabled(),
              user.getRole().name(),
              user.getMerchant() == null ? null : user.getMerchant().getId(),
              (long) active.size(),
              List.copyOf(roleCodes),
              user.getLastLoginAt(),
              user.getUpdatedAt()));
    }
    return out;
  }

  @Transactional(readOnly = true)
  public SystemUserDetailResponse getUserDetail(Long userId) {
    PlatformUser user = requireUser(userId);
    List<PlatformUserRbacBinding> bindings = bindingsForUser(user.getId());
    List<SystemUserBindingSummary> bindingDtos =
        bindings.stream()
            .map(this::toBindingSummary)
            .sorted(
                Comparator.comparing(SystemUserBindingSummary::roleCode)
                    .thenComparing(b -> b.merchantId() == null ? Long.MIN_VALUE : b.merchantId())
                    .thenComparing(SystemUserBindingSummary::status))
            .toList();

    Set<String> perms = new LinkedHashSet<>();
    for (PlatformUserRbacBinding b : bindings) {
      if (b.getStatus() != PlatformRbacBindingStatus.ACTIVE) {
        continue;
      }
      b.getRbacRole().getPermissions().stream()
          .map(p -> p.getCode())
          .sorted()
          .forEach(perms::add);
    }

    return new SystemUserDetailResponse(
        user.getId(),
        user.getUsername(),
        user.getEnabled(),
        user.getRole().name(),
        user.getMerchant() == null ? null : user.getMerchant().getId(),
        bindingDtos,
        List.copyOf(perms),
        user.getLastLoginAt(),
        user.getUpdatedAt());
  }

  @Transactional
  public SystemUserDetailResponse updateUserStatus(Long userId, SystemUserStatusUpdateRequest request) {
    PlatformUser user = requireUser(userId);
    platformUserRbacBindingRepository.lockEnabledActiveSystemAdminBindings();
    boolean beforeEnabled = Boolean.TRUE.equals(user.getEnabled());
    if (beforeEnabled == Boolean.TRUE.equals(request.enabled())) {
      throw new ApiException(
          "Requested status is already applied", HttpStatus.CONFLICT, "USER_STATUS_NOOP");
    }
    if (!Boolean.TRUE.equals(request.enabled()) && hasActiveSystemAdminBinding(user.getId())) {
      ensureAnotherEnabledSystemAdminExists(user.getId());
    }
    user.setEnabled(request.enabled());
    user.setCredentialVersion(user.getCredentialVersion() + 1);
    platformUserRepository.save(user);
    platformAuditService.recordForCurrentUser(
        "system.user.status.updated",
        "platform_user",
        userId,
        "{\"enabled\":" + beforeEnabled + "}",
        "{\"enabled\":" + request.enabled() + "}",
        "enabled=" + request.enabled() + ", cv=" + user.getCredentialVersion());
    return getUserDetail(userId);
  }

  @Transactional
  public SystemUserDetailResponse replaceBindings(Long userId, SystemUserBindingsUpdateRequest request) {
    PlatformUser user = requireUser(userId);
    platformUserRbacBindingRepository.lockBindingsForUser(userId);
    platformUserRbacBindingRepository.lockEnabledActiveSystemAdminBindings();
    if (request == null || request.bindings() == null) {
      throw new ApiException("bindings are required", HttpStatus.BAD_REQUEST, "BINDINGS_REQUIRED");
    }
    List<SystemUserBindingUpsertRequest> entries = request.bindings();
    Map<String, SystemUserBindingUpsertRequest> desired = new LinkedHashMap<>();
    for (SystemUserBindingUpsertRequest item : entries) {
      String roleCode = normalizeRoleCode(item.roleCode());
      if (item.active() == null) {
        throw new ApiException(
            "active is required", HttpStatus.BAD_REQUEST, "BINDING_ACTIVE_REQUIRED");
      }
      validateRoleScope(user, roleCode, item.merchantId());
      String key = key(roleCode, item.merchantId());
      desired.put(key, new SystemUserBindingUpsertRequest(roleCode, item.merchantId(), item.active()));
    }

    List<PlatformUserRbacBinding> existing = bindingsForUser(user.getId());
    boolean beforeAdmin =
        existing.stream()
            .anyMatch(
                b ->
                    b.getStatus() == PlatformRbacBindingStatus.ACTIVE
                        && "SYSTEM_ADMIN".equals(b.getRbacRole().getCode())
                        && b.getMerchant() == null);
    boolean afterAdmin = desiredHasSystemAdminActive(desired);
    if (user.getEnabled() && beforeAdmin && !afterAdmin) {
      ensureAnotherEnabledSystemAdminExists(user.getId());
    }

    Map<String, PlatformUserRbacBinding> existingByKey = new LinkedHashMap<>();
    for (PlatformUserRbacBinding b : existing) {
      existingByKey.put(key(b.getRbacRole().getCode(), b.getMerchant() == null ? null : b.getMerchant().getId()), b);
    }

    int changed = 0;
    int beforeActive =
        (int)
            existing.stream()
                .filter(b -> b.getStatus() == PlatformRbacBindingStatus.ACTIVE)
                .count();
    for (SystemUserBindingUpsertRequest item : desired.values()) {
      String k = key(item.roleCode(), item.merchantId());
      PlatformUserRbacBinding current = existingByKey.get(k);
      PlatformRbacBindingStatus target =
          Boolean.TRUE.equals(item.active())
              ? PlatformRbacBindingStatus.ACTIVE
              : PlatformRbacBindingStatus.DISABLED;
      if (current == null) {
        PlatformUserRbacBinding created = new PlatformUserRbacBinding();
        created.setPlatformUser(user);
        created.setRbacRole(requireRole(item.roleCode()));
        created.setMerchant(resolveMerchant(item.merchantId()));
        created.setStatus(target);
        try {
          platformUserRbacBindingRepository.saveAndFlush(created);
          changed++;
        } catch (DataIntegrityViolationException ex) {
          throw new ApiException(
              "Concurrent binding update conflict", HttpStatus.CONFLICT, "RBAC_BINDING_CONFLICT");
        }
      } else if (current.getStatus() != target) {
        current.setStatus(target);
        platformUserRbacBindingRepository.save(current);
        changed++;
      }
    }

    for (PlatformUserRbacBinding current : existing) {
      String k = key(current.getRbacRole().getCode(), current.getMerchant() == null ? null : current.getMerchant().getId());
      if (desired.containsKey(k)) {
        continue;
      }
      if (current.getStatus() == PlatformRbacBindingStatus.ACTIVE) {
        current.setStatus(PlatformRbacBindingStatus.DISABLED);
        platformUserRbacBindingRepository.save(current);
        changed++;
      }
    }

    if (changed > 0) {
      user.setCredentialVersion(user.getCredentialVersion() + 1);
      platformUserRepository.save(user);
    }
    platformAuditService.recordForCurrentUser(
        "system.user.bindings.replaced",
        "platform_user",
        userId,
        "{\"activeBindings\":" + beforeActive + "}",
        "{\"activeBindings\":"
            + bindingsForUser(user.getId()).stream()
                .filter(b -> b.getStatus() == PlatformRbacBindingStatus.ACTIVE)
                .count()
            + "}",
        "bindings=" + desired.size() + ", changed=" + changed + ", cv=" + user.getCredentialVersion());
    return getUserDetail(userId);
  }

  @Transactional(readOnly = true)
  public List<SystemRbacRoleResponse> listRoleCatalog() {
    return rbacRoleRepository.findAll(Sort.by("code").ascending()).stream()
        .map(
            r ->
                new SystemRbacRoleResponse(
                    r.getCode(),
                    r.getPermissions().stream().map(p -> p.getCode()).sorted().toList()))
        .toList();
  }

  private PlatformUser requireUser(Long userId) {
    return platformUserRepository
        .findById(userId)
        .orElseThrow(
            () -> new ApiException("User not found", HttpStatus.NOT_FOUND, "SYSTEM_USER_NOT_FOUND"));
  }

  private RbacRole requireRole(String roleCode) {
    return rbacRoleRepository
        .findByCode(roleCode)
        .orElseThrow(
            () ->
                new ApiException(
                    "RBAC role not found: " + roleCode, HttpStatus.BAD_REQUEST, "RBAC_ROLE_NOT_FOUND"));
  }

  private Merchant resolveMerchant(Long merchantId) {
    if (merchantId == null) {
      return null;
    }
    return merchantRepository
        .findById(merchantId)
        .orElseThrow(
            () -> new ApiException("Merchant not found", HttpStatus.BAD_REQUEST, "MERCHANT_NOT_FOUND"));
  }

  private List<PlatformUserRbacBinding> bindingsForUser(Long userId) {
    return platformUserRbacBindingRepository.findBindingsForUserWithRoleAndPermissions(userId);
  }

  private SystemUserBindingSummary toBindingSummary(PlatformUserRbacBinding b) {
    return new SystemUserBindingSummary(
        b.getId(),
        b.getRbacRole().getCode(),
        b.getMerchant() == null ? null : b.getMerchant().getId(),
        b.getStatus().name(),
        b.getRbacRole().getPermissions().stream().map(p -> p.getCode()).sorted().toList());
  }

  private static String normalizeRoleCode(String roleCode) {
    if (!StringUtils.hasText(roleCode)) {
      throw new ApiException("roleCode is required", HttpStatus.BAD_REQUEST, "ROLE_CODE_REQUIRED");
    }
    return roleCode.trim().toUpperCase(Locale.ROOT);
  }

  private boolean hasActiveSystemAdminBinding(Long userId) {
    return bindingsForUser(userId).stream()
        .anyMatch(
            b ->
                b.getStatus() == PlatformRbacBindingStatus.ACTIVE
                    && "SYSTEM_ADMIN".equals(b.getRbacRole().getCode())
                    && b.getMerchant() == null);
  }

  private static boolean desiredHasSystemAdminActive(
      Map<String, SystemUserBindingUpsertRequest> desired) {
    return desired.values().stream()
        .anyMatch(
            d ->
                Boolean.TRUE.equals(d.active())
                    && "SYSTEM_ADMIN".equals(d.roleCode())
                    && d.merchantId() == null);
  }

  private void ensureAnotherEnabledSystemAdminExists(Long excludedUserId) {
    long other = platformUserRbacBindingRepository.countOtherEnabledActiveSystemAdmins(excludedUserId);
    if (other <= 0) {
      throw new ApiException(
          "Cannot remove or disable the last enabled SYSTEM_ADMIN context",
          HttpStatus.CONFLICT,
          "LAST_SYSTEM_ADMIN_REQUIRED");
    }
  }

  private void validateRoleScope(PlatformUser targetUser, String roleCode, Long merchantId) {
    if (!rbacRoleRepository.findByCode(roleCode).isPresent()) {
      throw new ApiException(
          "Unknown roleCode: " + roleCode, HttpStatus.BAD_REQUEST, "RBAC_ROLE_UNKNOWN");
    }
    PlatformUserRole mapped = parsePlatformRole(roleCode);
    if (mapped == PlatformUserRole.MERCHANT || mapped == PlatformUserRole.SUB_MERCHANT) {
      if (merchantId == null) {
        throw new ApiException(
            "merchantId is required for merchant-scoped role",
            HttpStatus.BAD_REQUEST,
            "MERCHANT_ID_REQUIRED");
      }
      if (targetUser.getMerchant() != null && !merchantId.equals(targetUser.getMerchant().getId())) {
        throw new ApiException(
            "merchantId is incompatible with target user's tenant scope",
            HttpStatus.BAD_REQUEST,
            "RBAC_TENANT_MISMATCH");
      }
      return;
    }
    if (mapped != null && merchantId != null) {
      throw new ApiException(
          "merchantId is not allowed for platform-scoped role",
          HttpStatus.BAD_REQUEST,
          "MERCHANT_ID_NOT_ALLOWED");
    }
    if (mapped == null && merchantId != null) {
      throw new ApiException(
          "merchantId is not allowed for custom role",
          HttpStatus.BAD_REQUEST,
          "MERCHANT_ID_NOT_ALLOWED");
    }
  }

  private static PlatformUserRole parsePlatformRole(String roleCode) {
    try {
      return PlatformUserRole.valueOf(roleCode);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private static String key(String roleCode, Long merchantId) {
    return roleCode + "|" + (merchantId == null ? "null" : merchantId);
  }
}
