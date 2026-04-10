package com.bookingcore.api;

import com.bookingcore.api.ApiDtos.AvailabilityResponse;
import com.bookingcore.api.ApiDtos.BookingLockRequest;
import com.bookingcore.api.ApiDtos.BookingLockResponse;
import com.bookingcore.api.ApiDtos.ClientJoinMerchantByCodeRequest;
import com.bookingcore.api.ApiDtos.ClientJoinedMerchantSummary;
import com.bookingcore.api.ApiDtos.CustomizationSummary;
import com.bookingcore.api.ApiDtos.BookingSubmitResponse;
import com.bookingcore.api.ApiDtos.ClientMerchantCardSummary;
import com.bookingcore.api.ApiDtos.ClientBookingRequest;
import com.bookingcore.api.ApiDtos.ClientMerchantResponse;
import com.bookingcore.api.ApiDtos.ClientProfileResponse;
import com.bookingcore.api.ApiDtos.DynamicFieldSummary;
import com.bookingcore.api.ApiDtos.MerchantProfileSummary;
import com.bookingcore.api.ApiDtos.MerchantSummary;
import com.bookingcore.api.ApiDtos.PublicBookingRequest;
import com.bookingcore.api.ApiDtos.PublicBookingResponse;
import com.bookingcore.api.ApiDtos.ResourceItemSummary;
import com.bookingcore.api.ApiDtos.ServiceItemSummary;
import com.bookingcore.api.ApiDtos.StorefrontResponse;
import com.bookingcore.common.ApiException;
import com.bookingcore.modules.booking.Booking;
import com.bookingcore.modules.customization.CustomizationConfig;
import com.bookingcore.modules.customization.CustomizationConfigRepository;
import com.bookingcore.modules.merchant.DynamicFieldConfigRepository;
import com.bookingcore.modules.merchant.MerchantMembershipRepository;
import com.bookingcore.modules.merchant.MerchantMembershipStatus;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.MerchantProfile;
import com.bookingcore.modules.merchant.MerchantProfileRepository;
import com.bookingcore.modules.merchant.MerchantRepository;
import com.bookingcore.modules.merchant.MerchantVisibility;
import com.bookingcore.modules.merchant.ResourceItemRepository;
import com.bookingcore.modules.service.ServiceItem;
import com.bookingcore.modules.service.ServiceItemRepository;
import com.bookingcore.service.BookingCommandService;
import com.bookingcore.service.ClientAvailabilityService;
import com.bookingcore.service.MerchantAccessService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client")
public class ClientBookingController {
  private final MerchantRepository merchantRepository;
  private final MerchantProfileRepository profileRepository;
  private final ServiceItemRepository serviceItemRepository;
  private final CustomizationConfigRepository customizationConfigRepository;
  private final ResourceItemRepository resourceItemRepository;
  private final DynamicFieldConfigRepository dynamicFieldConfigRepository;
  private final MerchantMembershipRepository merchantMembershipRepository;
  private final BookingCommandService bookingCommandService;
  private final ClientAvailabilityService clientAvailabilityService;
  private final MerchantAccessService merchantAccessService;

  public ClientBookingController(
      MerchantRepository merchantRepository,
      MerchantProfileRepository profileRepository,
      ServiceItemRepository serviceItemRepository,
      CustomizationConfigRepository customizationConfigRepository,
      ResourceItemRepository resourceItemRepository,
      DynamicFieldConfigRepository dynamicFieldConfigRepository,
      MerchantMembershipRepository merchantMembershipRepository,
      BookingCommandService bookingCommandService,
      ClientAvailabilityService clientAvailabilityService,
      MerchantAccessService merchantAccessService) {
    this.merchantRepository = merchantRepository;
    this.profileRepository = profileRepository;
    this.serviceItemRepository = serviceItemRepository;
    this.customizationConfigRepository = customizationConfigRepository;
    this.resourceItemRepository = resourceItemRepository;
    this.dynamicFieldConfigRepository = dynamicFieldConfigRepository;
    this.merchantMembershipRepository = merchantMembershipRepository;
    this.bookingCommandService = bookingCommandService;
    this.clientAvailabilityService = clientAvailabilityService;
    this.merchantAccessService = merchantAccessService;
  }

  @GetMapping("/merchants")
  public java.util.List<ClientMerchantCardSummary> listVisibleMerchants() {
    Long userId =
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
        serviceItemRepository.findByMerchantId(merchant.getId()).stream()
            .map(s -> new ServiceItemSummary(s.getId(), s.getName(), s.getDurationMinutes(), s.getPrice(), s.getCategory()))
            .toList(),
        resourceItemRepository.findByMerchantId(merchant.getId()).stream()
            .map(r -> new ResourceItemSummary(r.getId(), r.getName(), r.getType(), r.getCategory(), r.getCapacity(), r.getActive(), r.getPrice()))
            .toList(),
        dynamicFieldConfigRepository.findByMerchantId(merchant.getId()).stream()
            .map(f -> new DynamicFieldSummary(f.getId(), f.getLabel(), f.getType(), f.getRequiredField(), f.getOptionsJson()))
            .toList());
  }

  @GetMapping("/availability")
  public AvailabilityResponse getClientAvailability(
      @RequestParam Long merchantId,
      @RequestParam Long serviceItemId,
      @RequestParam(required = false) Long resourceId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    Merchant merchant = bookingCommandService.ensureMerchant(merchantId);
    merchantAccessService.assertClientCanAccessMerchant(merchant);
    return clientAvailabilityService.getAvailability(merchantId, serviceItemId, resourceId, date);
  }

  @PostMapping("/booking/lock")
  public BookingLockResponse lockBookingSlot(@Valid @RequestBody BookingLockRequest request) {
    Merchant merchant = bookingCommandService.ensureMerchant(request.merchantId());
    merchantAccessService.assertClientCanAccessMerchant(merchant);
    return bookingCommandService.lockBookingSlot(request);
  }

  @PostMapping({"/booking", "/bookings"})
  public BookingSubmitResponse createClientBooking(@Valid @RequestBody ClientBookingRequest request) {
    Merchant merchant = bookingCommandService.ensureMerchant(request.merchantId());
    merchantAccessService.assertClientCanAccessMerchant(merchant);
    customizationConfigRepository.findByMerchantId(merchant.getId())
        .orElseThrow(() -> new ApiException("Merchant booking settings not found"));
    ServiceItem service = serviceItemRepository.findByIdAndMerchantId(request.serviceItemId(), merchant.getId())
        .orElseThrow(() -> new ApiException("Service not found"));
    if (!request.agreeTerms()) {
      throw new ApiException("Please agree to booking terms");
    }

    bookingCommandService.assertValidLockAndConsume(request);

    Booking booking = bookingCommandService.createBookingWithValidation(
        merchant, service, request.startAt(), request.customerName(), request.customerContact());
    return new BookingSubmitResponse(
        booking.getId(),
        "BK-" + booking.getId(),
        booking.getStartAt(),
        booking.getEndAt(),
        "/client/booking/" + merchant.getSlug() + "?bookingCode=BK-" + booking.getId());
  }

  @GetMapping("/profile")
  public ClientProfileResponse clientProfile() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null
        || !auth.isAuthenticated()
        || "anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
      return new ClientProfileResponse(false, null, null, null);
    }
    String role = auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(a -> a.startsWith("ROLE_"))
        .map(a -> a.substring(5))
        .findFirst()
        .orElse(null);
    String suggestedName = String.valueOf(auth.getPrincipal());
    return new ClientProfileResponse(true, role, suggestedName, null);
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
            .map(s -> new ServiceItemSummary(s.getId(), s.getName(), s.getDurationMinutes(), s.getPrice(), s.getCategory()))
            .toList());
  }
}
