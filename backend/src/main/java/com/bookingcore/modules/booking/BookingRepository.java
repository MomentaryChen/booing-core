package com.bookingcore.modules.booking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, UUID>, JpaSpecificationExecutor<Booking> {
  java.util.Optional<Booking> findByIdAndMerchantId(UUID id, UUID merchantId);
  java.util.Optional<Booking> findByIdAndPlatformUserId(UUID id, UUID platformUserId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT b FROM Booking b "
          + "JOIN FETCH b.serviceItem "
          + "JOIN FETCH b.merchant "
          + "WHERE b.id = :id AND b.platformUser.id = :platformUserId")
  java.util.Optional<Booking> findByIdAndPlatformUserIdForUpdate(
      @Param("id") UUID id, @Param("platformUserId") UUID platformUserId);
  List<Booking> findByMerchantIdOrderByStartAtAsc(UUID merchantId);
  List<Booking> findByMerchantIdAndStatusOrderByStartAtAsc(UUID merchantId, BookingStatus status);

  List<Booking> findByMerchantIdAndStatusNotAndStartAtLessThanAndEndAtGreaterThan(
      UUID merchantId, BookingStatus status, LocalDateTime endAt, LocalDateTime startAt);

  List<Booking>
      findByMerchantIdAndServiceItemIdInAndStatusNotAndStartAtLessThanAndEndAtGreaterThan(
          UUID merchantId,
          List<UUID> serviceItemIds,
          BookingStatus status,
          LocalDateTime endAt,
          LocalDateTime startAt);

  boolean existsByMerchantIdAndServiceItemIdAndStatusNotAndStartAtLessThanAndEndAtGreaterThanAndIdNot(
      UUID merchantId,
      UUID serviceItemId,
      BookingStatus status,
      LocalDateTime endAt,
      LocalDateTime startAt,
      UUID id);

  boolean existsByMerchantIdAndServiceItemIdInAndStatusNotAndStartAtLessThanAndEndAtGreaterThanAndIdNot(
      UUID merchantId,
      List<UUID> serviceItemIds,
      BookingStatus status,
      LocalDateTime endAt,
      LocalDateTime startAt,
      UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT b FROM Booking b "
          + "WHERE b.merchant.id = :merchantId "
          + "AND b.serviceItem.id IN :serviceItemIds "
          + "AND b.status <> :excludedStatus "
          + "AND b.startAt < :endAt "
          + "AND b.endAt > :startAt "
          + "AND (:excludedBookingId IS NULL OR b.id <> :excludedBookingId)")
  List<Booking> findOverlappingBookingsForUpdate(
      @Param("merchantId") UUID merchantId,
      @Param("serviceItemIds") List<UUID> serviceItemIds,
      @Param("excludedStatus") BookingStatus excludedStatus,
      @Param("endAt") LocalDateTime endAt,
      @Param("startAt") LocalDateTime startAt,
      @Param("excludedBookingId") UUID excludedBookingId);

  long countByMerchantIdAndServiceItemIdInAndStatusIn(
      UUID merchantId, List<UUID> serviceItemIds, List<BookingStatus> statuses);

  long countByMerchantIdAndServiceItemId(UUID merchantId, UUID serviceItemId);

  long countByStartAtGreaterThanEqualAndStartAtLessThan(LocalDateTime startInclusive, LocalDateTime endExclusive);

  long countByStartAtGreaterThanEqualAndStartAtLessThanAndStatusNot(
      LocalDateTime startInclusive, LocalDateTime endExclusive, BookingStatus status);

  long countByStatus(BookingStatus status);

  long countByStatusAndStartAtGreaterThanEqualAndStartAtLessThan(
      BookingStatus status, LocalDateTime startInclusive, LocalDateTime endExclusive);

  @Query("SELECT b FROM Booking b WHERE b.startAt >= :from AND b.startAt < :to")
  List<Booking> findByStartAtInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

  @Query("SELECT b FROM Booking b WHERE b.startAt >= :from AND b.startAt < :to AND b.status <> :cancelled")
  List<Booking> findByStartAtInRangeExcludingCancelled(
      @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("cancelled") BookingStatus cancelled);

  @Query(
      "SELECT DISTINCT b FROM Booking b JOIN FETCH b.serviceItem JOIN FETCH b.merchant WHERE b.startAt >= :from AND b.startAt < :to AND b.status <> :cancelled")
  List<Booking> findByStartAtInRangeWithDetailsExcludingCancelled(
      @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("cancelled") BookingStatus cancelled);

  @Query(
      "SELECT DISTINCT b FROM Booking b JOIN FETCH b.serviceItem JOIN FETCH b.merchant WHERE b.status <> :cancelled ORDER BY b.id DESC")
  List<Booking> findRecentWithDetailsExcludingCancelled(Pageable pageable, @Param("cancelled") BookingStatus cancelled);

  @EntityGraph(attributePaths = {"serviceItem", "merchant"})
  @Override
  Page<Booking> findAll(Specification<Booking> spec, Pageable pageable);
}
