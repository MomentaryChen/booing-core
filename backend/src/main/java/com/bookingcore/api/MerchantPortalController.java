package com.bookingcore.api;

import java.util.UUID;
import com.bookingcore.api.ApiDtos.AvailabilityExceptionRequest;
import com.bookingcore.api.ApiDtos.AvailabilityExceptionSummary;
import com.bookingcore.api.ApiDtos.BusinessHoursSummary;
import com.bookingcore.api.ApiDtos.BusinessHoursRequest;
import com.bookingcore.api.ApiDtos.CreateMerchantRequest;
import com.bookingcore.api.ApiDtos.PublicRegisterRequest;
import com.bookingcore.api.ApiDtos.PublicRegisterResponse;
import com.bookingcore.api.ApiDtos.CustomizationRequest;
import com.bookingcore.api.ApiDtos.DynamicFieldSummary;
import com.bookingcore.api.ApiDtos.DynamicFieldRequest;
import com.bookingcore.api.ApiDtos.ManualBookingRequest;
import com.bookingcore.api.ApiDtos.BookingAssignmentCommandRequest;
import com.bookingcore.api.ApiDtos.BookingAssignmentSummary;
import com.bookingcore.api.ApiDtos.MerchantInvitationCreateRequest;
import com.bookingcore.api.ApiDtos.MerchantInvitationSummary;
import com.bookingcore.api.ApiDtos.MerchantInvitationUpdateRequest;
import com.bookingcore.api.ApiDtos.MerchantBookingStatusUpdateRequest;
import com.bookingcore.api.ApiDtos.MerchantBookingSummary;
import com.bookingcore.api.ApiDtos.MerchantCustomizationResponse;
import com.bookingcore.api.ApiDtos.MerchantProfileResponse;
import com.bookingcore.api.ApiDtos.MerchantSummary;
import com.bookingcore.api.ApiDtos.MerchantVisibilityUpdateRequest;
import com.bookingcore.api.ApiDtos.TeamCreateRequest;
import com.bookingcore.api.ApiDtos.TeamMemberAssignRequest;
import com.bookingcore.api.ApiDtos.TeamMemberSummary;
import com.bookingcore.api.ApiDtos.TeamSummary;
import com.bookingcore.api.ApiDtos.TeamUpdateRequest;
import com.bookingcore.api.ApiDtos.ResourceItemSummary;
import com.bookingcore.api.ApiDtos.StaffCandidateSummary;
import com.bookingcore.api.ApiDtos.ResourceRequest;
import com.bookingcore.api.ApiDtos.ResourceBatchBusinessHoursRequest;
import com.bookingcore.api.ApiDtos.ResourceBatchPriceRequest;
import com.bookingcore.api.ApiDtos.ResourceBatchStatusRequest;
import com.bookingcore.api.ApiDtos.MerchantResourceListResponse;
import com.bookingcore.api.ApiDtos.ResourceOperationalStatus;
import com.bookingcore.api.ApiDtos.ResourceUpdateRequest;
import com.bookingcore.api.ApiDtos.ServiceCloneRequest;
import com.bookingcore.api.ApiDtos.ServiceItemSummary;
import com.bookingcore.api.ApiDtos.ServiceItemRequest;
import com.bookingcore.api.support.ResourceItemMediaResolver;
import com.bookingcore.common.ApiException;
import com.bookingcore.modules.booking.AvailabilityException;
import com.bookingcore.modules.booking.AvailabilityExceptionRepository;
import com.bookingcore.modules.booking.Booking;
import com.bookingcore.modules.booking.BookingRepository;
import com.bookingcore.modules.booking.BookingStatus;
import com.bookingcore.modules.booking.BookingTransitionEvent;
import com.bookingcore.modules.booking.BusinessHours;
import com.bookingcore.modules.booking.BusinessHoursRepository;
import com.bookingcore.modules.customization.CustomizationConfig;
import com.bookingcore.modules.customization.CustomizationConfigRepository;
import com.bookingcore.modules.merchant.DynamicFieldConfig;
import com.bookingcore.modules.merchant.DynamicFieldConfigRepository;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.MerchantInvitation;
import com.bookingcore.modules.merchant.MerchantInvitationRepository;
import com.bookingcore.modules.merchant.MerchantInvitationStatus;
import com.bookingcore.modules.merchant.MerchantProfile;
import com.bookingcore.modules.merchant.MerchantProfileRepository;
import com.bookingcore.modules.merchant.MerchantRepository;
import com.bookingcore.modules.merchant.ServiceTeam;
import com.bookingcore.modules.merchant.ServiceTeamRepository;
import com.bookingcore.modules.merchant.ServiceTeamStatus;
import com.bookingcore.modules.merchant.TeamMember;
import com.bookingcore.modules.merchant.TeamMemberRepository;
import com.bookingcore.modules.merchant.TeamMemberStatus;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.platform.PlatformUserRepository;
import com.bookingcore.modules.merchant.ResourceItem;
import com.bookingcore.modules.merchant.ResourceItemRepository;
import com.bookingcore.modules.service.ServiceItem;
import com.bookingcore.modules.service.ServiceItemRepository;
import com.bookingcore.service.BookingCommandService;
import com.bookingcore.service.MerchantProvisioningService;
import com.bookingcore.service.PublicRegistrationService;
import com.bookingcore.service.PlatformAuditService;
import com.bookingcore.service.MerchantAccessService;
import com.bookingcore.service.MerchantBookingAssignmentService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/merchant")
@Tag(name = "Merchant")
@PreAuthorize("@permissionAuthorizer.hasPermission(authentication, 'merchant.portal.access')")
public class MerchantPortalController {
  private final MerchantProfileRepository profileRepository;
  private final MerchantRepository merchantRepository;
  private final ServiceItemRepository serviceItemRepository;
  private final BusinessHoursRepository businessHoursRepository;
  private final BookingRepository bookingRepository;
  private final CustomizationConfigRepository customizationConfigRepository;
  private final DynamicFieldConfigRepository dynamicFieldConfigRepository;
  private final ResourceItemRepository resourceItemRepository;
  private final AvailabilityExceptionRepository availabilityExceptionRepository;
  private final MerchantProvisioningService merchantProvisioningService;
  private final PublicRegistrationService publicRegistrationService;
  private final BookingCommandService bookingCommandService;
  private final PlatformAuditService platformAuditService;
  private final MerchantInvitationRepository merchantInvitationRepository;
  private final PlatformUserRepository platformUserRepository;
  private final MerchantAccessService merchantAccessService;
  private final ServiceTeamRepository serviceTeamRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final MerchantBookingAssignmentService merchantBookingAssignmentService;
  private final ResourceItemMediaResolver resourceItemMediaResolver;
  private final ObjectMapper objectMapper;

  public MerchantPortalController(
      MerchantProfileRepository profileRepository,
      MerchantRepository merchantRepository,
      ServiceItemRepository serviceItemRepository,
      BusinessHoursRepository businessHoursRepository,
      BookingRepository bookingRepository,
      CustomizationConfigRepository customizationConfigRepository,
      DynamicFieldConfigRepository dynamicFieldConfigRepository,
      ResourceItemRepository resourceItemRepository,
      AvailabilityExceptionRepository availabilityExceptionRepository,
      MerchantProvisioningService merchantProvisioningService,
      PublicRegistrationService publicRegistrationService,
      BookingCommandService bookingCommandService,
      PlatformAuditService platformAuditService,
      MerchantInvitationRepository merchantInvitationRepository,
      PlatformUserRepository platformUserRepository,
      MerchantAccessService merchantAccessService,
      ServiceTeamRepository serviceTeamRepository,
      TeamMemberRepository teamMemberRepository,
      MerchantBookingAssignmentService merchantBookingAssignmentService,
      ResourceItemMediaResolver resourceItemMediaResolver,
      ObjectMapper objectMapper) {
    this.profileRepository = profileRepository;
    this.merchantRepository = merchantRepository;
    this.serviceItemRepository = serviceItemRepository;
    this.businessHoursRepository = businessHoursRepository;
    this.bookingRepository = bookingRepository;
    this.customizationConfigRepository = customizationConfigRepository;
    this.dynamicFieldConfigRepository = dynamicFieldConfigRepository;
    this.resourceItemRepository = resourceItemRepository;
    this.availabilityExceptionRepository = availabilityExceptionRepository;
    this.merchantProvisioningService = merchantProvisioningService;
    this.publicRegistrationService = publicRegistrationService;
    this.bookingCommandService = bookingCommandService;
    this.platformAuditService = platformAuditService;
    this.merchantInvitationRepository = merchantInvitationRepository;
    this.platformUserRepository = platformUserRepository;
    this.merchantAccessService = merchantAccessService;
    this.serviceTeamRepository = serviceTeamRepository;
    this.teamMemberRepository = teamMemberRepository;
    this.merchantBookingAssignmentService = merchantBookingAssignmentService;
    this.resourceItemMediaResolver = resourceItemMediaResolver;
    this.objectMapper = objectMapper;
  }

  private void assertMerchantScope(UUID merchantId) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      throw new ApiException("Unauthorized", HttpStatus.UNAUTHORIZED);
    }
    Object principal = auth.getPrincipal();
    // SYSTEM_ADMIN is already guarded by role in SecurityConfig; allow cross-merchant.
    boolean systemAdmin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_SYSTEM_ADMIN".equals(a.getAuthority()));
    if (systemAdmin) {
      return;
    }
    if (principal instanceof com.bookingcore.security.PlatformPrincipal p) {
      if (p.merchantId() == null) {
        throw new ApiException("Unauthorized", HttpStatus.UNAUTHORIZED);
      }
      if (!p.merchantId().equals(merchantId)) {
        platformAuditService.recordForCurrentUser(
            "merchant.scope.denied",
            "merchant",
            merchantId,
            "tokenMerchantId=" + p.merchantId() + " requestedMerchantId=" + merchantId);
        throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
      }
      if (merchantAccessService.hasActiveMembershipForCurrentUser(merchantId)) {
        return;
      }
      platformAuditService.recordForCurrentUser(
          "merchant.scope.denied",
          "merchant",
          merchantId,
          "membershipMissingOrInactive=true");
      throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
    }
    throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
  }

  @PostMapping("/merchants")
  @PreAuthorize("@permissionAuthorizer.hasPermission(authentication, 'merchant.registry.manage')")
  public MerchantSummary createMerchant(@RequestBody @Valid CreateMerchantRequest request) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    boolean systemAdmin = auth != null
        && auth.isAuthenticated()
        && auth.getAuthorities().stream().anyMatch(a -> "ROLE_SYSTEM_ADMIN".equals(a.getAuthority()));
    if (!systemAdmin) {
      throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
    }
    Merchant merchant = merchantProvisioningService.createMerchant(request);
    platformAuditService.recordForCurrentUser(
        "merchant.created",
        "merchant",
        merchant.getId(),
        "name=" + request.name() + " slug=" + request.slug());
    return new MerchantSummary(merchant.getId(), merchant.getName(), merchant.getSlug(), merchant.getActive());
  }

  /** Self-service merchant signup from the merchant portal. */
  @PostMapping("/register")
  @PreAuthorize("permitAll()")
  public PublicRegisterResponse registerMerchant(@RequestBody @Valid CreateMerchantRequest request) {
    return publicRegistrationService.register(
        new PublicRegisterRequest(
            PublicRegisterType.MERCHANT, request.name(), request.slug(), null, null));
  }

  @GetMapping("/{merchantId}/profile")
  public MerchantProfileResponse getProfile(@PathVariable UUID merchantId) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    MerchantProfile profile = profileRepository.findByMerchantId(merchantId)
        .orElseGet(() -> {
          MerchantProfile created = new MerchantProfile();
          created.setMerchant(bookingCommandService.ensureMerchant(merchantId));
          return profileRepository.save(created);
        });
    return new MerchantProfileResponse(
        profile.getDescription(),
        profile.getLogoUrl(),
        profile.getAddress(),
        profile.getPhone(),
        profile.getEmail(),
        profile.getWebsite(),
        profile.getStoreCategory(),
        profile.getLineContactUrl());
  }

  @PutMapping("/{merchantId}/profile/visibility")
  public MerchantSummary updateVisibility(
      @PathVariable UUID merchantId, @Valid @RequestBody MerchantVisibilityUpdateRequest request) {
    assertMerchantScope(merchantId);
    Merchant merchant = bookingCommandService.ensureMerchant(merchantId);
    merchant.setVisibility(request.visibility());
    Merchant saved = merchantRepository.save(merchant);
    platformAuditService.recordForCurrentUser(
        "merchant.visibility.updated",
        "merchant",
        merchantId,
        "visibility=" + request.visibility());
    return new MerchantSummary(saved.getId(), saved.getName(), saved.getSlug(), saved.getActive());
  }

  @GetMapping("/{merchantId}/invitations")
  public List<MerchantInvitationSummary> listInvitations(@PathVariable UUID merchantId) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    return merchantInvitationRepository.findByMerchantIdOrderByIdDesc(merchantId).stream()
        .map(
            i ->
                new MerchantInvitationSummary(
                    i.getId(),
                    i.getMerchant().getId(),
                    i.getInviteCode(),
                    i.getInviteeUser().getUsername(),
                    i.getStatus(),
                    i.getExpiresAt()))
        .toList();
  }

  @PostMapping("/{merchantId}/invitations")
  public MerchantInvitationSummary createInvitation(
      @PathVariable UUID merchantId, @Valid @RequestBody MerchantInvitationCreateRequest request) {
    assertMerchantScope(merchantId);
    Merchant merchant = bookingCommandService.ensureMerchant(merchantId);
    PlatformUser invitee =
        platformUserRepository
            .findByUsername(request.inviteeUsername())
            .orElseThrow(() -> new ApiException("Invitee user not found", HttpStatus.NOT_FOUND));

    MerchantInvitation invitation = new MerchantInvitation();
    invitation.setMerchant(merchant);
    invitation.setInviteeUser(invitee);
    invitation.setInviteCode(merchantAccessService.generateInviteCode());
    invitation.setStatus(MerchantInvitationStatus.PENDING);
    invitation.setExpiresAt(request.expiresAt());
    invitation.setCreatedBy(merchantAccessService.currentUsername());
    MerchantInvitation saved = merchantInvitationRepository.save(invitation);
    platformAuditService.recordForCurrentUser(
        "merchant.invitation.created",
        "merchant_invitation",
        saved.getId(),
        "merchantId=" + merchantId + " invitee=" + invitee.getUsername());
    return new MerchantInvitationSummary(
        saved.getId(),
        saved.getMerchant().getId(),
        saved.getInviteCode(),
        saved.getInviteeUser().getUsername(),
        saved.getStatus(),
        saved.getExpiresAt());
  }

  @PatchMapping("/{merchantId}/invitations/{invitationId}")
  public MerchantInvitationSummary updateInvitation(
      @PathVariable UUID merchantId,
      @PathVariable Long invitationId,
      @Valid @RequestBody MerchantInvitationUpdateRequest request) {
    assertMerchantScope(merchantId);
    MerchantInvitation invitation =
        merchantInvitationRepository
            .findById(invitationId)
            .orElseThrow(() -> new ApiException("Invitation not found", HttpStatus.NOT_FOUND));
    if (!invitation.getMerchant().getId().equals(merchantId)) {
      throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
    }
    if (invitation.getStatus() != MerchantInvitationStatus.PENDING
        || request.status() != MerchantInvitationStatus.REVOKED) {
      throw new ApiException(
          "Illegal invitation transition: only PENDING -> REVOKED is allowed",
          HttpStatus.CONFLICT);
    }
    invitation.setStatus(request.status());
    MerchantInvitation saved = merchantInvitationRepository.save(invitation);
    platformAuditService.recordForCurrentUser(
        "merchant.invitation.updated",
        "merchant_invitation",
        saved.getId(),
        "status=" + request.status());
    return new MerchantInvitationSummary(
        saved.getId(),
        saved.getMerchant().getId(),
        saved.getInviteCode(),
        saved.getInviteeUser().getUsername(),
        saved.getStatus(),
        saved.getExpiresAt());
  }

  @PutMapping("/{merchantId}/profile")
  public MerchantProfileResponse updateProfile(@PathVariable UUID merchantId, @RequestBody MerchantProfileResponse request) {
    assertMerchantScope(merchantId);
    Merchant merchant = bookingCommandService.ensureMerchant(merchantId);
    MerchantProfile profile = profileRepository.findByMerchantId(merchantId).orElseGet(() -> {
      MerchantProfile created = new MerchantProfile();
      created.setMerchant(merchant);
      return created;
    });
    profile.setDescription(request.description());
    profile.setLogoUrl(normalizeLogoImageData(request.logoUrl()));
    profile.setAddress(request.address());
    profile.setPhone(request.phone());
    profile.setEmail(request.email());
    profile.setWebsite(request.website());
    profile.setStoreCategory(request.storeCategory() == null ? null : request.storeCategory().trim());
    profile.setLineContactUrl(request.lineContactUrl() == null ? null : request.lineContactUrl().trim());
    MerchantProfile saved = profileRepository.save(profile);
    return new MerchantProfileResponse(
        saved.getDescription(),
        saved.getLogoUrl(),
        saved.getAddress(),
        saved.getPhone(),
        saved.getEmail(),
        saved.getWebsite(),
        saved.getStoreCategory(),
        saved.getLineContactUrl());
  }

  private String normalizeLogoImageData(String rawLogo) {
    return normalizeImageDataUrl(rawLogo, "logoUrl");
  }

  private String normalizeImageDataUrl(String rawImage, String fieldName) {
    if (rawImage == null) {
      return null;
    }
    String normalized = rawImage.trim();
    if (normalized.isEmpty()) {
      return null;
    }
    if (!normalized.startsWith("data:image/") || !normalized.contains(";base64,")) {
      throw new ApiException(fieldName + " must be a base64 data image URL", HttpStatus.BAD_REQUEST);
    }
    return normalized;
  }

  @GetMapping("/{merchantId}/services")
  public List<ServiceItemSummary> getServices(@PathVariable UUID merchantId) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    return serviceItemRepository.findByMerchantId(merchantId).stream()
        .map(
            s ->
                new ServiceItemSummary(
                    s.getId(),
                    s.getName(),
                    s.getDurationMinutes(),
                    s.getPrice(),
                    s.getCategory(),
                    s.getImageUrl(),
                    Boolean.TRUE.equals(s.getActive()),
                    countResourcesForService(merchantId, s.getId())))
        .toList();
  }

  @PostMapping("/{merchantId}/services")
  public ServiceItemSummary createService(@PathVariable UUID merchantId, @Valid @RequestBody ServiceItemRequest request) {
    assertMerchantScope(merchantId);
    Merchant merchant = bookingCommandService.ensureMerchant(merchantId);
    if (serviceItemRepository.findByMerchantId(merchantId).size() >= merchant.getServiceLimit()) {
      throw new ApiException("Service limit exceeded for this merchant");
    }
    ServiceItem item = new ServiceItem();
    item.setMerchant(merchant);
    item.setName(request.name());
    item.setCategory(request.category());
    item.setPrice(request.price());
    item.setDurationMinutes(request.durationMinutes());
    item.setImageUrl(normalizeImageDataUrl(request.imageUrl(), "imageUrl"));
    ServiceItem saved = serviceItemRepository.save(item);
    return new ServiceItemSummary(
        saved.getId(),
        saved.getName(),
        saved.getDurationMinutes(),
        saved.getPrice(),
        saved.getCategory(),
        saved.getImageUrl(),
        Boolean.TRUE.equals(saved.getActive()),
        countResourcesForService(merchantId, saved.getId()));
  }

  @PutMapping("/{merchantId}/services/{serviceId}")
  public ServiceItemSummary updateService(
      @PathVariable UUID merchantId, @PathVariable UUID serviceId, @Valid @RequestBody ServiceItemRequest request) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    ServiceItem item = serviceItemRepository.findByIdAndMerchantId(serviceId, merchantId)
        .orElseThrow(() -> new ApiException("Service not found"));
    item.setName(request.name());
    item.setCategory(request.category());
    item.setPrice(request.price());
    item.setDurationMinutes(request.durationMinutes());
    item.setImageUrl(normalizeImageDataUrl(request.imageUrl(), "imageUrl"));
    ServiceItem saved = serviceItemRepository.save(item);
    return new ServiceItemSummary(
        saved.getId(),
        saved.getName(),
        saved.getDurationMinutes(),
        saved.getPrice(),
        saved.getCategory(),
        saved.getImageUrl(),
        Boolean.TRUE.equals(saved.getActive()),
        countResourcesForService(merchantId, saved.getId()));
  }

  @PatchMapping("/{merchantId}/services/{serviceId}/active")
  public ServiceItemSummary toggleServiceActive(
      @PathVariable UUID merchantId, @PathVariable UUID serviceId, @RequestParam boolean active) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    ServiceItem item =
        serviceItemRepository
            .findByIdAndMerchantId(serviceId, merchantId)
            .orElseThrow(() -> new ApiException("Service not found", HttpStatus.NOT_FOUND));
    item.setActive(active);
    ServiceItem saved = serviceItemRepository.save(item);
    return new ServiceItemSummary(
        saved.getId(),
        saved.getName(),
        saved.getDurationMinutes(),
        saved.getPrice(),
        saved.getCategory(),
        saved.getImageUrl(),
        Boolean.TRUE.equals(saved.getActive()),
        countResourcesForService(merchantId, saved.getId()));
  }

  @PostMapping("/{merchantId}/services/{serviceId}/clone")
  public ServiceItemSummary cloneService(
      @PathVariable UUID merchantId,
      @PathVariable UUID serviceId,
      @Valid @RequestBody(required = false) ServiceCloneRequest request) {
    assertMerchantScope(merchantId);
    Merchant merchant = bookingCommandService.ensureMerchant(merchantId);
    ServiceItem source =
        serviceItemRepository
            .findByIdAndMerchantId(serviceId, merchantId)
            .orElseThrow(() -> new ApiException("Service not found", HttpStatus.NOT_FOUND));
    String suffix = request == null || request.nameSuffix() == null || request.nameSuffix().isBlank()
        ? " (Copy)"
        : " " + request.nameSuffix().trim();
    ServiceItem clone = new ServiceItem();
    clone.setMerchant(merchant);
    clone.setName(source.getName() + suffix);
    clone.setCategory(source.getCategory());
    clone.setDurationMinutes(source.getDurationMinutes());
    clone.setPrice(source.getPrice());
    clone.setImageUrl(source.getImageUrl());
    clone.setActive(source.getActive());
    ServiceItem saved = serviceItemRepository.save(clone);
    return new ServiceItemSummary(
        saved.getId(),
        saved.getName(),
        saved.getDurationMinutes(),
        saved.getPrice(),
        saved.getCategory(),
        saved.getImageUrl(),
        Boolean.TRUE.equals(saved.getActive()),
        0L);
  }

  @DeleteMapping("/{merchantId}/services/{serviceId}")
  public void deleteService(@PathVariable UUID merchantId, @PathVariable UUID serviceId) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    ServiceItem item = serviceItemRepository.findByIdAndMerchantId(serviceId, merchantId).orElse(null);
    if (item == null) {
      // Idempotent delete: if the row is already removed/archived in this scope, treat as success.
      return;
    }
    long bookingHistoryCount = bookingRepository.countByMerchantIdAndServiceItemId(merchantId, serviceId);
    if (bookingHistoryCount == 0) {
      serviceItemRepository.delete(item);
      return;
    }

    // Keep FK integrity for booking history while allowing operators to "remove" the service from active management.
    String currentName = item.getName() == null ? "" : item.getName().trim();
    String baseName = currentName.isEmpty() ? "Service" : currentName;
    item.setName(toArchivedServiceName(baseName));
    item.setCategory("archived");
    item.setDurationMinutes(Math.max(1, item.getDurationMinutes() == null ? 1 : item.getDurationMinutes()));
    item.setPrice(item.getPrice() == null ? BigDecimal.ZERO : item.getPrice());
    item.setImageUrl(null);
    serviceItemRepository.save(item);
  }

  private String toArchivedServiceName(String baseName) {
    final String prefix = "[REMOVED] ";
    final int maxLength = 120;
    String normalized = baseName == null ? "Service" : baseName.trim();
    if (normalized.isEmpty()) {
      normalized = "Service";
    }
    if (normalized.startsWith(prefix)) {
      return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }
    int allowedBaseLength = maxLength - prefix.length();
    if (normalized.length() > allowedBaseLength) {
      normalized = normalized.substring(0, allowedBaseLength);
    }
    return prefix + normalized;
  }

  @GetMapping("/{merchantId}/business-hours")
  public List<BusinessHoursSummary> getBusinessHours(@PathVariable UUID merchantId) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    return businessHoursRepository.findByMerchantId(merchantId).stream()
        .map(h -> new BusinessHoursSummary(h.getId(), h.getDayOfWeek(), h.getStartTime(), h.getEndTime()))
        .toList();
  }

  @PutMapping("/{merchantId}/business-hours")
  @Transactional
  public List<BusinessHoursSummary> replaceBusinessHours(
      @PathVariable UUID merchantId, @RequestBody(required = false) List<@Valid BusinessHoursRequest> request) {
    assertMerchantScope(merchantId);
    Merchant merchant = bookingCommandService.ensureMerchant(merchantId);
    List<BusinessHoursRequest> incoming = request == null ? List.of() : request;
    for (BusinessHoursRequest item : incoming) {
      if (!item.endTime().isAfter(item.startTime())) {
        throw new ApiException("endTime must be after startTime");
      }
    }
    businessHoursRepository.deleteByMerchantId(merchantId);
    List<BusinessHours> rows = incoming.stream().map(item -> {
      BusinessHours h = new BusinessHours();
      h.setMerchant(merchant);
      h.setDayOfWeek(item.dayOfWeek());
      h.setStartTime(item.startTime());
      h.setEndTime(item.endTime());
      return h;
    }).toList();
    return businessHoursRepository.saveAll(rows).stream()
        .map(h -> new BusinessHoursSummary(h.getId(), h.getDayOfWeek(), h.getStartTime(), h.getEndTime()))
        .toList();
  }

  @GetMapping("/{merchantId}/bookings")
  public List<MerchantBookingSummary> getBookings(
      @PathVariable UUID merchantId,
      @RequestParam(required = false) BookingStatus status) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    if (status != null) {
      return bookingRepository.findByMerchantIdAndStatusOrderByStartAtAsc(merchantId, status).stream()
          .map(b -> new MerchantBookingSummary(
              b.getId(),
              b.getServiceItem().getId(),
              b.getStartAt(),
              b.getEndAt(),
              b.getCustomerName(),
              b.getCustomerContact(),
              b.getStatus()))
          .toList();
    }
    return bookingRepository.findByMerchantIdOrderByStartAtAsc(merchantId).stream()
        .map(b -> new MerchantBookingSummary(
            b.getId(),
            b.getServiceItem().getId(),
            b.getStartAt(),
            b.getEndAt(),
            b.getCustomerName(),
            b.getCustomerContact(),
            b.getStatus()))
        .toList();
  }

  @GetMapping("/{merchantId}/customization")
  public MerchantCustomizationResponse getCustomization(@PathVariable UUID merchantId) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    CustomizationConfig config = customizationConfigRepository.findByMerchantId(merchantId)
        .orElseGet(() -> {
          CustomizationConfig created = new CustomizationConfig();
          created.setMerchant(bookingCommandService.ensureMerchant(merchantId));
          return customizationConfigRepository.save(created);
        });
    return toCustomizationResponse(config);
  }

  @PutMapping("/{merchantId}/customization")
  public MerchantCustomizationResponse updateCustomization(@PathVariable UUID merchantId, @Valid @RequestBody CustomizationRequest request) {
    assertMerchantScope(merchantId);
    Merchant merchant = bookingCommandService.ensureMerchant(merchantId);
    CustomizationConfig config = customizationConfigRepository.findByMerchantId(merchantId)
        .orElseGet(() -> {
          CustomizationConfig created = new CustomizationConfig();
          created.setMerchant(merchant);
          return created;
        });
    config.setThemeColor(request.themeColor());
    config.setThemePreset(request.themePreset());
    config.setHeroTitle(request.heroTitle());
    config.setBookingFlowText(request.bookingFlowText());
    config.setInviteCode(request.inviteCode());
    config.setTermsText(request.termsText());
    config.setAnnouncementText(request.announcementText());
    config.setFaqJson(request.faqJson());
    config.setBufferMinutes(request.bufferMinutes());
    config.setHomepageSectionsJson(request.homepageSectionsJson());
    config.setCategoryOrderJson(request.categoryOrderJson());
    config.setNotificationNewBooking(request.notificationNewBooking());
    config.setNotificationCancellation(request.notificationCancellation());
    config.setNotificationDailySummary(request.notificationDailySummary());
    return toCustomizationResponse(customizationConfigRepository.save(config));
  }

  @GetMapping("/{merchantId}/dynamic-fields")
  public List<DynamicFieldSummary> getDynamicFields(@PathVariable UUID merchantId) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    return dynamicFieldConfigRepository.findByMerchantId(merchantId).stream()
        .map(f -> new DynamicFieldSummary(f.getId(), f.getLabel(), f.getType(), f.getRequiredField(), f.getOptionsJson()))
        .toList();
  }

  @PostMapping("/{merchantId}/dynamic-fields")
  public DynamicFieldSummary createDynamicField(@PathVariable UUID merchantId, @Valid @RequestBody DynamicFieldRequest request) {
    assertMerchantScope(merchantId);
    Merchant merchant = bookingCommandService.ensureMerchant(merchantId);
    DynamicFieldConfig config = new DynamicFieldConfig();
    config.setMerchant(merchant);
    config.setLabel(request.label());
    config.setType(request.type());
    config.setRequiredField(request.requiredField());
    config.setOptionsJson(request.optionsJson());
    DynamicFieldConfig saved = dynamicFieldConfigRepository.save(config);
    return new DynamicFieldSummary(saved.getId(), saved.getLabel(), saved.getType(), saved.getRequiredField(), saved.getOptionsJson());
  }

  @DeleteMapping("/{merchantId}/dynamic-fields/{fieldId}")
  public void deleteDynamicField(@PathVariable UUID merchantId, @PathVariable Long fieldId) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    DynamicFieldConfig field = dynamicFieldConfigRepository.findByIdAndMerchantId(fieldId, merchantId)
        .orElseThrow(() -> new ApiException("Dynamic field not found"));
    dynamicFieldConfigRepository.delete(field);
  }

  @GetMapping("/{merchantId}/resources")
  public MerchantResourceListResponse getResources(
      @PathVariable UUID merchantId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) ResourceOperationalStatus status) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 200));
    Page<ResourceItem> resourcePage =
        status == ResourceOperationalStatus.MAINTENANCE
            ? resourceItemRepository.findByMerchantIdAndMaintenance(merchantId, true, pageable)
            : resourceItemRepository.findByMerchantId(merchantId, pageable);
    var serviceById = resourceItemMediaResolver.indexById(serviceItemRepository.findByMerchantId(merchantId));
    List<ResourceItemSummary> items =
        resourcePage.getContent().stream()
            .map(
                r -> {
                  ResourceOperationalStatus computedStatus = resolveResourceStatus(merchantId, r);
                  if (status != null && computedStatus != status) {
                    return null;
                  }
                  return resourceItemMediaResolver.toSummary(r, merchantId, serviceById, computedStatus);
                })
            .filter(java.util.Objects::nonNull)
            .toList();
    long effectiveTotal;
    if (status == null) {
      effectiveTotal = resourcePage.getTotalElements();
    } else if (status == ResourceOperationalStatus.MAINTENANCE) {
      effectiveTotal = resourcePage.getTotalElements();
    } else {
      effectiveTotal =
          resourceItemRepository.findByMerchantId(merchantId).stream()
              .map(resource -> resolveResourceStatus(merchantId, resource))
              .filter(computed -> computed == status)
              .count();
    }
    return new MerchantResourceListResponse(items, page, size, effectiveTotal);
  }

  @PostMapping("/{merchantId}/resources")
  public ResourceItemSummary createResource(@PathVariable UUID merchantId, @Valid @RequestBody ResourceRequest request) {
    assertMerchantScope(merchantId);
    Merchant merchant = bookingCommandService.ensureMerchant(merchantId);
    ResourceItem item = new ResourceItem();
    item.setMerchant(merchant);
    item.setName(request.name());
    item.setType(request.type());
    item.setCategory(request.category() == null ? "GENERAL" : request.category());
    item.setCapacity(request.capacity() == null ? 1 : request.capacity());
    item.setActive(request.active() == null ? Boolean.TRUE : request.active());
    item.setServiceItemsJson(request.serviceItemsJson());
    item.setAssignedStaffIdsJson(encodeAssignedStaffIds(merchantId, request.assignedStaffIds()));
    item.setPrice(request.price());
    ResourceItem saved = resourceItemRepository.save(item);
    var serviceById = resourceItemMediaResolver.indexById(serviceItemRepository.findByMerchantId(merchantId));
    return resourceItemMediaResolver.toSummary(
        saved, merchantId, serviceById, resolveResourceStatus(merchantId, saved));
  }

  @PatchMapping("/{merchantId}/resources/{resourceId}")
  public ResourceItemSummary updateResource(
      @PathVariable UUID merchantId,
      @PathVariable UUID resourceId,
      @Valid @RequestBody ResourceUpdateRequest request) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    ResourceItem item =
        resourceItemRepository
            .findByIdAndMerchantId(resourceId, merchantId)
            .orElseThrow(() -> new ApiException("Resource not found", HttpStatus.NOT_FOUND));
    if (request.name() != null) {
      String name = request.name().trim();
      if (name.isEmpty()) {
        throw new ApiException("Resource name must not be blank", HttpStatus.BAD_REQUEST);
      }
      item.setName(name);
    }
    if (request.type() != null) {
      String type = request.type().trim();
      if (type.isEmpty()) {
        throw new ApiException("Resource type must not be blank", HttpStatus.BAD_REQUEST);
      }
      item.setType(type.toUpperCase(Locale.ROOT));
    }
    if (request.category() != null) {
      String category = request.category().trim();
      item.setCategory(category.isEmpty() ? "GENERAL" : category.toUpperCase(Locale.ROOT));
    }
    if (request.capacity() != null) {
      item.setCapacity(request.capacity());
    }
    if (request.active() != null) {
      item.setActive(request.active());
    }
    if (request.serviceItemsJson() != null) {
      item.setServiceItemsJson(request.serviceItemsJson().trim().isEmpty() ? "[]" : request.serviceItemsJson());
    }
    if (request.assignedStaffIds() != null) {
      item.setAssignedStaffIdsJson(encodeAssignedStaffIds(merchantId, request.assignedStaffIds()));
    }
    if (request.price() != null) {
      item.setPrice(request.price());
    }
    ResourceItem saved = resourceItemRepository.save(item);
    var serviceById = resourceItemMediaResolver.indexById(serviceItemRepository.findByMerchantId(merchantId));
    return resourceItemMediaResolver.toSummary(
        saved, merchantId, serviceById, resolveResourceStatus(merchantId, saved));
  }

  @PatchMapping("/{merchantId}/resources/batch/price")
  @Transactional
  public MerchantResourceListResponse batchUpdateResourcePrice(
      @PathVariable UUID merchantId, @Valid @RequestBody ResourceBatchPriceRequest request) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    List<ResourceItem> resources =
        resourceItemRepository.findAllById(request.resourceIds()).stream()
            .filter(item -> item.getMerchant().getId().equals(merchantId))
            .toList();
    if (resources.size() != request.resourceIds().size()) {
      throw new ApiException("Some resources are out of merchant scope", HttpStatus.FORBIDDEN);
    }
    resources.forEach(item -> item.setPrice(request.price()));
    resourceItemRepository.saveAll(resources);
    var serviceById = resourceItemMediaResolver.indexById(serviceItemRepository.findByMerchantId(merchantId));
    List<ResourceItemSummary> items =
        resources.stream()
            .map(r -> resourceItemMediaResolver.toSummary(r, merchantId, serviceById, resolveResourceStatus(merchantId, r)))
            .toList();
    return new MerchantResourceListResponse(items, 0, items.size(), items.size());
  }

  @PatchMapping("/{merchantId}/resources/batch/status")
  @Transactional
  public MerchantResourceListResponse batchUpdateResourceStatus(
      @PathVariable UUID merchantId, @Valid @RequestBody ResourceBatchStatusRequest request) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    if (request.status() == ResourceOperationalStatus.FULLY_BOOKED) {
      throw new ApiException("FULLY_BOOKED is derived and cannot be set manually", HttpStatus.CONFLICT);
    }
    List<ResourceItem> resources =
        resourceItemRepository.findAllById(request.resourceIds()).stream()
            .filter(item -> item.getMerchant().getId().equals(merchantId))
            .toList();
    if (resources.size() != request.resourceIds().size()) {
      throw new ApiException("Some resources are out of merchant scope", HttpStatus.FORBIDDEN);
    }
    resources.forEach(
        item -> {
          boolean maintenance = request.status() == ResourceOperationalStatus.MAINTENANCE;
          item.setMaintenance(maintenance);
          item.setActive(!maintenance);
        });
    resourceItemRepository.saveAll(resources);
    var serviceById = resourceItemMediaResolver.indexById(serviceItemRepository.findByMerchantId(merchantId));
    List<ResourceItemSummary> items =
        resources.stream()
            .map(r -> resourceItemMediaResolver.toSummary(r, merchantId, serviceById, resolveResourceStatus(merchantId, r)))
            .toList();
    return new MerchantResourceListResponse(items, 0, items.size(), items.size());
  }

  @PutMapping("/{merchantId}/resources/batch/business-hours")
  @Transactional
  public MerchantResourceListResponse batchUpdateResourceBusinessHours(
      @PathVariable UUID merchantId, @Valid @RequestBody ResourceBatchBusinessHoursRequest request) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    List<ResourceItem> resources =
        resourceItemRepository.findAllById(request.resourceIds()).stream()
            .filter(item -> item.getMerchant().getId().equals(merchantId))
            .toList();
    if (resources.size() != request.resourceIds().size()) {
      throw new ApiException("Some resources are out of merchant scope", HttpStatus.FORBIDDEN);
    }
    resources.forEach(item -> item.setBusinessHoursJson(request.businessHoursJson()));
    resourceItemRepository.saveAll(resources);
    var serviceById = resourceItemMediaResolver.indexById(serviceItemRepository.findByMerchantId(merchantId));
    List<ResourceItemSummary> items =
        resources.stream()
            .map(r -> resourceItemMediaResolver.toSummary(r, merchantId, serviceById, resolveResourceStatus(merchantId, r)))
            .toList();
    return new MerchantResourceListResponse(items, 0, items.size(), items.size());
  }

  @DeleteMapping("/{merchantId}/resources/{resourceId}")
  public void deleteResource(@PathVariable UUID merchantId, @PathVariable UUID resourceId) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    ResourceItem item = resourceItemRepository.findByIdAndMerchantId(resourceId, merchantId)
        .orElseThrow(() -> new ApiException("Resource not found"));
    List<UUID> scopedServiceIds = parseServiceItemIds(item.getServiceItemsJson());
    if (!scopedServiceIds.isEmpty()) {
      long blockingCount =
          bookingRepository.countByMerchantIdAndServiceItemIdInAndStatusIn(
              merchantId, scopedServiceIds, listBlockingStatuses());
      if (blockingCount > 0) {
        throw new ApiException(
            "Resource has unfinished bookings and cannot be deleted", HttpStatus.CONFLICT);
      }
    }
    resourceItemRepository.delete(item);
  }

  private List<UUID> parseServiceItemIds(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    try {
      List<UUID> parsed = objectMapper.readValue(raw, new TypeReference<List<UUID>>() {});
      return parsed.stream().filter(id -> id != null).distinct().toList();
    } catch (Exception ex) {
      return List.of();
    }
  }

  private String encodeAssignedStaffIds(UUID merchantId, List<UUID> rawStaffIds) {
    if (rawStaffIds == null || rawStaffIds.isEmpty()) {
      return "[]";
    }
    List<UUID> normalized =
        rawStaffIds.stream()
            .filter(id -> id != null)
            .collect(
                java.util.stream.Collectors.collectingAndThen(
                    java.util.stream.Collectors.toCollection(LinkedHashSet::new), List::copyOf));
    if (normalized.isEmpty()) {
      return "[]";
    }

    List<TeamMember> activeMembers =
        teamMemberRepository.findByMerchantIdAndPlatformUserIdInAndStatus(
            merchantId, normalized, TeamMemberStatus.ACTIVE);
    Set<UUID> activeUserIds =
        activeMembers.stream().map(member -> member.getPlatformUser().getId()).collect(java.util.stream.Collectors.toSet());
    List<UUID> invalidIds =
        normalized.stream().filter(id -> !activeUserIds.contains(id)).toList();
    if (!invalidIds.isEmpty()) {
      throw new ApiException(
          "assignedStaffIds must be active team members in this merchant: " + invalidIds,
          HttpStatus.BAD_REQUEST);
    }

    try {
      return objectMapper.writeValueAsString(normalized);
    } catch (Exception ex) {
      throw new ApiException("Failed to encode assignedStaffIds", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private List<BookingStatus> listBlockingStatuses() {
    Set<BookingStatus> terminalStatuses =
        EnumSet.of(
            BookingStatus.CANCELLED,
            BookingStatus.COMPLETED,
            BookingStatus.EXPIRED,
            BookingStatus.REFUNDED,
            BookingStatus.NO_SHOW);
    return java.util.Arrays.stream(BookingStatus.values())
        .filter(status -> !terminalStatuses.contains(status))
        .toList();
  }

  private long countResourcesForService(UUID merchantId, UUID serviceId) {
    return resourceItemRepository.findByMerchantId(merchantId).stream()
        .filter(item -> parseServiceItemIds(item.getServiceItemsJson()).contains(serviceId))
        .count();
  }

  private ResourceOperationalStatus resolveResourceStatus(UUID merchantId, ResourceItem resource) {
    if (Boolean.TRUE.equals(resource.getMaintenance())) {
      return ResourceOperationalStatus.MAINTENANCE;
    }
    List<UUID> scopedServiceIds = parseServiceItemIds(resource.getServiceItemsJson());
    if (!scopedServiceIds.isEmpty()) {
      List<Booking> activeBookings =
          bookingRepository.findByMerchantIdAndServiceItemIdInAndStatusNotAndStartAtLessThanAndEndAtGreaterThan(
              merchantId,
              scopedServiceIds,
              BookingStatus.CANCELLED,
              LocalDateTime.now(),
              LocalDateTime.now());
      if ((long) activeBookings.size() >= Math.max(1, resource.getCapacity() == null ? 1 : resource.getCapacity())) {
        return ResourceOperationalStatus.FULLY_BOOKED;
      }
    }
    return ResourceOperationalStatus.ACTIVE;
  }

  @GetMapping("/{merchantId}/availability-exceptions")
  public List<AvailabilityExceptionSummary> getAvailabilityExceptions(@PathVariable UUID merchantId) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    return availabilityExceptionRepository.findByMerchantId(merchantId).stream()
        .map(ex -> new AvailabilityExceptionSummary(ex.getId(), ex.getType(), ex.getStartAt(), ex.getEndAt(), ex.getReason()))
        .toList();
  }

  @PostMapping("/{merchantId}/availability-exceptions")
  public AvailabilityExceptionSummary createAvailabilityException(
      @PathVariable UUID merchantId, @Valid @RequestBody AvailabilityExceptionRequest request) {
    assertMerchantScope(merchantId);
    Merchant merchant = bookingCommandService.ensureMerchant(merchantId);
    AvailabilityException ex = new AvailabilityException();
    ex.setMerchant(merchant);
    ex.setType(request.type());
    ex.setStartAt(request.startAt());
    ex.setEndAt(request.endAt());
    ex.setReason(request.reason());
    AvailabilityException saved = availabilityExceptionRepository.save(ex);
    return new AvailabilityExceptionSummary(saved.getId(), saved.getType(), saved.getStartAt(), saved.getEndAt(), saved.getReason());
  }

  @DeleteMapping("/{merchantId}/availability-exceptions/{exceptionId}")
  public void deleteAvailabilityException(@PathVariable UUID merchantId, @PathVariable Long exceptionId) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    AvailabilityException ex = availabilityExceptionRepository.findById(exceptionId)
        .orElseThrow(() -> new ApiException("Exception not found"));
    if (!ex.getMerchant().getId().equals(merchantId)) {
      throw new ApiException("Exception not in this merchant");
    }
    availabilityExceptionRepository.delete(ex);
  }

  @PutMapping("/{merchantId}/bookings/{bookingId}/status")
  public MerchantBookingSummary updateBookingStatus(
      @PathVariable UUID merchantId,
      @PathVariable UUID bookingId,
      @Valid @RequestBody MerchantBookingStatusUpdateRequest request) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    BookingTransitionEvent event = toTransitionEvent(request.status());
    Booking booking = bookingCommandService.transitionBooking(merchantId, bookingId, event);
    return new MerchantBookingSummary(
        booking.getId(),
        booking.getServiceItem().getId(),
        booking.getStartAt(),
        booking.getEndAt(),
        booking.getCustomerName(),
        booking.getCustomerContact(),
        booking.getStatus());
  }

  @PostMapping("/{merchantId}/bookings")
  public MerchantBookingSummary createManualBooking(@PathVariable UUID merchantId, @Valid @RequestBody ManualBookingRequest request) {
    assertMerchantScope(merchantId);
    Merchant merchant = bookingCommandService.ensureMerchant(merchantId);
    ServiceItem service = serviceItemRepository.findByIdAndMerchantId(request.serviceItemId(), merchant.getId())
        .orElseThrow(() -> new ApiException("Service not found"));
    Booking booking = bookingCommandService.createBookingWithValidation(merchant, service, request.startAt(), request.customerName(), request.customerContact());
    return new MerchantBookingSummary(
        booking.getId(),
        booking.getServiceItem().getId(),
        booking.getStartAt(),
        booking.getEndAt(),
        booking.getCustomerName(),
        booking.getCustomerContact(),
        booking.getStatus());
  }

  @PostMapping("/{merchantId}/bookings/{bookingId}/assign")
  public BookingAssignmentSummary assignBooking(
      @PathVariable UUID merchantId,
      @PathVariable UUID bookingId,
      @Valid @RequestBody BookingAssignmentCommandRequest request) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    return merchantBookingAssignmentService.assign(merchantId, bookingId, request);
  }

  @PostMapping("/{merchantId}/bookings/{bookingId}/reassign")
  public BookingAssignmentSummary reassignBooking(
      @PathVariable UUID merchantId,
      @PathVariable UUID bookingId,
      @Valid @RequestBody BookingAssignmentCommandRequest request) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    return merchantBookingAssignmentService.reassign(merchantId, bookingId, request);
  }

  @PostMapping("/{merchantId}/bookings/{bookingId}/release")
  public BookingAssignmentSummary releaseBooking(
      @PathVariable UUID merchantId,
      @PathVariable UUID bookingId,
      @Valid @RequestBody BookingAssignmentCommandRequest request) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    return merchantBookingAssignmentService.release(merchantId, bookingId, request);
  }

  @GetMapping("/{merchantId}/resources/{resourceId}/staff-candidates")
  public List<StaffCandidateSummary> listStaffCandidates(
      @PathVariable UUID merchantId,
      @PathVariable UUID resourceId,
      @RequestParam LocalDateTime startAt,
      @RequestParam LocalDateTime endAt) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    return merchantBookingAssignmentService.listStaffCandidates(merchantId, resourceId, startAt, endAt);
  }

  @GetMapping("/{merchantId}/teams")
  public List<TeamSummary> listTeams(@PathVariable UUID merchantId) {
    assertMerchantScope(merchantId);
    bookingCommandService.ensureMerchant(merchantId);
    return serviceTeamRepository.findByMerchantIdOrderByIdDesc(merchantId).stream()
        .map(this::toTeamSummary)
        .toList();
  }

  @PostMapping("/{merchantId}/teams")
  public TeamSummary createTeam(
      @PathVariable UUID merchantId, @Valid @RequestBody TeamCreateRequest request) {
    assertMerchantScope(merchantId);
    Merchant merchant = bookingCommandService.ensureMerchant(merchantId);
    serviceTeamRepository
        .findByMerchantIdAndCode(merchantId, request.code().trim())
        .ifPresent(
            existing -> {
              throw new ApiException("Team code already exists", HttpStatus.CONFLICT);
            });
    ServiceTeam team = new ServiceTeam();
    team.setMerchant(merchant);
    team.setName(request.name().trim());
    team.setCode(request.code().trim());
    team.setStatus(request.status() == null ? ServiceTeamStatus.ACTIVE : request.status());
    ServiceTeam saved = serviceTeamRepository.save(team);
    return toTeamSummary(saved);
  }

  @PutMapping("/{merchantId}/teams/{teamId}")
  public TeamSummary updateTeam(
      @PathVariable UUID merchantId,
      @PathVariable Long teamId,
      @Valid @RequestBody TeamUpdateRequest request) {
    assertMerchantScope(merchantId);
    ServiceTeam team = requireTeamInMerchant(merchantId, teamId);
    team.setName(request.name().trim());
    if (request.status() != null) {
      team.setStatus(request.status());
    }
    return toTeamSummary(serviceTeamRepository.save(team));
  }

  @GetMapping("/{merchantId}/teams/{teamId}/members")
  public List<TeamMemberSummary> listTeamMembers(
      @PathVariable UUID merchantId, @PathVariable Long teamId) {
    assertMerchantScope(merchantId);
    requireTeamInMerchant(merchantId, teamId);
    return teamMemberRepository.findByMerchantIdAndTeamIdOrderByIdAsc(merchantId, teamId).stream()
        .map(this::toTeamMemberSummary)
        .toList();
  }

  @PostMapping("/{merchantId}/teams/{teamId}/members")
  public TeamMemberSummary assignTeamMember(
      @PathVariable UUID merchantId,
      @PathVariable Long teamId,
      @Valid @RequestBody TeamMemberAssignRequest request) {
    assertMerchantScope(merchantId);
    ServiceTeam team = requireTeamInMerchant(merchantId, teamId);
    PlatformUser user =
        platformUserRepository
            .findById(request.userId())
            .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    if (!merchantAccessService.hasStrictActiveMembership(merchantId, user.getId())) {
      throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
    }

    TeamMember member =
        teamMemberRepository
            .findByMerchantIdAndTeamIdAndPlatformUserId(merchantId, teamId, request.userId())
            .orElseGet(TeamMember::new);
    member.setMerchant(team.getMerchant());
    member.setTeam(team);
    member.setPlatformUser(user);
    member.setRole(request.role().trim());
    member.setStatus(request.status() == null ? TeamMemberStatus.ACTIVE : request.status());
    return toTeamMemberSummary(teamMemberRepository.save(member));
  }

  @DeleteMapping("/{merchantId}/teams/{teamId}/members/{memberId}")
  public void removeTeamMember(
      @PathVariable UUID merchantId, @PathVariable Long teamId, @PathVariable Long memberId) {
    assertMerchantScope(merchantId);
    ServiceTeam team = requireTeamInMerchant(merchantId, teamId);
    TeamMember member =
        teamMemberRepository
            .findById(memberId)
            .orElseThrow(() -> new ApiException("Team member not found", HttpStatus.NOT_FOUND));
    if (!member.getMerchant().getId().equals(merchantId) || !member.getTeam().getId().equals(team.getId())) {
      throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
    }
    teamMemberRepository.delete(member);
  }

  private ServiceTeam requireTeamInMerchant(UUID merchantId, Long teamId) {
    ServiceTeam team =
        serviceTeamRepository
            .findById(teamId)
            .orElseThrow(() -> new ApiException("Team not found", HttpStatus.NOT_FOUND));
    if (!team.getMerchant().getId().equals(merchantId)) {
      throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
    }
    return team;
  }

  private TeamSummary toTeamSummary(ServiceTeam team) {
    return new TeamSummary(
        team.getId(),
        team.getMerchant().getId(),
        team.getName(),
        team.getCode(),
        team.getStatus(),
        team.getCreatedAt());
  }

  private TeamMemberSummary toTeamMemberSummary(TeamMember member) {
    return new TeamMemberSummary(
        member.getId(),
        member.getMerchant().getId(),
        member.getTeam().getId(),
        member.getPlatformUser().getId(),
        member.getPlatformUser().getUsername(),
        member.getRole(),
        member.getStatus());
  }

  private MerchantCustomizationResponse toCustomizationResponse(CustomizationConfig config) {
    return new MerchantCustomizationResponse(
        config.getThemePreset(),
        config.getThemeColor(),
        config.getHeroTitle(),
        config.getBookingFlowText(),
        config.getInviteCode(),
        config.getTermsText(),
        config.getAnnouncementText(),
        config.getFaqJson(),
        config.getBufferMinutes(),
        config.getHomepageSectionsJson(),
        config.getCategoryOrderJson(),
        Boolean.TRUE.equals(config.getNotificationNewBooking()),
        Boolean.TRUE.equals(config.getNotificationCancellation()),
        Boolean.TRUE.equals(config.getNotificationDailySummary()));
  }

  private BookingTransitionEvent toTransitionEvent(BookingStatus status) {
    return switch (status) {
      case CHECKED_IN -> BookingTransitionEvent.CHECK_IN;
      case COMPLETED -> BookingTransitionEvent.COMPLETE;
      case CANCELLED -> BookingTransitionEvent.CANCEL;
      case NO_SHOW -> BookingTransitionEvent.MARK_NO_SHOW;
      default -> throw new ApiException("Unsupported status transition: " + status);
    };
  }
}
