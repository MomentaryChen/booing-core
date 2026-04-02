package com.bookingcore.modules.booking;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityExceptionRepository extends JpaRepository<AvailabilityException, Long> {
  List<AvailabilityException> findByMerchantId(Long merchantId);

  List<AvailabilityException> findByMerchantIdAndStartAtLessThanAndEndAtGreaterThan(
      Long merchantId, LocalDateTime endAt, LocalDateTime startAt);
}
