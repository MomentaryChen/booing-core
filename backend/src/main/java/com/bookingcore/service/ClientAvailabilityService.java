package com.bookingcore.service;

import com.bookingcore.api.ApiDtos.AvailabilityResponse;
import com.bookingcore.api.ApiDtos.AvailabilitySlot;
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
import com.bookingcore.modules.merchant.ResourceItemRepository;
import com.bookingcore.modules.service.ServiceItem;
import com.bookingcore.modules.service.ServiceItemRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

  public ClientAvailabilityService(
      BookingCommandService bookingCommandService,
      ServiceItemRepository serviceItemRepository,
      BusinessHoursRepository businessHoursRepository,
      BookingRepository bookingRepository,
      CustomizationConfigRepository customizationConfigRepository,
      AvailabilityExceptionRepository availabilityExceptionRepository,
      ResourceItemRepository resourceItemRepository) {
    this.bookingCommandService = bookingCommandService;
    this.serviceItemRepository = serviceItemRepository;
    this.businessHoursRepository = businessHoursRepository;
    this.bookingRepository = bookingRepository;
    this.customizationConfigRepository = customizationConfigRepository;
    this.availabilityExceptionRepository = availabilityExceptionRepository;
    this.resourceItemRepository = resourceItemRepository;
  }

  public AvailabilityResponse getAvailability(Long merchantId, Long serviceItemId, Long resourceId, LocalDate date) {
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
}
