package com.bookingcore.api;

import com.bookingcore.modules.booking.BookingStatus;
import com.bookingcore.modules.booking.BookingTransitionEvent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public final class ApiDtos {
  private ApiDtos() {}

  public record CreateMerchantRequest(@NotBlank String name, @NotBlank String slug) {}

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

  public record DomainTemplateRequest(@NotBlank String domainName, @NotBlank String fieldsJson) {}

  public record SystemSettingsRequest(String emailTemplate, String smsTemplate, String maintenanceAnnouncement) {}

  public record MerchantSummary(Long id, String name, String slug, Boolean active) {}

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
      @NotBlank String inviteCode,
      @NotBlank String customerName,
      @NotBlank String customerContact,
      @NotNull Boolean agreeTerms,
      Map<String, String> dynamicFieldValues) {}

  public record ClientProfileResponse(Boolean authenticated, String role, String suggestedName, String suggestedContact) {}

  public record BookingSubmitResponse(
      Long bookingId, String bookingCode, LocalDateTime startAt, LocalDateTime endAt, String queryPath) {}

  public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

  public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds, String role) {}

  /** Allowed UI routes from {@code platform_pages} / {@code role_page_grants} for the current actor. */
  public record NavigationResponse(List<String> routeKeys, List<NavigationItem> items) {}

  public record NavigationItem(String routeKey, String path, String labelKey, int sortOrder) {}
}
