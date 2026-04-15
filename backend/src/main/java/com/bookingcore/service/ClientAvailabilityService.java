package com.bookingcore.service;

import com.bookingcore.api.ApiDtos.AvailabilityResponse;
import com.bookingcore.api.ApiDtos.AvailabilitySlot;
import com.bookingcore.api.ApiDtos.ClientResourceAvailabilityResponse;
import com.bookingcore.api.ApiDtos.ClientResourceAvailabilitySlot;
import com.bookingcore.common.ApiException;
import com.bookingcore.modules.booking.AvailabilityException;
import com.bookingcore.modules.booking.AvailabilityExceptionRepository;
import com.bookingcore.modules.booking.Booking;
import com.bookingcore.modules.booking.BookingRepository;
import com.bookingcore.modules.booking.BookingStatus;
import com.bookingcore.modules.booking.BusinessHours;
import com.bookingcore.modules.booking.BusinessHoursRepository;
import com.bookingcore.modules.customization.CustomizationConfig;
import com.bookingcore.modules.customization.CustomizationConfigRepository;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.ResourceItem;
import com.bookingcore.modules.merchant.ResourceItemRepository;
import com.bookingcore.modules.service.ServiceItem;
import com.bookingcore.modules.service.ServiceItemRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bookingcore.security.PlatformPrincipal;
import com.bookingcore.security.PlatformUserRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ClientAvailabilityService {
  private final BookingCommandService bookingCommandService;
  private final ServiceItemRepository serviceItemRepository;
  private final BusinessHoursRepository businessHoursRepository;
  private final BookingRepository bookingRepository;
  private final CustomizationConfigRepository customizationConfigRepository;
  private final AvailabilityExceptionRepository availabilityExceptionRepository;
  private final ResourceItemRepository resourceItemRepository;
  private final ObjectMapper objectMapper;

  public ClientAvailabilityService(
      BookingCommandService bookingCommandService,
      ServiceItemRepository serviceItemRepository,
      BusinessHoursRepository businessHoursRepository,
      BookingRepository bookingRepository,
      CustomizationConfigRepository customizationConfigRepository,
      AvailabilityExceptionRepository availabilityExceptionRepository,
      ResourceItemRepository resourceItemRepository,
      ObjectMapper objectMapper) {
    this.bookingCommandService = bookingCommandService;
    this.serviceItemRepository = serviceItemRepository;
    this.businessHoursRepository = businessHoursRepository;
    this.bookingRepository = bookingRepository;
    this.customizationConfigRepository = customizationConfigRepository;
    this.availabilityExceptionRepository = availabilityExceptionRepository;
    this.resourceItemRepository = resourceItemRepository;
    this.objectMapper = objectMapper;
  }

  public AvailabilityResponse getAvailability(UUID merchantId, UUID serviceItemId, UUID resourceId, LocalDate date) {
    Merchant merchant = bookingCommandService.ensureMerchant(merchantId);
    ServiceItem service = serviceItemRepository.findByIdAndMerchantId(serviceItemId, merchantId)
        .orElseThrow(() -> new ApiException("Service not found"));
    if (resourceId != null) {
      resourceItemRepository.findByIdAndMerchantId(resourceId, merchantId)
          .orElseThrow(() -> new ApiException("Resource not found"));
    }

    List<BusinessHours> businessHours = businessHoursRepository.findByMerchantIdAndDayOfWeek(merchantId, date.getDayOfWeek());
    if (businessHours.isEmpty()) {
      return new AvailabilityResponse(date, List.of());
    }

    Integer buffer = customizationConfigRepository.findByMerchantId(merchant.getId())
        .map(CustomizationConfig::getBufferMinutes)
        .orElse(0);
    List<Booking> existingBookings = bookingRepository.findByMerchantIdAndStatusNotAndStartAtLessThanAndEndAtGreaterThan(
        merchantId, BookingStatus.CANCELLED, date.plusDays(1).atStartOfDay(), date.atStartOfDay().minusDays(1));
    List<AvailabilityException> exceptions = availabilityExceptionRepository
        .findByMerchantIdAndStartAtLessThanAndEndAtGreaterThan(merchantId, date.plusDays(1).atStartOfDay(), date.atStartOfDay());

    int stepMinutes = getStepMinutes(service.getDurationMinutes());
    List<AvailabilitySlot> slots = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    for (BusinessHours h : businessHours) {
      LocalDateTime cursor = date.atTime(h.getStartTime());
      LocalDateTime periodEnd = date.atTime(h.getEndTime());
      while (!cursor.plusMinutes(service.getDurationMinutes()).isAfter(periodEnd)) {
        LocalDateTime slotStart = cursor;
        LocalDateTime slotEnd = slotStart.plusMinutes(service.getDurationMinutes());
        boolean past = slotStart.isBefore(now);
        boolean blockedByException = exceptions.stream()
            .anyMatch(ex -> "BLOCK".equalsIgnoreCase(ex.getType())
                && ex.getStartAt().isBefore(slotEnd)
                && ex.getEndAt().isAfter(slotStart));
        boolean conflict = existingBookings.stream().anyMatch(b ->
            b.getStartAt().isBefore(slotEnd.plusMinutes(buffer))
                && b.getEndAt().isAfter(slotStart.minusMinutes(buffer)));
        boolean available = !past && !blockedByException && !conflict;
        slots.add(new AvailabilitySlot(slotStart, slotEnd, available, available ? "AVAILABLE" : "UNAVAILABLE"));
        cursor = cursor.plusMinutes(stepMinutes);
      }
    }
    slots.sort(Comparator.comparing(AvailabilitySlot::startAt));
    return new AvailabilityResponse(date, slots);
  }

  public ClientResourceAvailabilityResponse getResourceAvailability(UUID resourceId, LocalDate date) {
    ResourceItem resource =
        resourceItemRepository
            .findById(resourceId)
            .orElseThrow(() -> new ApiException("Resource not found", HttpStatus.NOT_FOUND));
    Merchant merchant = resource.getMerchant();
    assertTenantScopedPrincipalCanReadResource(merchant.getId());
    // Keep client visibility checks centralized so invite-only tenants still require membership.
    bookingCommandService.ensureMerchant(merchant.getId());

    List<BusinessHours> businessHours =
        businessHoursRepository.findByMerchantIdAndDayOfWeek(merchant.getId(), date.getDayOfWeek());
    if (businessHours.isEmpty()) {
      return new ClientResourceAvailabilityResponse(date, List.of());
    }

    int slotMinutes = inferSlotMinutes(resource);
    int capacity = Optional.ofNullable(resource.getCapacity()).filter(c -> c > 0).orElse(1);
    Integer buffer =
        customizationConfigRepository
            .findByMerchantId(merchant.getId())
            .map(CustomizationConfig::getBufferMinutes)
            .orElse(0);
    List<Booking> existingBookings =
        bookingRepository.findByMerchantIdAndStatusNotAndStartAtLessThanAndEndAtGreaterThan(
            merchant.getId(),
            BookingStatus.CANCELLED,
            date.plusDays(1).atStartOfDay(),
            date.atStartOfDay());
    List<AvailabilityException> exceptions =
        availabilityExceptionRepository.findByMerchantIdAndStartAtLessThanAndEndAtGreaterThan(
            merchant.getId(), date.plusDays(1).atStartOfDay(), date.atStartOfDay());

    Set<UUID> scopedServiceItemIds = resolveScopedServiceItemIds(resource);
    List<ClientResourceAvailabilitySlot> slots = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    for (BusinessHours h : businessHours) {
      LocalDateTime cursor = date.atTime(h.getStartTime());
      LocalDateTime periodEnd = date.atTime(h.getEndTime());
      while (!cursor.plusMinutes(slotMinutes).isAfter(periodEnd)) {
        LocalDateTime slotStart = cursor;
        LocalDateTime slotEnd = slotStart.plusMinutes(slotMinutes);
        boolean past = slotStart.isBefore(now);
        boolean blockedByException =
            exceptions.stream()
                .anyMatch(
                    ex ->
                        "BLOCK".equalsIgnoreCase(ex.getType())
                            && ex.getStartAt().isBefore(slotEnd)
                            && ex.getEndAt().isAfter(slotStart));
        long occupiedCount =
            existingBookings.stream()
                .filter(
                    b ->
                        !scopedServiceItemIds.isEmpty()
                            && scopedServiceItemIds.contains(b.getServiceItem().getId()))
                .filter(
                    b ->
                        b.getStartAt().isBefore(slotEnd.plusMinutes(buffer))
                            && b.getEndAt().isAfter(slotStart.minusMinutes(buffer)))
                .count();
        int capacityRemaining = Math.max(0, capacity - Math.toIntExact(occupiedCount));
        boolean available = !past && !blockedByException && capacityRemaining > 0;
        slots.add(
            new ClientResourceAvailabilitySlot(slotStart, slotEnd, available, capacityRemaining));
        cursor = cursor.plusMinutes(slotMinutes);
      }
    }
    slots.sort(Comparator.comparing(ClientResourceAvailabilitySlot::startAt));
    return new ClientResourceAvailabilityResponse(date, slots);
  }

  private int getStepMinutes(Integer durationMinutes) {
    if (durationMinutes == null) {
      return 30;
    }
    if (durationMinutes <= 30) {
      return 15;
    }
    if (durationMinutes <= 60) {
      return 30;
    }
    return 60;
  }

  private int inferSlotMinutes(ResourceItem resource) {
    String configuredType = resource.getType();
    if (configuredType != null && configuredType.toUpperCase().contains("LONG")) {
      return 60;
    }
    return 30;
  }

  private Set<UUID> resolveScopedServiceItemIds(ResourceItem resource) {
    String raw = resource.getServiceItemsJson();
    if (raw == null || raw.isBlank()) {
      return Set.of();
    }
    try {
      List<UUID> ids = objectMapper.readValue(raw, new TypeReference<List<UUID>>() {});
      return new HashSet<>(ids);
    } catch (Exception ignored) {
      return Set.of();
    }
  }

  private void assertTenantScopedPrincipalCanReadResource(UUID merchantId) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof PlatformPrincipal p)) {
      return;
    }
    if ((p.role() == PlatformUserRole.MERCHANT || p.role() == PlatformUserRole.SUB_MERCHANT)
        && p.merchantId() != null
        && !merchantId.equals(p.merchantId())) {
      throw new ApiException("Resource not found", HttpStatus.NOT_FOUND);
    }
  }
}
