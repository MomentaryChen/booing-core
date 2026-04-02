package com.bookingcore.modules.booking;

import java.time.DayOfWeek;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessHoursRepository extends JpaRepository<BusinessHours, Long> {
  List<BusinessHours> findByMerchantId(Long merchantId);

  List<BusinessHours> findByMerchantIdAndDayOfWeek(Long merchantId, DayOfWeek dayOfWeek);

  void deleteByMerchantId(Long merchantId);
}
