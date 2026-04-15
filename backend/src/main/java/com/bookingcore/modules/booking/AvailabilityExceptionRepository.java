package com.bookingcore.modules.booking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityExceptionRepository extends JpaRepository<AvailabilityException, Long> {
  List<AvailabilityException> findByMerchantId(UUID merchantId);

  List<AvailabilityException> findByMerchantIdAndStartAtLessThanAndEndAtGreaterThan(
      UUID merchantId, LocalDateTime endAt, LocalDateTime startAt);
}
