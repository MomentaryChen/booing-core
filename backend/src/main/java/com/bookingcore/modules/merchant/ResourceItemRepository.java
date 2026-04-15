package com.bookingcore.modules.merchant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ResourceItemRepository extends JpaRepository<ResourceItem, UUID> {
  List<ResourceItem> findByMerchantId(UUID merchantId);

  Page<ResourceItem> findByMerchantId(UUID merchantId, Pageable pageable);

  Page<ResourceItem> findByMerchantIdAndMaintenance(UUID merchantId, Boolean maintenance, Pageable pageable);

  Optional<ResourceItem> findByIdAndMerchantId(UUID id, UUID merchantId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<ResourceItem> findWithLockById(UUID id);
}
