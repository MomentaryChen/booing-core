package com.bookingcore.modules.booking;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessHoursRepository extends JpaRepository<BusinessHours, Long> {
  List<BusinessHours> findByMerchantId(UUID merchantId);

  List<BusinessHours> findByMerchantIdAndDayOfWeek(UUID merchantId, DayOfWeek dayOfWeek);

  void deleteByMerchantId(UUID merchantId);
}
