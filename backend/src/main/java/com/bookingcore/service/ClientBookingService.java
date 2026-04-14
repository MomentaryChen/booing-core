package com.bookingcore.service;

import com.bookingcore.api.ApiDtos.ClientBookingCreateRequest;
import com.bookingcore.api.ApiDtos.ClientBookingCreateResponse;
import com.bookingcore.api.ApiDtos.ClientBookingListItem;
import com.bookingcore.api.ApiDtos.ClientBookingListResponse;
import com.bookingcore.common.ApiException;
import com.bookingcore.modules.booking.Booking;
import com.bookingcore.modules.booking.BookingRepository;
import com.bookingcore.modules.booking.BookingStatus;
import com.bookingcore.modules.merchant.ResourceItem;
import com.bookingcore.modules.merchant.ResourceItemRepository;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.service.ServiceItem;
import com.bookingcore.modules.service.ServiceItemRepository;
import com.bookingcore.service.clientbooking.ClientBookingSpecifications;
import com.bookingcore.service.clientbooking.ClientBookingValidationContext;
import com.bookingcore.service.clientbooking.ClientBookingValidationStrategy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientBookingService {
  private final ResourceItemRepository resourceItemRepository;
  private final ServiceItemRepository serviceItemRepository;
  private final BookingRepository bookingRepository;
  private final MerchantAccessService merchantAccessService;
  private final ObjectMapper objectMapper;
  private final Map<String, ClientBookingValidationStrategy> strategies;

  public ClientBookingService(
      ResourceItemRepository resourceItemRepository,
      ServiceItemRepository serviceItemRepository,
      BookingRepository bookingRepository,
      MerchantAccessService merchantAccessService,
      ObjectMapper objectMapper,
      List<ClientBookingValidationStrategy> strategies) {
    this.resourceItemRepository = resourceItemRepository;
    this.serviceItemRepository = serviceItemRepository;
    this.bookingRepository = bookingRepository;
    this.merchantAccessService = merchantAccessService;
    this.objectMapper = objectMapper;
    this.strategies = toStrategyMap(strategies);
  }

  @Transactional(readOnly = true)
  public ClientBookingListResponse listMyBookings(String tab, int page, int size) {
    PlatformUser user = merchantAccessService.currentPlatformUserOrNull();
    if (user == null) {
      throw new ApiException("Unauthorized", HttpStatus.UNAUTHORIZED);
    }
    int safePage = Math.max(0, page);
    int safeSize = Math.min(Math.max(1, size), 100);
    var spec = ClientBookingSpecifications.forClientMyBookingsTab(user.getId(), tab);
    Page<Booking> result =
        bookingRepository.findAll(
            spec, PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "startAt")));
    List<ClientBookingListItem> items = result.getContent().stream().map(this::toListItem).toList();
    return new ClientBookingListResponse(items, safePage, safeSize, result.getTotalElements());
  }

  @Transactional
  public ClientBookingCreateResponse createBooking(ClientBookingCreateRequest request) {
    PlatformUser user = merchantAccessService.currentPlatformUserOrNull();
    if (user == null) {
      throw new ApiException("Unauthorized", HttpStatus.UNAUTHORIZED);
    }
    if (request.startAt().isBefore(LocalDateTime.now())) {
      throw new ApiException("Booking startAt must be in the future", HttpStatus.BAD_REQUEST);
    }

    ResourceItem resource =
        resourceItemRepository
            .findWithLockById(request.resourceId())
            .orElseThrow(() -> new ApiException("Resource not found", HttpStatus.NOT_FOUND));
    assertClientCanAccessResource(resource);

    resolveStrategy(resource.getType())
        .validate(new ClientBookingValidationContext(resource, request.startAt()));

    List<Long> scopedServiceItemIds = parseServiceItemIds(resource.getServiceItemsJson());
    if (scopedServiceItemIds.isEmpty()) {
      throw new ApiException(
          "Resource strategy rejected booking: no service scope",
          HttpStatus.UNPROCESSABLE_ENTITY,
          "BOOKING_RULE_REJECTED");
    }
    // Lock the same conflict scope to prevent concurrent double booking across shared service scopes.
    List<ServiceItem> lockedScopedServices =
        serviceItemRepository.findByMerchantIdAndIdIn(
            resource.getMerchant().getId(), scopedServiceItemIds);
    if (lockedScopedServices.size() != scopedServiceItemIds.size()) {
      throw new ApiException(
          "Resource strategy rejected booking: invalid service scope",
          HttpStatus.UNPROCESSABLE_ENTITY,
          "BOOKING_RULE_REJECTED");
    }
    ServiceItem serviceItem = resolvePrimaryServiceItem(resource, scopedServiceItemIds);
    LocalDateTime endAt = request.startAt().plusMinutes(serviceItem.getDurationMinutes());

    boolean conflict =
        !bookingRepository
            .findByMerchantIdAndServiceItemIdInAndStatusNotAndStartAtLessThanAndEndAtGreaterThan(
                resource.getMerchant().getId(),
                scopedServiceItemIds,
                BookingStatus.CANCELLED,
                endAt,
                request.startAt())
            .isEmpty();
    if (conflict) {
      throw new ApiException(
          "Selected time conflicts with an existing booking",
          HttpStatus.CONFLICT,
          "BOOKING_SLOT_CONFLICT");
    }

    Booking booking = new Booking();
    booking.setMerchant(resource.getMerchant());
    booking.setServiceItem(serviceItem);
    booking.setStartAt(request.startAt());
    booking.setEndAt(endAt);
    booking.setCustomerName("CLIENT");
    booking.setCustomerContact(request.notes() == null ? "N/A" : request.notes());
    booking.setPlatformUser(user);
    booking.setStatus(BookingStatus.PENDING);
    Booking saved = bookingRepository.save(booking);

    return new ClientBookingCreateResponse(
        saved.getId(),
        "BK-" + saved.getId(),
        saved.getStatus(),
        resource.getId(),
        saved.getStartAt(),
        saved.getEndAt(),
        resource.getMerchant().getId(),
        LocalDateTime.now());
  }

  private ClientBookingListItem toListItem(Booking b) {
    LocalDateTime start = b.getStartAt();
    return new ClientBookingListItem(
        b.getId(),
        "BK-" + b.getId(),
        b.getServiceItem().getName(),
        b.getMerchant().getName(),
        start.toLocalDate().toString(),
        String.format("%02d:%02d", start.getHour(), start.getMinute()),
        b.getServiceItem().getDurationMinutes(),
        b.getStatus(),
        b.getServiceItem().getPrice());
  }

  private void assertClientCanAccessResource(ResourceItem resource) {
    try {
      merchantAccessService.assertClientCanAccessMerchant(resource.getMerchant());
    } catch (ApiException ex) {
      if (ex.getStatus() == HttpStatus.FORBIDDEN) {
        throw new ApiException("Resource not found", HttpStatus.NOT_FOUND);
      }
      throw ex;
    }
  }

  private ServiceItem resolvePrimaryServiceItem(ResourceItem resource, List<Long> serviceItemIds) {
    Long serviceItemId = serviceItemIds.get(0);
    return serviceItemRepository
        .findByIdAndMerchantId(serviceItemId, resource.getMerchant().getId())
        .orElseThrow(
            () ->
                new ApiException(
                    "Resource strategy rejected booking: invalid service scope",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "BOOKING_RULE_REJECTED"));
  }

  private ClientBookingValidationStrategy resolveStrategy(String type) {
    String key = normalizeType(type);
    ClientBookingValidationStrategy strategy = strategies.get(key);
    if (strategy == null) {
      throw new ApiException(
          "Resource booking strategy is not registered",
          HttpStatus.UNPROCESSABLE_ENTITY,
          "BOOKING_STRATEGY_NOT_REGISTERED");
    }
    return strategy;
  }

  private List<Long> parseServiceItemIds(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(raw, new TypeReference<List<Long>>() {});
    } catch (Exception ex) {
      throw new ApiException(
          "Resource strategy rejected booking: invalid service scope",
          HttpStatus.UNPROCESSABLE_ENTITY,
          "BOOKING_RULE_REJECTED");
    }
  }

  private Map<String, ClientBookingValidationStrategy> toStrategyMap(
      List<ClientBookingValidationStrategy> strategyList) {
    Map<String, ClientBookingValidationStrategy> map = new HashMap<>();
    for (ClientBookingValidationStrategy strategy : strategyList) {
      map.put(normalizeType(strategy.resourceType()), strategy);
    }
    return Map.copyOf(map);
  }

  private String normalizeType(String resourceType) {
    return resourceType == null ? "" : resourceType.trim().toUpperCase(Locale.ROOT);
  }
}
