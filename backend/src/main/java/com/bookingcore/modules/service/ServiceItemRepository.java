package com.bookingcore.modules.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {
  List<ServiceItem> findByMerchantId(Long merchantId);

  Optional<ServiceItem> findByIdAndMerchantId(Long id, Long merchantId);
}
