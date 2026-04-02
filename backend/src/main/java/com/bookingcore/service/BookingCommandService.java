package com.bookingcore.service;

import com.bookingcore.api.ApiDtos.BookingLockRequest;
import com.bookingcore.api.ApiDtos.BookingLockResponse;
import com.bookingcore.api.ApiDtos.ClientBookingRequest;
import com.bookingcore.common.ApiException;
import com.bookingcore.modules.booking.AvailabilityException;
import com.bookingcore.modules.booking.AvailabilityExceptionRepository;
import com.bookingcore.modules.booking.Booking;
import com.bookingcore.modules.booking.BookingRepository;
import com.bookingcore.modules.booking.BookingStatus;
import com.bookingcore.modules.booking.BookingTransitionEvent;
import com.bookingcore.modules.booking.BusinessHoursRepository;
import com.bookingcore.modules.customization.CustomizationConfig;
import com.bookingcore.modules.customization.CustomizationConfigRepository;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.MerchantRepository;
import com.bookingcore.modules.merchant.ResourceItemRepository;
import com.bookingcore.modules.service.ServiceItem;
import com.bookingcore.modules.service.ServiceItemRepository;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingCommandService {
  private static final long BOOKING_LOCK_TTL_SECONDS = 180;
  private static final ConcurrentHashMap<String, PendingBookingLock> PENDING_BOOKING_LOCKS = new ConcurrentHashMap<>();

  private final MerchantRepository merchantRepository;
  private final ServiceItemRepository serviceItemRepository;
  private final BusinessHoursRepository businessHoursRepository;
  private final BookingRepository bookingRepository;
  private final CustomizationConfigRepository customizationConfigRepository;
  private final AvailabilityExceptionRepository availabilityExceptionRepository;
  private final ResourceItemRepository resourceItemRepository;
  private final BookingStateMachineService bookingStateMachineService;

  public BookingCommandService(
      MerchantRepository merchantRepository,
      ServiceItemRepository serviceItemRepository,
      BusinessHoursRepository businessHoursRepository,
      BookingRepository bookingRepository,
      CustomizationConfigRepository customizationConfigRepository,
      AvailabilityExceptionRepository availabilityExceptionRepository,
      ResourceItemRepository resourceItemRepository,
      BookingStateMachineService bookingStateMachineService) {
    this.merchantRepository = merchantRepository;
    this.serviceItemRepository = serviceItemRepository;
    this.businessHoursRepository = businessHoursRepository;
    this.bookingRepository = bookingRepository;
    this.customizationConfigRepository = customizationConfigRepository;
    this.availabilityExceptionRepository = availabilityExceptionRepository;
    this.resourceItemRepository = resourceItemRepository;
    this.bookingStateMachineService = bookingStateMachineService;
  }

  public Merchant ensureMerchant(Long merchantId) {
    return merchantRepository.findById(merchantId).orElseThrow(() -> new ApiException("Merchant not found"));
  }

  public BookingLockResponse lockBookingSlot(BookingLockRequest request) {
    Merchant merchant = ensureMerchant(request.merchantId());
    serviceItemRepository.findByIdAndMerchantId(request.serviceItemId(), merchant.getId())
        .orElseThrow(() -> new ApiException("Service not found"));
    if (request.resourceId() != null) {
      resourceItemRepository.findByIdAndMerchantId(request.resourceId(), merchant.getId())
          .orElseThrow(() -> new ApiException("Resource not found"));
    }

    String lockKey = toLockKey(request.merchantId(), request.serviceItemId(), request.resourceId(), request.startAt());
    PendingBookingLock existing = PENDING_BOOKING_LOCKS.get(lockKey);
    if (existing != null && existing.expiresAt().isAfter(LocalDateTime.now())) {
      throw new ApiException("Selected slot is currently being held. Please choose another time.");
    }

    String lockId = UUID.randomUUID().toString();
    LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(BOOKING_LOCK_TTL_SECONDS);
    PENDING_BOOKING_LOCKS.put(lockKey, new PendingBookingLock(lockId, lockKey, expiresAt));
    return new BookingLockResponse(lockId, expiresAt);
  }

  public void assertValidLockAndConsume(ClientBookingRequest request) {
    String expectedLockKey = toLockKey(
        request.merchantId(), request.serviceItemId(), request.resourceId(), request.startAt());
    PendingBookingLock lock = PENDING_BOOKING_LOCKS.get(expectedLockKey);
    if (lock == null || !lock.lockId().equals(request.lockId()) || lock.expiresAt().isBefore(LocalDateTime.now())) {
      throw new ApiException("Booking lock expired. Please re-select a timeslot.");
    }
    PENDING_BOOKING_LOCKS.remove(expectedLockKey);
  }

  @Transactional
  public Booking createBookingWithValidation(
      Merchant merchant, ServiceItem service, LocalDateTime startAt, String customerName, String customerContact) {
    LocalDateTime endAt = startAt.plusMinutes(service.getDurationMinutes());
    Integer buffer = customizationConfigRepository.findByMerchantId(merchant.getId())
        .map(CustomizationConfig::getBufferMinutes)
        .orElse(0);
    LocalDateTime endWithBuffer = endAt.plusMinutes(buffer);

    DayOfWeek day = startAt.getDayOfWeek();
    LocalTime startTime = startAt.toLocalTime();
    LocalTime endTime = endAt.toLocalTime();
    boolean insideBusinessHours = businessHoursRepository.findByMerchantIdAndDayOfWeek(merchant.getId(), day).stream()
        .anyMatch(h -> !startTime.isBefore(h.getStartTime()) && !endTime.isAfter(h.getEndTime()));
    if (!insideBusinessHours) {
      throw new ApiException("Booking time is outside business hours");
    }

    List<AvailabilityException> exceptions = availabilityExceptionRepository
        .findByMerchantIdAndStartAtLessThanAndEndAtGreaterThan(merchant.getId(), endAt, startAt);
    boolean blocked = exceptions.stream().anyMatch(ex -> "BLOCK".equalsIgnoreCase(ex.getType()));
    if (blocked) {
      throw new ApiException("Booking falls into blocked exception window");
    }

    List<Booking> conflicts = bookingRepository.findByMerchantIdAndStatusNotAndStartAtLessThanAndEndAtGreaterThan(
        merchant.getId(), BookingStatus.CANCELLED, endWithBuffer, startAt.minusMinutes(buffer));
    if (!conflicts.isEmpty()) {
      throw new ApiException("Selected time conflicts with an existing booking");
    }

    Booking booking = new Booking();
    booking.setMerchant(merchant);
    booking.setServiceItem(service);
    booking.setStartAt(startAt);
    booking.setEndAt(endAt);
    booking.setCustomerName(customerName);
    booking.setCustomerContact(customerContact);
    booking.setStatus(BookingStatus.DRAFT);
    Booking saved = bookingRepository.save(booking);
    bookingStateMachineService.transition(saved, BookingTransitionEvent.SUBMIT);
    bookingStateMachineService.transition(saved, BookingTransitionEvent.PAY_SUCCESS);
    return bookingRepository.save(saved);
  }

  @Transactional
  public Booking transitionBooking(Long merchantId, Long bookingId, BookingTransitionEvent event) {
    Booking booking = bookingRepository.findByIdAndMerchantId(bookingId, merchantId)
        .orElseThrow(() -> new ApiException("Booking not found"));
    bookingStateMachineService.transition(booking, event);
    return bookingRepository.save(booking);
  }

  private String toLockKey(Long merchantId, Long serviceItemId, Long resourceId, LocalDateTime startAt) {
    String resourcePart = resourceId == null ? "auto" : String.valueOf(resourceId);
    return merchantId + ":" + serviceItemId + ":" + resourcePart + ":" + startAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
  }

  private record PendingBookingLock(String lockId, String key, LocalDateTime expiresAt) {}
}
