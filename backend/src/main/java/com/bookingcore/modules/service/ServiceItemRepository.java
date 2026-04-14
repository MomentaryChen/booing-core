package com.bookingcore.modules.service;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {
  List<ServiceItem> findByMerchantId(Long merchantId);

  Optional<ServiceItem> findByIdAndMerchantId(Long id, Long merchantId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<ServiceItem> findByMerchantIdAndIdIn(Long merchantId, List<Long> ids);
}
