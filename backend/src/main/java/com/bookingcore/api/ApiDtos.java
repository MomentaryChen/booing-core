package com.bookingcore.api;

import com.bookingcore.modules.booking.BookingStatus;
import com.bookingcore.modules.booking.BookingTransitionEvent;
import com.bookingcore.modules.merchant.MerchantInvitationStatus;
import com.bookingcore.modules.merchant.MerchantMembershipStatus;
import com.bookingcore.modules.merchant.MerchantVisibility;
import com.bookingcore.modules.merchant.ResourceStaffAssignmentStatus;
import com.bookingcore.modules.merchant.ServiceTeamStatus;
import com.bookingcore.modules.merchant.TeamMemberStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ApiDtos {
  private ApiDtos() {}

  public record ApiEnvelope<T>(int code, String message, T data) {}

  public static <T> ApiEnvelope<T> success(T data) {
    return new ApiEnvelope<>(0, "success", data);
  }

  public record CreateMerchantRequest(@NotBlank String name, @NotBlank String slug) {}

  /**
   * Unified public registration body for {@code POST /api/auth/register}. {@code registerType} is
   * validated against a server allowlist; unsafe types are rejected without provisioning.
   */
  /**
   * {@link PublicRegisterType#MERCHANT}: {@code name}, {@code slug} required.<br>
   * {@link PublicRegisterType#CLIENT}: {@code username} (email), {@code password} required.<br>
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
      UUID id, String name, String slug, Boolean active, String nextDestination) {}

  public record MerchantProfileResponse(
      String description,
      String logoUrl,
      String address,
      String phone,
      String email,
      String website,
      String storeCategory,
      String lineContactUrl) {}

  public record ServiceItemRequest(
      @NotBlank String name,
      @NotNull @Min(10) Integer durationMinutes,
      @NotNull BigDecimal price,
      @NotBlank String category,
      String imageUrl) {}

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
      @NotBlank String categoryOrderJson,
      @NotNull Boolean notificationNewBooking,
      @NotNull Boolean notificationCancellation,
      @NotNull Boolean notificationDailySummary) {}

  public record PublicBookingRequest(
      @NotNull UUID serviceItemId,
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
      List<@NotNull UUID> assignedStaffIds,
      @NotNull BigDecimal price) {}

  public record ResourceUpdateRequest(
      String name,
      String type,
      String category,
      @Min(1) Integer capacity,
      Boolean active,
      String serviceItemsJson,
      List<@NotNull UUID> assignedStaffIds,
      @Min(0) BigDecimal price) {}

  public enum ResourceOperationalStatus {
    ACTIVE,
    MAINTENANCE,
    FULLY_BOOKED
  }

  public record ServiceCloneRequest(@Size(max = 60) String nameSuffix) {}

  public record ResourceBatchPriceRequest(
      @NotEmpty List<@NotNull UUID> resourceIds, @NotNull @Min(0) BigDecimal price) {}

  public record ResourceBatchStatusRequest(
      @NotEmpty List<@NotNull UUID> resourceIds, @NotNull ResourceOperationalStatus status) {}

  public record ResourceBatchBusinessHoursRequest(
      @NotEmpty List<@NotNull UUID> resourceIds, @NotBlank String businessHoursJson) {}

  public record MerchantResourceListResponse(
      List<ResourceItemSummary> items, int page, int size, long total) {}

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
      @NotNull UUID serviceItemId,
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

  public record TeamCreateRequest(
      @NotBlank String name,
      @NotBlank @Size(max = 80) String code,
      ServiceTeamStatus status) {}

  public record TeamUpdateRequest(
      @NotBlank String name,
      ServiceTeamStatus status) {}

  public record TeamMemberAssignRequest(
      @NotNull UUID userId,
      @NotBlank @Size(max = 64) String role,
      TeamMemberStatus status) {}

  public record BookingAssignmentCommandRequest(
      @NotNull UUID resourceId,
      UUID staffId,
      UUID newStaffId,
      @Size(max = 300) String reason) {}

  public record DomainTemplateRequest(@NotBlank String domainName, @NotBlank String fieldsJson) {}

  public record SystemSettingsRequest(String emailTemplate, String smsTemplate, String maintenanceAnnouncement) {}

  public record MerchantSummary(UUID id, String name, String slug, Boolean active) {}

  public record ClientMerchantCardSummary(
      UUID merchantId,
      String merchantName,
      String merchantSlug,
      MerchantVisibility visibility,
      String joinState) {}

  public record MerchantInvitationSummary(
      Long invitationId,
      UUID merchantId,
      String inviteCode,
      String inviteeUsername,
      MerchantInvitationStatus status,
      LocalDateTime expiresAt) {}

  public record TeamSummary(
      Long id,
      UUID merchantId,
      String name,
      String code,
      ServiceTeamStatus status,
      LocalDateTime createdAt) {}

  public record TeamMemberSummary(
      Long id,
      UUID merchantId,
      Long teamId,
      UUID userId,
      String username,
      String role,
      TeamMemberStatus status) {}

  public record BookingAssignmentSummary(
      Long assignmentId,
      UUID bookingId,
      UUID merchantId,
      UUID resourceId,
      UUID staffId,
      String staffUsername,
      ResourceStaffAssignmentStatus status,
      String reason,
      LocalDateTime startAt,
      LocalDateTime endAt) {}

  public record StaffCandidateSummary(
      UUID staffId, String staffUsername, Boolean active, Boolean available, String unavailableReason) {}

  public record ClientJoinedMerchantSummary(
      UUID merchantId,
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
      UUID id,
      String name,
      Integer durationMinutes,
      BigDecimal price,
      String category,
      String imageUrl,
      Boolean active,
      Long resourceCount) {}

  public record ResourceItemSummary(
      UUID id,
      String name,
      String type,
      String category,
      Integer capacity,
      Boolean active,
      BigDecimal price,
      List<UUID> assignedStaffIds,
      String serviceItemsJson,
      /** Optional storefront media; derived from scoped service items when available. */
      String imageUrl,
      ResourceOperationalStatus status,
      String businessHoursJson) {}

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
      String categoryOrderJson,
      Boolean notificationNewBooking,
      Boolean notificationCancellation,
      Boolean notificationDailySummary) {}

  public record MerchantBookingSummary(
      UUID id,
      UUID serviceItemId,
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
      UUID bookingId,
      String bookingCode,
      LocalDateTime startAt,
      LocalDateTime endAt,
      BookingStatus status) {}

  public record AvailabilityResponse(LocalDate date, List<AvailabilitySlot> slots) {}

  public record AvailabilitySlot(LocalDateTime startAt, LocalDateTime endAt, Boolean available, String status) {}

  public record ClientResourceAvailabilityResponse(
      LocalDate date, List<ClientResourceAvailabilitySlot> slots) {}

  public record ClientResourceAvailabilitySlot(
      LocalDateTime startAt,
      LocalDateTime endAt,
      Boolean isAvailable,
      Integer capacityRemaining) {}

  public record BookingLockRequest(
      @NotNull UUID merchantId,
      @NotNull UUID serviceItemId,
      UUID resourceId,
      @NotNull LocalDateTime startAt) {}

  public record BookingLockResponse(String lockId, LocalDateTime expiresAt) {}

  public record ClientBookingRequest(
      @NotNull UUID merchantId,
      @NotNull UUID serviceItemId,
      UUID resourceId,
      @NotBlank String lockId,
      @NotNull LocalDateTime startAt,
      String inviteCode,
      @NotBlank String customerName,
      @NotBlank String customerContact,
      @NotNull Boolean agreeTerms,
      Map<String, String> dynamicFieldValues) {}

  public record ClientProfileResponse(
      Boolean authenticated,
      String role,
      String suggestedName,
      String suggestedContact,
      String language,
      String timezone,
      String currency,
      Boolean emailNotifications,
      Boolean smsNotifications) {}

  public record ClientProfileUpdateRequest(
      @Size(max = 120) String suggestedName,
      @Size(max = 120) String suggestedContact,
      @Size(max = 16) String language,
      @Size(max = 64) String timezone,
      @Size(max = 16) String currency,
      Boolean emailNotifications,
      Boolean smsNotifications) {}

  public record ClientPasswordUpdateRequest(
      @NotBlank @Size(min = 8, max = 120) String currentPassword,
      @NotBlank @Size(min = 8, max = 120) String newPassword) {}

  public record ClientProfilePreferencesResponse(
      String language,
      String timezone,
      String currency,
      Boolean emailNotifications,
      Boolean smsNotifications) {}

  public record ClientProfilePreferencesUpdateRequest(
      @Size(max = 16) String language,
      @Size(max = 64) String timezone,
      @Size(max = 16) String currency,
      Boolean emailNotifications,
      Boolean smsNotifications) {}

  public record ClientPasswordUpdateResponse(LocalDateTime updatedAt) {}

  public record BookingSubmitResponse(
      UUID bookingId, String bookingCode, LocalDateTime startAt, LocalDateTime endAt, String queryPath) {}

  public record ClientBookingCreateRequest(
      @NotNull UUID resourceId, @NotNull LocalDateTime startAt, @Size(max = 500) String notes) {}

  public record ClientBookingCreateResponse(
      UUID id,
      String bookingNo,
      BookingStatus status,
      UUID resourceId,
      LocalDateTime startAt,
      LocalDateTime endAt,
      UUID tenantId,
      LocalDateTime createdAt) {}

  public record ClientBookingListItem(
      UUID id,
      String bookingNo,
      String serviceName,
      String providerName,
      String date,
      String time,
      Integer durationMinutes,
      BookingStatus status,
      java.math.BigDecimal price) {}

  public record ClientBookingListResponse(
      java.util.List<ClientBookingListItem> items, int page, int size, long total) {}

  public record ClientBookingCancelRequest(@Size(max = 300) String reason) {}

  public record ClientBookingRescheduleRequest(@NotNull LocalDateTime newStartAt, @Size(max = 300) String reason) {}

  public record ClientBookingStatusResponse(UUID id, BookingStatus status, LocalDateTime updatedAt) {}

  public record ClientCatalogResourceSummary(
      UUID id,
      String name,
      String category,
      BigDecimal price,
      Integer durationMinutes,
      Double rating,
      String imageUrl,
      String merchantName) {}

  public record ClientCategorySummary(String key, String label, long count) {}

  public record ClientResourceDetailResponse(
      UUID id,
      String name,
      String description,
      String category,
      BigDecimal price,
      Integer durationMinutes,
      Double rating,
      MerchantSummary merchant,
      String imageUrl) {}

  /** Non-admin accounts must provide an email-formatted username; SYSTEM_ADMIN may still use plain username. */
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
      String canonicalRole,
      List<String> canonicalRoles,
      List<String> roleAliases,
      List<String> permissions,
      UUID merchantId,
      String sessionState,
      List<AuthContextOption> availableContexts,
      AuthContextOption activeContext) {
    public AuthMeResponse {
      roles = roles == null ? List.of() : Collections.unmodifiableList(List.copyOf(roles));
      canonicalRoles =
          canonicalRoles == null ? List.of() : Collections.unmodifiableList(List.copyOf(canonicalRoles));
      roleAliases =
          roleAliases == null ? List.of() : Collections.unmodifiableList(List.copyOf(roleAliases));
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
  public record AuthContextOption(String kind, UUID merchantId, String role) {}

  /** Body for {@code POST /api/auth/context/select}. Omit or leave {@code role} empty to behave like refresh. */
  public record ContextSelectRequest(UUID merchantId, String role) {}

  public record MerchantEnableRequest(@NotBlank String name, @NotBlank String slug) {}

  public record MerchantEnableResponse(
      UUID merchantId, String name, String slug, String ownerRole, String membershipStatus) {}

  public record TokenResponse(
      String accessToken,
      String tokenType,
      long expiresInSeconds,
      String role,
      List<String> roles,
      String canonicalRole,
      List<String> canonicalRoles,
      List<String> roleAliases,
      List<String> permissions) {
    public TokenResponse {
      roles = roles == null ? List.of() : Collections.unmodifiableList(List.copyOf(roles));
      canonicalRoles =
          canonicalRoles == null ? List.of() : Collections.unmodifiableList(List.copyOf(canonicalRoles));
      roleAliases =
          roleAliases == null ? List.of() : Collections.unmodifiableList(List.copyOf(roleAliases));
      permissions =
          permissions == null ? List.of() : Collections.unmodifiableList(List.copyOf(permissions));
    }
  }

  /** Allowed UI routes from {@code platform_pages} / {@code role_page_grants} for the current actor. */
  public record NavigationResponse(List<String> routeKeys, List<NavigationItem> items) {}

  public record NavigationItem(String routeKey, String path, String labelKey, int sortOrder) {}

  public record SystemUserSummary(
      UUID id,
      String username,
      Boolean enabled,
      String primaryRole,
      UUID primaryMerchantId,
      Long activeBindingsCount,
      List<String> roleCodes,
      LocalDateTime lastLoginAt,
      LocalDateTime updatedAt) {}

  public record SystemUserBindingSummary(
      Long bindingId, String roleCode, UUID merchantId, String status, List<String> permissions) {}

  public record SystemUserDetailResponse(
      UUID id,
      String username,
      Boolean enabled,
      String primaryRole,
      UUID primaryMerchantId,
      List<SystemUserBindingSummary> bindings,
      List<String> effectivePermissions,
      LocalDateTime lastLoginAt,
      LocalDateTime updatedAt) {}

  public record SystemUserStatusUpdateRequest(@NotNull Boolean enabled) {}

  public record SystemUserBindingUpsertRequest(
      @NotBlank String roleCode,
      UUID merchantId,
      @NotNull Boolean active) {}

  public record SystemUserBindingsUpdateRequest(
      @NotEmpty List<@jakarta.validation.Valid SystemUserBindingUpsertRequest> bindings) {}

  public record SystemRbacRoleResponse(String roleCode, List<String> permissions) {}

  public record SystemBookingTransitionRequest(
      @NotNull UUID merchantId,
      @NotNull BookingTransitionEvent event,
      String reason) {}

  public record HomepageConfigSection(String id, boolean enabled, int order, String contentKey) {}

  public record HomepageFallbackPolicy(boolean allowTenantDefault, boolean crossTenantFallback) {}

  public record HomepageConfigResponse(
      String tenantId,
      String locale,
      String pageVariant,
      List<HomepageConfigSection> sections,
      HomepageFallbackPolicy fallbackPolicy,
      String stateLegalityNotice) {}

  public record HomepageSeoResponse(
      String tenantId,
      String locale,
      String variant,
      String title,
      String description,
      String canonicalUrl,
      String robots,
      Map<String, Object> structuredData) {}

  public record HomepageTrackingEventRequest(
      @NotBlank String eventType,
      @NotBlank String tenantId,
      @NotBlank String locale,
      @NotBlank String sectionId,
      @NotBlank String campaign,
      String pageVariant,
      @NotNull Instant occurredAt,
      Map<String, Object> metadata) {
    public HomepageTrackingEventRequest {
      metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
  }

  public record HomepageTrackingBatchRequest(
      @NotEmpty @jakarta.validation.Valid List<HomepageTrackingEventRequest> events) {}

  public record HomepageTrackingAcceptResponse(int accepted, int rejected) {}
}
