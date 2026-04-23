package com.bookingcore.api;

import java.util.UUID;
import com.bookingcore.api.ApiDtos.AvailabilityResponse;
import com.bookingcore.api.ApiDtos.ApiEnvelope;
import com.bookingcore.api.ApiDtos.BookingLockRequest;
import com.bookingcore.api.ApiDtos.BookingLockResponse;
import com.bookingcore.api.ApiDtos.ClientBookingCreateRequest;
import com.bookingcore.api.ApiDtos.ClientBookingCreateResponse;
import com.bookingcore.api.ApiDtos.ClientBookingCancelRequest;
import com.bookingcore.api.ApiDtos.ClientBookingListResponse;
import com.bookingcore.api.ApiDtos.ClientBookingRescheduleRequest;
import com.bookingcore.api.ApiDtos.ClientBookingStatusResponse;
import com.bookingcore.api.ApiDtos.ClientCatalogResourceSummary;
import com.bookingcore.api.ApiDtos.ClientCategorySummary;
import com.bookingcore.api.ApiDtos.ClientJoinMerchantByCodeRequest;
import com.bookingcore.api.ApiDtos.ClientJoinedMerchantSummary;
import com.bookingcore.api.ApiDtos.CustomizationSummary;
import com.bookingcore.api.ApiDtos.ClientMerchantCardSummary;
import com.bookingcore.api.ApiDtos.ClientMerchantResponse;
import com.bookingcore.api.ApiDtos.ClientProfileResponse;
import com.bookingcore.api.ApiDtos.ClientProfileUpdateRequest;
import com.bookingcore.api.ApiDtos.ClientPasswordUpdateRequest;
import com.bookingcore.api.ApiDtos.ClientPasswordUpdateResponse;
import com.bookingcore.api.ApiDtos.ClientProfilePreferencesResponse;
import com.bookingcore.api.ApiDtos.ClientProfilePreferencesUpdateRequest;
import com.bookingcore.api.ApiDtos.ClientResourceDetailResponse;
import com.bookingcore.api.ApiDtos.ClientResourceAvailabilityResponse;
import com.bookingcore.api.ApiDtos.ResourceOperationalStatus;
import com.bookingcore.api.ApiDtos.DynamicFieldSummary;
import com.bookingcore.api.ApiDtos.MerchantProfileSummary;
import com.bookingcore.api.ApiDtos.MerchantSummary;
import com.bookingcore.api.ApiDtos.PublicBookingRequest;
import com.bookingcore.api.ApiDtos.PublicBookingResponse;
import com.bookingcore.api.ApiDtos.ServiceItemSummary;
import com.bookingcore.api.ApiDtos.StorefrontResponse;
import com.bookingcore.api.support.ResourceItemMediaResolver;
import com.bookingcore.common.ApiException;
import com.bookingcore.modules.booking.Booking;
import com.bookingcore.modules.customization.CustomizationConfig;
import com.bookingcore.modules.customization.CustomizationConfigRepository;
import com.bookingcore.modules.client.ClientProfile;
import com.bookingcore.modules.client.ClientProfileRepository;
import com.bookingcore.modules.merchant.DynamicFieldConfigRepository;
import com.bookingcore.modules.merchant.MerchantMembershipRepository;
import com.bookingcore.modules.merchant.MerchantMembershipStatus;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.MerchantProfile;
import com.bookingcore.modules.merchant.MerchantProfileRepository;
import com.bookingcore.modules.merchant.MerchantRepository;
import com.bookingcore.modules.merchant.ResourceItem;
import com.bookingcore.modules.merchant.MerchantVisibility;
import com.bookingcore.modules.merchant.ResourceItemRepository;
import com.bookingcore.modules.service.ServiceItem;
import com.bookingcore.modules.service.ServiceItemRepository;
import com.bookingcore.service.BookingCommandService;
import com.bookingcore.service.ClientBookingService;
import com.bookingcore.service.ClientBookingService.ClientCatalogResourcesPage;
import com.bookingcore.service.ClientAvailabilityService;
import com.bookingcore.service.ClientProfileService;
import com.bookingcore.service.MerchantAccessService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/client")
@Tag(name = "Client")
public class ClientBookingController {
  private final MerchantRepository merchantRepository;
  private final MerchantProfileRepository profileRepository;
  private final ServiceItemRepository serviceItemRepository;
  private final CustomizationConfigRepository customizationConfigRepository;
  private final ResourceItemRepository resourceItemRepository;
  private final DynamicFieldConfigRepository dynamicFieldConfigRepository;
  private final MerchantMembershipRepository merchantMembershipRepository;
  private final ClientProfileRepository clientProfileRepository;
  private final BookingCommandService bookingCommandService;
  private final ClientAvailabilityService clientAvailabilityService;
  private final MerchantAccessService merchantAccessService;
  private final ClientBookingService clientBookingService;
  private final ClientProfileService clientProfileService;
  private final ResourceItemMediaResolver resourceItemMediaResolver;

  public ClientBookingController(
      MerchantRepository merchantRepository,
      MerchantProfileRepository profileRepository,
      ServiceItemRepository serviceItemRepository,
      CustomizationConfigRepository customizationConfigRepository,
      ResourceItemRepository resourceItemRepository,
      DynamicFieldConfigRepository dynamicFieldConfigRepository,
      MerchantMembershipRepository merchantMembershipRepository,
      ClientProfileRepository clientProfileRepository,
      BookingCommandService bookingCommandService,
      ClientAvailabilityService clientAvailabilityService,
      MerchantAccessService merchantAccessService,
      ClientBookingService clientBookingService,
      ClientProfileService clientProfileService,
      ResourceItemMediaResolver resourceItemMediaResolver) {
    this.merchantRepository = merchantRepository;
    this.profileRepository = profileRepository;
    this.serviceItemRepository = serviceItemRepository;
    this.customizationConfigRepository = customizationConfigRepository;
    this.resourceItemRepository = resourceItemRepository;
    this.dynamicFieldConfigRepository = dynamicFieldConfigRepository;
    this.merchantMembershipRepository = merchantMembershipRepository;
    this.clientProfileRepository = clientProfileRepository;
    this.bookingCommandService = bookingCommandService;
    this.clientAvailabilityService = clientAvailabilityService;
    this.merchantAccessService = merchantAccessService;
    this.clientBookingService = clientBookingService;
    this.clientProfileService = clientProfileService;
    this.resourceItemMediaResolver = resourceItemMediaResolver;
  }

  @GetMapping("/merchants")
  public java.util.List<ClientMerchantCardSummary> listVisibleMerchants() {
    UUID userId =
        java.util.Optional.ofNullable(merchantAccessService.currentPlatformUserOrNull())
            .map(u -> u.getId())
            .orElse(null);
    return merchantRepository.findByActiveTrueOrderByIdAsc().stream()
        .map(
            m -> {
              String joinState = "NONE";
              if (m.getVisibility() == MerchantVisibility.PUBLIC) {
                joinState = "PUBLIC";
              } else if (userId != null) {
                boolean joined =
                    merchantMembershipRepository
                        .findByMerchantIdAndPlatformUserId(m.getId(), userId)
                        .map(mm -> mm.getMembershipStatus() == MerchantMembershipStatus.ACTIVE)
                        .orElse(false);
                joinState = joined ? "JOINED" : "INVITE_REQUIRED";
              } else {
                joinState = "INVITE_REQUIRED";
              }
              return new ClientMerchantCardSummary(
                  m.getId(), m.getName(), m.getSlug(), m.getVisibility(), joinState);
            })
        .toList();
  }

  @GetMapping("/merchants/joined")
  public java.util.List<ClientJoinedMerchantSummary> listJoinedMerchants() {
    var user = merchantAccessService.currentPlatformUserOrNull();
    if (user == null) {
      return java.util.List.of();
    }
    return merchantMembershipRepository
        .findByPlatformUserIdAndMembershipStatus(user.getId(), MerchantMembershipStatus.ACTIVE)
        .stream()
        .map(
            m ->
                new ClientJoinedMerchantSummary(
                    m.getMerchant().getId(),
                    m.getMerchant().getName(),
                    m.getMerchant().getSlug(),
                    m.getMembershipStatus()))
        .toList();
  }

  @PostMapping("/merchant-memberships/join-code")
  public ClientJoinedMerchantSummary joinMerchantByCode(
      @Valid @RequestBody ClientJoinMerchantByCodeRequest request) {
    var membership = merchantAccessService.acceptInvitationByCode(request.inviteCode());
    return new ClientJoinedMerchantSummary(
        membership.getMerchant().getId(),
        membership.getMerchant().getName(),
        membership.getMerchant().getSlug(),
        membership.getMembershipStatus());
  }

  @GetMapping("/merchant/{slug}")
  public ClientMerchantResponse getClientMerchant(@PathVariable String slug) {
    Merchant merchant = merchantRepository.findBySlug(slug).orElseThrow(() -> new ApiException("Merchant not found"));
    merchantAccessService.assertClientCanAccessMerchant(merchant);
    MerchantProfile profile = profileRepository.findByMerchantId(merchant.getId()).orElse(null);
    CustomizationConfig config = customizationConfigRepository.findByMerchantId(merchant.getId())
        .orElseGet(() -> {
          CustomizationConfig created = new CustomizationConfig();
          created.setMerchant(merchant);
          return customizationConfigRepository.save(created);
        });
    var services = serviceItemRepository.findByMerchantId(merchant.getId());
    var serviceById = resourceItemMediaResolver.indexById(services);
    return new ClientMerchantResponse(
        new MerchantSummary(merchant.getId(), merchant.getName(), merchant.getSlug(), merchant.getActive()),
        new MerchantProfileSummary(profile == null ? null : profile.getDescription(), profile == null ? null : profile.getLogoUrl()),
        new CustomizationSummary(
            config.getThemePreset(),
            config.getThemeColor(),
            config.getHeroTitle(),
            config.getBookingFlowText(),
            config.getTermsText(),
            config.getAnnouncementText(),
            config.getBufferMinutes()),
        services.stream()
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
                        0L))
            .toList(),
        resourceItemRepository.findByMerchantId(merchant.getId()).stream()
            .map(
                r ->
                    resourceItemMediaResolver.toSummary(
                        r,
                        merchant.getId(),
                        serviceById,
                        Boolean.TRUE.equals(r.getMaintenance())
                            ? ResourceOperationalStatus.MAINTENANCE
                            : ResourceOperationalStatus.ACTIVE))
            .toList(),
        dynamicFieldConfigRepository.findByMerchantId(merchant.getId()).stream()
            .map(f -> new DynamicFieldSummary(f.getId(), f.getLabel(), f.getType(), f.getRequiredField(), f.getOptionsJson()))
            .toList());
  }

  @GetMapping("/availability")
  public AvailabilityResponse getClientAvailability(
      @RequestParam UUID merchantId,
      @RequestParam UUID serviceItemId,
      @RequestParam(required = false) UUID resourceId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    Merchant merchant = bookingCommandService.ensureMerchant(merchantId);
    merchantAccessService.assertClientCanAccessMerchant(merchant);
    return clientAvailabilityService.getAvailability(merchantId, serviceItemId, resourceId, date);
  }

  @GetMapping("/resources/{resourceId}/availability")
  public ClientResourceAvailabilityResponse getResourceAvailability(
      @PathVariable UUID resourceId, @RequestParam String date) {
    if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
      throw new ApiException("Invalid date format, expected YYYY-MM-DD", HttpStatus.BAD_REQUEST);
    }
    LocalDate parsedDate;
    try {
      parsedDate = LocalDate.parse(date);
    } catch (DateTimeParseException ex) {
      throw new ApiException("Invalid date format, expected YYYY-MM-DD", HttpStatus.BAD_REQUEST);
    }

    ResourceItem resource =
        resourceItemRepository
            .findById(resourceId)
            .orElseThrow(() -> new ApiException("Resource not found", HttpStatus.NOT_FOUND));
    try {
      merchantAccessService.assertClientCanAccessMerchant(resource.getMerchant());
    } catch (ApiException ex) {
      if (ex.getStatus() == HttpStatus.FORBIDDEN) {
        throw new ApiException("Resource not found", HttpStatus.NOT_FOUND);
      }
      throw ex;
    }
    return clientAvailabilityService.getResourceAvailability(resourceId, parsedDate);
  }

  @GetMapping("/resources/featured")
  public java.util.List<ClientCatalogResourceSummary> getFeaturedResources(
      @RequestParam(defaultValue = "6") int limit) {
    return clientBookingService.listFeaturedResources(limit);
  }

  @GetMapping("/categories")
  public java.util.List<ClientCategorySummary> getClientCategories() {
    return clientBookingService.listClientCategories();
  }

  @GetMapping("/resources")
  public ClientCatalogResourcesPage listClientResources(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String resourceType,
      @RequestParam(defaultValue = "relevance") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return clientBookingService.listClientResources(q, category, resourceType, sort, page, size);
  }

  @GetMapping("/resources/{resourceId}")
  public ClientResourceDetailResponse getClientResourceDetail(@PathVariable UUID resourceId) {
    return clientBookingService.getResourceDetail(resourceId);
  }

  @PostMapping("/booking/lock")
  public BookingLockResponse lockBookingSlot(@Valid @RequestBody BookingLockRequest request) {
    Merchant merchant = bookingCommandService.ensureMerchant(request.merchantId());
    merchantAccessService.assertClientCanAccessMerchant(merchant);
    return bookingCommandService.lockBookingSlot(request);
  }

  @PostMapping("/bookings")
  @PreAuthorize("hasAnyRole('CLIENT','CLIENT_USER')")
  @Operation(summary = "Create booking (CLIENT / CLIENT_USER)")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Booking created"),
      @ApiResponse(responseCode = "400", description = "Validation failed"),
      @ApiResponse(responseCode = "401", description = "Not authenticated"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Resource not found"),
      @ApiResponse(responseCode = "409", description = "Slot conflict"),
      @ApiResponse(responseCode = "422", description = "Business rule rejected")
  })
  public ResponseEntity<ApiEnvelope<ClientBookingCreateResponse>> createClientBooking(
      @Valid @RequestBody ClientBookingCreateRequest request) {
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiDtos.success(clientBookingService.createBooking(request)));
  }

  @GetMapping("/bookings")
  @PreAuthorize("hasAnyRole('CLIENT','CLIENT_USER')")
  public ApiEnvelope<ClientBookingListResponse> listMyBookings(
      @RequestParam(defaultValue = "upcoming") String tab,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiDtos.success(clientBookingService.listMyBookings(tab, page, size));
  }

  @PatchMapping("/bookings/{bookingId}/cancel")
  @PreAuthorize("hasAnyRole('CLIENT','CLIENT_USER')")
  public ApiEnvelope<ClientBookingStatusResponse> cancelMyBooking(
      @PathVariable UUID bookingId, @Valid @RequestBody(required = false) ClientBookingCancelRequest request) {
    return ApiDtos.success(
        clientBookingService.cancelMyBooking(bookingId, request == null ? null : request.reason()));
  }

  @PatchMapping("/bookings/{bookingId}/reschedule")
  @PreAuthorize("hasAnyRole('CLIENT','CLIENT_USER')")
  public ApiEnvelope<ClientBookingStatusResponse> rescheduleMyBooking(
      @PathVariable UUID bookingId, @Valid @RequestBody ClientBookingRescheduleRequest request) {
    return ApiDtos.success(clientBookingService.rescheduleMyBooking(bookingId, request));
  }

  @GetMapping("/bookings/{bookingId}/availability")
  @PreAuthorize("hasAnyRole('CLIENT','CLIENT_USER')")
  public ApiEnvelope<AvailabilityResponse> getMyBookingRescheduleAvailability(
      @PathVariable UUID bookingId, @RequestParam String date) {
    if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
      throw new ApiException("Invalid date format, expected YYYY-MM-DD", HttpStatus.BAD_REQUEST);
    }
    LocalDate parsedDate;
    try {
      parsedDate = LocalDate.parse(date);
    } catch (DateTimeParseException ex) {
      throw new ApiException("Invalid date format, expected YYYY-MM-DD", HttpStatus.BAD_REQUEST);
    }
    return ApiDtos.success(clientBookingService.getMyBookingRescheduleAvailability(bookingId, parsedDate));
  }

  @GetMapping("/profile")
  public ClientProfileResponse clientProfile() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null
        || !auth.isAuthenticated()
        || "anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
      return new ClientProfileResponse(
          false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
    String role = auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(a -> a.startsWith("ROLE_"))
        .map(a -> a.substring(5))
        .findFirst()
        .orElse(null);
    var platformUser = merchantAccessService.currentPlatformUserOrNull();
    if (platformUser == null) {
      return new ClientProfileResponse(
          true,
          role,
          String.valueOf(auth.getPrincipal()),
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null);
    }
    var profile = clientProfileRepository.findByPlatformUserId(platformUser.getId()).orElse(null);
    String suggestedName =
        profile != null && profile.getDisplayName() != null && !profile.getDisplayName().isBlank()
            ? profile.getDisplayName()
            : platformUser.getUsername();
    String suggestedContact = profile == null ? null : profile.getContactPhone();
    return new ClientProfileResponse(
        true,
        role,
        suggestedName,
        suggestedContact,
        profile == null ? null : profile.getLocation(),
        profile == null ? null : profile.getBio(),
        profile == null ? null : profile.getLanguage(),
        profile == null ? null : profile.getTimezone(),
        profile == null ? null : profile.getCurrency(),
        profile == null ? null : profile.getTheme(),
        profile == null ? null : profile.getEmailNotifications(),
        profile == null ? null : profile.getSmsNotifications(),
        profile == null ? null : profile.getPushNotifications(),
        profile == null ? null : profile.getMarketingEmails(),
        profile == null ? null : profile.getSecurityAlerts(),
        profile == null ? null : profile.getProductUpdates(),
        profile == null ? null : profile.getTwoFactorEnabled());
  }

  @PutMapping("/profile")
  @PreAuthorize("hasAnyRole('CLIENT','CLIENT_USER')")
  public ClientProfileResponse updateClientProfile(
      @Valid @RequestBody ClientProfileUpdateRequest request) {
    var platformUser =
        java.util.Optional.ofNullable(merchantAccessService.currentPlatformUserOrNull())
            .orElseThrow(() -> new ApiException("Unauthorized", HttpStatus.UNAUTHORIZED));
    ClientProfile profile =
        clientProfileRepository
            .findByPlatformUserId(platformUser.getId())
            .orElseGet(
                () -> {
                  ClientProfile created = new ClientProfile();
                  created.setPlatformUser(platformUser);
                  return created;
                });
    if (request.suggestedName() != null) {
      profile.setDisplayName(trimToNull(request.suggestedName()));
    }
    if (request.suggestedContact() != null) {
      profile.setContactPhone(trimToNull(request.suggestedContact()));
    }
    if (request.location() != null) {
      profile.setLocation(trimToNull(request.location()));
    }
    if (request.bio() != null) {
      profile.setBio(trimToNull(request.bio()));
    }
    if (request.language() != null) {
      profile.setLanguage(trimToNull(request.language()));
    }
    if (request.timezone() != null) {
      profile.setTimezone(trimToNull(request.timezone()));
    }
    if (request.currency() != null) {
      profile.setCurrency(trimToNull(request.currency()));
    }
    if (request.theme() != null) {
      profile.setTheme(trimToNull(request.theme()));
    }
    if (request.emailNotifications() != null) {
      profile.setEmailNotifications(request.emailNotifications());
    }
    if (request.smsNotifications() != null) {
      profile.setSmsNotifications(request.smsNotifications());
    }
    if (request.pushNotifications() != null) {
      profile.setPushNotifications(request.pushNotifications());
    }
    if (request.marketingEmails() != null) {
      profile.setMarketingEmails(request.marketingEmails());
    }
    if (request.securityAlerts() != null) {
      profile.setSecurityAlerts(request.securityAlerts());
    }
    if (request.productUpdates() != null) {
      profile.setProductUpdates(request.productUpdates());
    }
    if (request.twoFactorEnabled() != null) {
      profile.setTwoFactorEnabled(request.twoFactorEnabled());
    }
    ClientProfile saved = clientProfileRepository.save(profile);
    return new ClientProfileResponse(
        true,
        platformUser.getRole().name(),
        saved.getDisplayName() == null ? platformUser.getUsername() : saved.getDisplayName(),
        saved.getContactPhone(),
        saved.getLocation(),
        saved.getBio(),
        saved.getLanguage(),
        saved.getTimezone(),
        saved.getCurrency(),
        saved.getTheme(),
        saved.getEmailNotifications(),
        saved.getSmsNotifications(),
        saved.getPushNotifications(),
        saved.getMarketingEmails(),
        saved.getSecurityAlerts(),
        saved.getProductUpdates(),
        saved.getTwoFactorEnabled());
  }

  @PatchMapping("/profile/password")
  @PreAuthorize("hasAnyRole('CLIENT','CLIENT_USER')")
  public ClientPasswordUpdateResponse updateClientPassword(
      @Valid @RequestBody ClientPasswordUpdateRequest request) {
    return clientProfileService.updatePassword(request);
  }

  @GetMapping("/profile/preferences")
  @PreAuthorize("hasAnyRole('CLIENT','CLIENT_USER')")
  public ClientProfilePreferencesResponse getClientProfilePreferences() {
    return clientProfileService.getPreferences();
  }

  @PutMapping("/profile/preferences")
  @PreAuthorize("hasAnyRole('CLIENT','CLIENT_USER')")
  public ClientProfilePreferencesResponse updateClientProfilePreferences(
      @Valid @RequestBody ClientProfilePreferencesUpdateRequest request) {
    return clientProfileService.updatePreferences(request);
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  @PostMapping("/public/{slug}/bookings")
  public PublicBookingResponse createPublicBooking(
      @PathVariable String slug, @Valid @RequestBody PublicBookingRequest request) {
    Merchant merchant = merchantRepository.findBySlug(slug).orElseThrow(() -> new ApiException("Merchant not found"));
    merchantAccessService.assertClientCanAccessMerchant(merchant);
    ServiceItem service = serviceItemRepository.findByIdAndMerchantId(request.serviceItemId(), merchant.getId())
        .orElseThrow(() -> new ApiException("Service not found"));
    Booking booking = bookingCommandService.createBookingWithValidation(
        merchant, service, request.startAt(), request.customerName(), request.customerContact());
    return new PublicBookingResponse(
        booking.getId(),
        "BK-" + booking.getId(),
        booking.getStartAt(),
        booking.getEndAt(),
        booking.getStatus());
  }

  @GetMapping("/public/{slug}/storefront")
  public StorefrontResponse getStorefront(@PathVariable String slug) {
    Merchant merchant = merchantRepository.findBySlug(slug).orElseThrow(() -> new ApiException("Merchant not found"));
    merchantAccessService.assertClientCanAccessMerchant(merchant);
    MerchantProfile profile = profileRepository.findByMerchantId(merchant.getId()).orElse(null);
    CustomizationConfig config = customizationConfigRepository.findByMerchantId(merchant.getId())
        .orElseGet(() -> {
          CustomizationConfig created = new CustomizationConfig();
          created.setMerchant(merchant);
          return customizationConfigRepository.save(created);
        });
    return new StorefrontResponse(
        new MerchantSummary(merchant.getId(), merchant.getName(), merchant.getSlug(), merchant.getActive()),
        new MerchantProfileSummary(profile == null ? null : profile.getDescription(), profile == null ? null : profile.getLogoUrl()),
        new CustomizationSummary(
            config.getThemePreset(),
            config.getThemeColor(),
            config.getHeroTitle(),
            config.getBookingFlowText(),
            config.getTermsText(),
            config.getAnnouncementText(),
            config.getBufferMinutes()),
        serviceItemRepository.findByMerchantId(merchant.getId()).stream()
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
                        0L))
            .toList());
  }
}
