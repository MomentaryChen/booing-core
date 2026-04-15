package com.bookingcore.modules.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, UUID> {
  List<ServiceItem> findByMerchantId(UUID merchantId);

  List<ServiceItem> findByMerchantIdAndActive(UUID merchantId, Boolean active);

  Optional<ServiceItem> findByIdAndMerchantId(UUID id, UUID merchantId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<ServiceItem> findByMerchantIdAndIdIn(UUID merchantId, List<UUID> ids);
}
