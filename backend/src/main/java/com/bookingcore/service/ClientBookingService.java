package com.bookingcore.service;

import com.bookingcore.api.ApiDtos.ClientBookingCreateRequest;
import com.bookingcore.api.ApiDtos.ClientBookingCreateResponse;
import com.bookingcore.api.ApiDtos.ClientBookingListItem;
import com.bookingcore.api.ApiDtos.ClientBookingListResponse;
import com.bookingcore.api.ApiDtos.ClientBookingRescheduleRequest;
import com.bookingcore.api.ApiDtos.ClientBookingStatusResponse;
import com.bookingcore.api.ApiDtos.ClientCatalogResourceSummary;
import com.bookingcore.api.ApiDtos.ClientCategorySummary;
import com.bookingcore.api.ApiDtos.ClientResourceDetailResponse;
import com.bookingcore.api.ApiDtos.MerchantSummary;
import com.bookingcore.common.ApiException;
import com.bookingcore.modules.booking.Booking;
import com.bookingcore.modules.booking.BookingRepository;
import com.bookingcore.modules.booking.BookingStatus;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.MerchantRepository;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ClientBookingService {
  private final ResourceItemRepository resourceItemRepository;
  private final ServiceItemRepository serviceItemRepository;
  private final MerchantRepository merchantRepository;
  private final BookingRepository bookingRepository;
  private final MerchantAccessService merchantAccessService;
  private final ObjectMapper objectMapper;
  private final Map<String, ClientBookingValidationStrategy> strategies;
  private final PlatformAuditService platformAuditService;

  public ClientBookingService(
      ResourceItemRepository resourceItemRepository,
      ServiceItemRepository serviceItemRepository,
      MerchantRepository merchantRepository,
      BookingRepository bookingRepository,
      MerchantAccessService merchantAccessService,
      ObjectMapper objectMapper,
      List<ClientBookingValidationStrategy> strategies,
      PlatformAuditService platformAuditService) {
    this.resourceItemRepository = resourceItemRepository;
    this.serviceItemRepository = serviceItemRepository;
    this.merchantRepository = merchantRepository;
    this.bookingRepository = bookingRepository;
    this.merchantAccessService = merchantAccessService;
    this.objectMapper = objectMapper;
    this.strategies = toStrategyMap(strategies);
    this.platformAuditService = platformAuditService;
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

    List<UUID> scopedServiceItemIds = new ArrayList<>(parseServiceItemIds(resource.getServiceItemsJson()));
    Collections.sort(scopedServiceItemIds);
    if (scopedServiceItemIds.isEmpty()) {
      throw new ApiException(
          "Resource strategy rejected booking: no service scope",
          HttpStatus.UNPROCESSABLE_ENTITY,
          "BOOKING_RULE_REJECTED");
    }
    // Lock scoped ServiceItem rows (PESSIMISTIC_WRITE on repository method) so two resources sharing
    // the same service cannot both pass the overlap check before insert.
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

    // Pessimistic lock overlapping rows in the same service scope to close the TOCTOU window
    // when multiple resources share services (resource rows are locked per-resource only).
    boolean conflict =
        !bookingRepository
            .findOverlappingBookingsForUpdate(
                resource.getMerchant().getId(),
                scopedServiceItemIds,
                BookingStatus.CANCELLED,
                endAt,
                request.startAt(),
                null)
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

  @Transactional
  public ClientBookingStatusResponse cancelMyBooking(UUID bookingId, String reason) {
    PlatformUser user = merchantAccessService.currentPlatformUserOrNull();
    if (user == null) {
      throw new ApiException("Unauthorized", HttpStatus.UNAUTHORIZED);
    }
    // Same row lock as reschedule to avoid interleaving: cancel vs reschedule must serialize.
    Booking booking =
        bookingRepository
            .findByIdAndPlatformUserIdForUpdate(bookingId, user.getId())
            .orElseThrow(() -> new ApiException("Booking not found", HttpStatus.NOT_FOUND));
    Set<BookingStatus> cancellable = Set.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);
    if (!cancellable.contains(booking.getStatus())) {
      throw new ApiException("Booking cannot be cancelled in current status", HttpStatus.CONFLICT);
    }
    booking.setStatus(BookingStatus.CANCELLED);
    booking.setCustomerContact(appendCancellationReason(booking.getCustomerContact(), reason));
    Booking saved = bookingRepository.save(booking);
    return new ClientBookingStatusResponse(saved.getId(), saved.getStatus(), LocalDateTime.now());
  }

  @Transactional
  public ClientBookingStatusResponse rescheduleMyBooking(
      UUID bookingId, ClientBookingRescheduleRequest request) {
    PlatformUser user = merchantAccessService.currentPlatformUserOrNull();
    if (user == null) {
      throw new ApiException("Unauthorized", HttpStatus.UNAUTHORIZED);
    }
    if (request.newStartAt().isBefore(LocalDateTime.now())) {
      throw new ApiException("Booking startAt must be in the future", HttpStatus.BAD_REQUEST);
    }
    Booking scopeSource =
        bookingRepository
            .findByIdAndPlatformUserId(bookingId, user.getId())
            .orElseThrow(() -> new ApiException("Booking not found", HttpStatus.NOT_FOUND));
    // Keep lock order aligned with createBooking: service scope first, then booking row.
    List<UUID> scopedServiceItemIds = new ArrayList<>(resolveScopedServiceItemIdsForBooking(scopeSource));
    Collections.sort(scopedServiceItemIds);
    List<ServiceItem> lockedScopedServices =
        serviceItemRepository.findByMerchantIdAndIdIn(scopeSource.getMerchant().getId(), scopedServiceItemIds);
    if (lockedScopedServices.size() != scopedServiceItemIds.size()) {
      throw new ApiException(
          "Resource strategy rejected booking: invalid service scope",
          HttpStatus.UNPROCESSABLE_ENTITY,
          "BOOKING_RULE_REJECTED");
    }
    Booking booking =
        bookingRepository
            .findByIdAndPlatformUserIdForUpdate(bookingId, user.getId())
            .orElseThrow(() -> new ApiException("Booking not found", HttpStatus.NOT_FOUND));
    Set<BookingStatus> reschedulable = Set.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);
    if (!reschedulable.contains(booking.getStatus())) {
      HttpStatus stateErrorStatus =
          booking.getStatus() == BookingStatus.CANCELLED
              ? HttpStatus.CONFLICT
              : HttpStatus.UNPROCESSABLE_ENTITY;
      throw new ApiException("Booking cannot be rescheduled in current status", stateErrorStatus);
    }
    int durationMinutes = booking.getServiceItem().getDurationMinutes();
    LocalDateTime newEndAt = request.newStartAt().plusMinutes(durationMinutes);
    LocalDateTime originalStartAt = booking.getStartAt();
    LocalDateTime originalEndAt = booking.getEndAt();
    boolean conflict =
        !bookingRepository
            .findOverlappingBookingsForUpdate(
                booking.getMerchant().getId(),
                scopedServiceItemIds,
                BookingStatus.CANCELLED,
                newEndAt,
                request.newStartAt(),
                booking.getId())
            .isEmpty();
    if (conflict) {
      throw new ApiException(
          "Selected time conflicts with an existing booking",
          HttpStatus.CONFLICT,
          "BOOKING_SLOT_CONFLICT");
    }
    booking.setStartAt(request.newStartAt());
    booking.setEndAt(newEndAt);
    booking.setCustomerContact(appendRescheduleReason(booking.getCustomerContact(), request.reason()));
    Booking saved = bookingRepository.save(booking);
    platformAuditService.recordForCurrentUser(
        "client.booking.reschedule",
        "booking",
        saved.getId(),
        "from="
            + originalStartAt
            + "->"
            + originalEndAt
            + ",to="
            + request.newStartAt()
            + "->"
            + newEndAt
            + ",reason="
            + (request.reason() == null ? "" : request.reason().trim()));
    return new ClientBookingStatusResponse(saved.getId(), saved.getStatus(), LocalDateTime.now());
  }

  @Transactional(readOnly = true)
  public List<ClientCatalogResourceSummary> listFeaturedResources(int limit) {
    List<ClientCatalogResourceSummary> all =
        listClientResources(null, null, "rating", 0, Integer.MAX_VALUE).items();
    int safeLimit = Math.max(1, Math.min(limit, 20));
    return all.stream().limit(safeLimit).toList();
  }

  public record ClientCatalogResourcesPage(List<ClientCatalogResourceSummary> items, int page, int size, long total) {}

  @Transactional(readOnly = true)
  public ClientCatalogResourcesPage listClientResources(
      String q, String category, String sort, int page, int size) {
    int safePage = Math.max(0, page);
    int safeSize = Math.min(Math.max(1, size), 100);
    String keyword = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
    String categoryFilter =
        category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
    List<ClientCatalogResourceSummary> flattened =
        merchantRepository.findByActiveTrueOrderByIdAsc().stream()
            .filter(this::clientCanAccessMerchant)
            .flatMap(
                merchant -> {
                  Map<UUID, ServiceItem> services = serviceById(merchant.getId());
                  return resourceItemRepository.findByMerchantId(merchant.getId()).stream()
                      .filter(r -> Boolean.TRUE.equals(r.getActive()))
                      .map(r -> toCatalogSummary(r, merchant, services));
                })
            .filter(item -> keyword.isBlank() || item.name().toLowerCase(Locale.ROOT).contains(keyword))
            .filter(
                item ->
                    categoryFilter.isBlank()
                        || "all".equals(categoryFilter)
                        || item.category().equalsIgnoreCase(categoryFilter))
            .sorted(comparatorForSort(sort))
            .toList();
    int from = Math.min(safePage * safeSize, flattened.size());
    int to = Math.min(from + safeSize, flattened.size());
    return new ClientCatalogResourcesPage(flattened.subList(from, to), safePage, safeSize, flattened.size());
  }

  @Transactional(readOnly = true)
  public List<ClientCategorySummary> listClientCategories() {
    var grouped =
        listClientResources(null, null, "relevance", 0, Integer.MAX_VALUE).items().stream()
            .collect(
                java.util.stream.Collectors.groupingBy(
                    item -> item.category() == null ? "general" : item.category().toLowerCase(Locale.ROOT),
                    java.util.stream.Collectors.counting()));
    return grouped.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> new ClientCategorySummary(entry.getKey(), entry.getKey(), entry.getValue()))
        .toList();
  }

  @Transactional(readOnly = true)
  public ClientResourceDetailResponse getResourceDetail(UUID resourceId) {
    ResourceItem resource =
        resourceItemRepository
            .findById(resourceId)
            .orElseThrow(() -> new ApiException("Resource not found", HttpStatus.NOT_FOUND));
    assertClientCanAccessResource(resource);
    Merchant merchant = resource.getMerchant();
    ServiceItem primary = resolvePrimaryServiceFromResource(resource, merchant.getId());
    return new ClientResourceDetailResponse(
        resource.getId(),
        resource.getName(),
        resource.getCategory(),
        resource.getCategory(),
        resource.getPrice(),
        primary == null ? 60 : primary.getDurationMinutes(),
        4.5d,
        new MerchantSummary(merchant.getId(), merchant.getName(), merchant.getSlug(), merchant.getActive()),
        primary == null ? null : primary.getImageUrl());
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

  private ServiceItem resolvePrimaryServiceItem(ResourceItem resource, List<UUID> serviceItemIds) {
    UUID serviceItemId = serviceItemIds.get(0);
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

  private List<UUID> parseServiceItemIds(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(raw, new TypeReference<List<UUID>>() {});
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

  private String appendCancellationReason(String customerContact, String reason) {
    if (reason == null || reason.isBlank()) {
      return customerContact;
    }
    String existing = customerContact == null ? "" : customerContact;
    String suffix = " | cancelReason=" + reason.trim();
    if (existing.contains("cancelReason=")) {
      return existing;
    }
    return (existing + suffix).trim();
  }

  private String appendRescheduleReason(String customerContact, String reason) {
    if (!StringUtils.hasText(reason)) {
      return customerContact;
    }
    String existing = customerContact == null ? "" : customerContact;
    String suffix = " | rescheduleReason=" + reason.trim();
    if (existing.contains("rescheduleReason=")) {
      return existing;
    }
    return (existing + suffix).trim();
  }

  private boolean clientCanAccessMerchant(Merchant merchant) {
    try {
      merchantAccessService.assertClientCanAccessMerchant(merchant);
      return true;
    } catch (ApiException ex) {
      return false;
    }
  }

  private Map<UUID, ServiceItem> serviceById(UUID merchantId) {
    return serviceItemRepository.findByMerchantId(merchantId).stream()
        .collect(java.util.stream.Collectors.toMap(ServiceItem::getId, s -> s, (a, b) -> a));
  }

  private ClientCatalogResourceSummary toCatalogSummary(
      ResourceItem resource, Merchant merchant, Map<UUID, ServiceItem> serviceById) {
    ServiceItem primary = resolvePrimaryServiceFromResource(resource, merchant.getId(), serviceById);
    return new ClientCatalogResourceSummary(
        resource.getId(),
        resource.getName(),
        resource.getCategory(),
        resource.getPrice(),
        primary == null ? 60 : primary.getDurationMinutes(),
        4.5d,
        primary == null ? null : primary.getImageUrl(),
        merchant.getName());
  }

  private ServiceItem resolvePrimaryServiceFromResource(ResourceItem resource, UUID merchantId) {
    return resolvePrimaryServiceFromResource(resource, merchantId, serviceById(merchantId));
  }

  private ServiceItem resolvePrimaryServiceFromResource(
      ResourceItem resource, UUID merchantId, Map<UUID, ServiceItem> serviceById) {
    List<UUID> ids = parseServiceItemIds(resource.getServiceItemsJson());
    if (ids.isEmpty()) {
      return null;
    }
    UUID first = ids.get(0);
    ServiceItem service = serviceById.get(first);
    if (service != null) {
      return service;
    }
    return serviceItemRepository.findByIdAndMerchantId(first, merchantId).orElse(null);
  }

  private Comparator<ClientCatalogResourceSummary> comparatorForSort(String sort) {
    String key = sort == null ? "relevance" : sort.trim();
    return switch (key) {
      case "priceAsc" -> Comparator.comparing(ClientCatalogResourceSummary::price, Comparator.nullsLast(Comparator.naturalOrder()));
      case "priceDesc" -> Comparator.comparing(ClientCatalogResourceSummary::price, Comparator.nullsLast(Comparator.reverseOrder()));
      case "rating" -> Comparator.comparing(ClientCatalogResourceSummary::rating, Comparator.nullsLast(Comparator.reverseOrder()));
      default -> Comparator.comparing(ClientCatalogResourceSummary::name, String.CASE_INSENSITIVE_ORDER);
    };
  }

  private List<UUID> resolveScopedServiceItemIdsForBooking(Booking booking) {
    UUID merchantId = booking.getMerchant().getId();
    UUID serviceItemId = booking.getServiceItem().getId();
    var scopedIds =
        resourceItemRepository.findByMerchantId(merchantId).stream()
            .map(ResourceItem::getServiceItemsJson)
            .map(this::parseServiceItemIds)
            .filter(ids -> ids.contains(serviceItemId))
            .flatMap(List::stream)
            .distinct()
            .toList();
    if (scopedIds.isEmpty()) {
      return List.of(serviceItemId);
    }
    return scopedIds;
  }
}
