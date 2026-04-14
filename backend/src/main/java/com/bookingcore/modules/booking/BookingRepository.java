package com.bookingcore.modules.booking;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {
  java.util.Optional<Booking> findByIdAndMerchantId(Long id, Long merchantId);
  List<Booking> findByMerchantIdOrderByStartAtAsc(Long merchantId);
  List<Booking> findByMerchantIdAndStatusOrderByStartAtAsc(Long merchantId, BookingStatus status);

  List<Booking> findByMerchantIdAndStatusNotAndStartAtLessThanAndEndAtGreaterThan(
      Long merchantId, BookingStatus status, LocalDateTime endAt, LocalDateTime startAt);

  List<Booking>
      findByMerchantIdAndServiceItemIdInAndStatusNotAndStartAtLessThanAndEndAtGreaterThan(
          Long merchantId,
          List<Long> serviceItemIds,
          BookingStatus status,
          LocalDateTime endAt,
          LocalDateTime startAt);

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
