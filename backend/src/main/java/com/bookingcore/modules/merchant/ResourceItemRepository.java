package com.bookingcore.modules.merchant;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ResourceItemRepository extends JpaRepository<ResourceItem, Long> {
  List<ResourceItem> findByMerchantId(Long merchantId);

  Optional<ResourceItem> findByIdAndMerchantId(Long id, Long merchantId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<ResourceItem> findWithLockById(Long id);
}
