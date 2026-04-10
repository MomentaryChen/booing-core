package com.bookingcore.api;

import com.bookingcore.modules.booking.BookingStatus;
import com.bookingcore.modules.booking.BookingTransitionEvent;
import com.bookingcore.modules.merchant.MerchantInvitationStatus;
import com.bookingcore.modules.merchant.MerchantMembershipStatus;
import com.bookingcore.modules.merchant.MerchantVisibility;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ApiDtos {
  private ApiDtos() {}

  public record CreateMerchantRequest(@NotBlank String name, @NotBlank String slug) {}

  /**
   * Unified public registration body for {@code POST /api/auth/register}. {@code registerType} is
   * validated against a server allowlist; unsafe types are rejected without provisioning.
   */
  /**
   * {@link PublicRegisterType#MERCHANT}: {@code name}, {@code slug} required.<br>
   * {@link PublicRegisterType#CLIENT}: {@code username}, {@code password} required.<br>
   * Other fields are ignored by the service layer.
   */
  public record PublicRegisterRequest(
      @NotNull PublicRegisterType registerType,
      String name,
      String slug,
      String username,
      String password) {}

  /**
   * Merchant signup result including a server-chosen post-registration path (never derived from
   * client input).
   */
  public record PublicRegisterResponse(
      Long id, String name, String slug, Boolean active, String nextDestination) {}

  public record MerchantProfileResponse(String description, String logoUrl) {}

  public record ServiceItemRequest(
      @NotBlank String name,
      @NotNull @Min(10) Integer durationMinutes,
      @NotNull BigDecimal price,
      @NotBlank String category) {}

  public record BusinessHoursRequest(
      @NotNull DayOfWeek dayOfWeek,
      @NotNull LocalTime startTime,
      @NotNull LocalTime endTime) {}

  public record CustomizationRequest(
      @NotBlank String themePreset,
      @NotBlank String themeColor,
      @NotBlank String heroTitle,
      @NotBlank String bookingFlowText,
      String inviteCode,
      String termsText,
      String announcementText,
      @NotBlank String faqJson,
      @NotNull Integer bufferMinutes,
      @NotBlank String homepageSectionsJson,
      @NotBlank String categoryOrderJson) {}

  public record PublicBookingRequest(
      @NotNull Long serviceItemId,
      @NotNull LocalDateTime startAt,
      @NotBlank String customerName,
      @NotBlank String customerContact) {}

  public record DynamicFieldRequest(
      @NotBlank String label,
      @NotBlank String type,
      @NotNull Boolean requiredField,
      @NotBlank String optionsJson) {}

  public record ResourceRequest(
      @NotBlank String name,
      @NotBlank String type,
      String category,
      @Min(1) Integer capacity,
      Boolean active,
      @NotBlank String serviceItemsJson,
      @NotNull BigDecimal price) {}

  public record AvailabilityExceptionRequest(
      @NotBlank String type,
      @NotNull LocalDateTime startAt,
      @NotNull LocalDateTime endAt,
      String reason) {}

  public record BookingStatusRequest(@NotNull BookingStatus status) {}

  /** Merchant booking status update (backward-compatible with current frontend). */
  public record MerchantBookingStatusUpdateRequest(@NotNull BookingStatus status, String reason) {}

  /** Event-based transition request (reserved for future UI). */
  public record BookingTransitionRequest(@NotNull BookingTransitionEvent event, String reason) {}

  public record ManualBookingRequest(
      @NotNull Long serviceItemId,
      @NotNull LocalDateTime startAt,
      @NotBlank String customerName,
      @NotBlank String customerContact) {}

  public record MerchantStatusRequest(@NotNull Boolean active) {}

  public record MerchantLimitRequest(@NotNull @Min(1) Integer serviceLimit) {}

  public record MerchantVisibilityUpdateRequest(@NotNull MerchantVisibility visibility) {}

  public record MerchantInvitationCreateRequest(
      @NotBlank String inviteeUsername,
      LocalDateTime expiresAt) {}

  public record MerchantInvitationUpdateRequest(@NotNull MerchantInvitationStatus status) {}

  public record ClientJoinMerchantByCodeRequest(@NotBlank String inviteCode) {}

  public record DomainTemplateRequest(@NotBlank String domainName, @NotBlank String fieldsJson) {}

  public record SystemSettingsRequest(String emailTemplate, String smsTemplate, String maintenanceAnnouncement) {}

  public record MerchantSummary(Long id, String name, String slug, Boolean active) {}

  public record ClientMerchantCardSummary(
      Long merchantId,
      String merchantName,
      String merchantSlug,
      MerchantVisibility visibility,
      String joinState) {}

  public record MerchantInvitationSummary(
      Long invitationId,
      Long merchantId,
      String inviteCode,
      String inviteeUsername,
      MerchantInvitationStatus status,
      LocalDateTime expiresAt) {}

  public record ClientJoinedMerchantSummary(
      Long merchantId,
      String merchantName,
      String merchantSlug,
      MerchantMembershipStatus membershipStatus) {}

  public record MerchantProfileSummary(String description, String logoUrl) {}

  public record CustomizationSummary(
      String themePreset,
      String themeColor,
      String heroTitle,
      String bookingFlowText,
      String termsText,
      String announcementText,
      Integer bufferMinutes) {}

  public record ServiceItemSummary(
      Long id, String name, Integer durationMinutes, BigDecimal price, String category) {}

  public record ResourceItemSummary(
      Long id,
      String name,
      String type,
      String category,
      Integer capacity,
      Boolean active,
      BigDecimal price) {}

  public record DynamicFieldSummary(
      Long id,
      String label,
      String type,
      Boolean requiredField,
      String optionsJson) {}

  public record BusinessHoursSummary(
      Long id,
      DayOfWeek dayOfWeek,
      LocalTime startTime,
      LocalTime endTime) {}

  public record AvailabilityExceptionSummary(
      Long id,
      String type,
      LocalDateTime startAt,
      LocalDateTime endAt,
      String reason) {}

  public record MerchantCustomizationResponse(
      String themePreset,
      String themeColor,
      String heroTitle,
      String bookingFlowText,
      String inviteCode,
      String termsText,
      String announcementText,
      String faqJson,
      Integer bufferMinutes,
      String homepageSectionsJson,
      String categoryOrderJson) {}

  public record MerchantBookingSummary(
      Long id,
      Long serviceItemId,
      LocalDateTime startAt,
      LocalDateTime endAt,
      String customerName,
      String customerContact,
      BookingStatus status) {}

  public record StorefrontResponse(
      MerchantSummary merchant,
      MerchantProfileSummary profile,
      CustomizationSummary customization,
      List<ServiceItemSummary> services) {}

  public record ClientMerchantResponse(
      MerchantSummary merchant,
      MerchantProfileSummary profile,
      CustomizationSummary customization,
      List<ServiceItemSummary> services,
      List<ResourceItemSummary> resources,
      List<DynamicFieldSummary> dynamicFields) {}

  public record PublicBookingResponse(
      Long bookingId,
      String bookingCode,
      LocalDateTime startAt,
      LocalDateTime endAt,
      BookingStatus status) {}

  public record AvailabilityResponse(LocalDate date, List<AvailabilitySlot> slots) {}

  public record AvailabilitySlot(LocalDateTime startAt, LocalDateTime endAt, Boolean available, String status) {}

  public record BookingLockRequest(
      @NotNull Long merchantId,
      @NotNull Long serviceItemId,
      Long resourceId,
      @NotNull LocalDateTime startAt) {}

  public record BookingLockResponse(String lockId, LocalDateTime expiresAt) {}

  public record ClientBookingRequest(
      @NotNull Long merchantId,
      @NotNull Long serviceItemId,
      Long resourceId,
      @NotBlank String lockId,
      @NotNull LocalDateTime startAt,
      String inviteCode,
      @NotBlank String customerName,
      @NotBlank String customerContact,
      @NotNull Boolean agreeTerms,
      Map<String, String> dynamicFieldValues) {}

  public record ClientProfileResponse(Boolean authenticated, String role, String suggestedName, String suggestedContact) {}

  public record BookingSubmitResponse(
      Long bookingId, String bookingCode, LocalDateTime startAt, LocalDateTime endAt, String queryPath) {}

  public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

  /**
   * Current principal snapshot for unified login / SPA bootstrap.
   * {@code availableContexts} lists server-verifiable options (length 1 for most users; SYSTEM_ADMIN may see
   * an extra MERCHANT-scoped option backed by the first merchant row for demo / support-style switching).
   */
  public record AuthMeResponse(
      String username,
      String role,
      List<String> roles,
      List<String> permissions,
      Long merchantId,
      String sessionState,
      List<AuthContextOption> availableContexts,
      AuthContextOption activeContext) {
    public AuthMeResponse {
      roles = roles == null ? List.of() : Collections.unmodifiableList(List.copyOf(roles));
      permissions =
          permissions == null ? List.of() : Collections.unmodifiableList(List.copyOf(permissions));
      availableContexts =
          availableContexts == null
              ? List.of()
              : Collections.unmodifiableList(List.copyOf(availableContexts));
    }
  }

  /**
   * One selectable auth context. {@code kind} is diagnostic: {@code PLATFORM} matches the stored platform
   * role; {@code MERCHANT_SCOPED} is a derived MERCHANT view (e.g. admin previewing a tenant).
   */
  public record AuthContextOption(String kind, Long merchantId, String role) {}

  /** Body for {@code POST /api/auth/context/select}. Omit or leave {@code role} empty to behave like refresh. */
  public record ContextSelectRequest(Long merchantId, String role) {}

  public record TokenResponse(
      String accessToken,
      String tokenType,
      long expiresInSeconds,
      String role,
      List<String> roles,
      List<String> permissions) {
    public TokenResponse {
      roles = roles == null ? List.of() : Collections.unmodifiableList(List.copyOf(roles));
      permissions =
          permissions == null ? List.of() : Collections.unmodifiableList(List.copyOf(permissions));
    }
  }

  /** Allowed UI routes from {@code platform_pages} / {@code role_page_grants} for the current actor. */
  public record NavigationResponse(List<String> routeKeys, List<NavigationItem> items) {}

  public record NavigationItem(String routeKey, String path, String labelKey, int sortOrder) {}

  public record SystemUserSummary(
      Long id,
      String username,
      Boolean enabled,
      String primaryRole,
      Long primaryMerchantId,
      Long activeBindingsCount,
      List<String> roleCodes,
      LocalDateTime lastLoginAt,
      LocalDateTime updatedAt) {}

  public record SystemUserBindingSummary(
      Long bindingId, String roleCode, Long merchantId, String status, List<String> permissions) {}

  public record SystemUserDetailResponse(
      Long id,
      String username,
      Boolean enabled,
      String primaryRole,
      Long primaryMerchantId,
      List<SystemUserBindingSummary> bindings,
      List<String> effectivePermissions,
      LocalDateTime lastLoginAt,
      LocalDateTime updatedAt) {}

  public record SystemUserStatusUpdateRequest(@NotNull Boolean enabled) {}

  public record SystemUserBindingUpsertRequest(
      @NotBlank String roleCode,
      Long merchantId,
      @NotNull Boolean active) {}

  public record SystemUserBindingsUpdateRequest(
      @NotEmpty List<@jakarta.validation.Valid SystemUserBindingUpsertRequest> bindings) {}

  public record SystemRbacRoleResponse(String roleCode, List<String> permissions) {}

  public record SystemBookingTransitionRequest(
      @NotNull Long merchantId,
      @NotNull BookingTransitionEvent event,
      String reason) {}
}
