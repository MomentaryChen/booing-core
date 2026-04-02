package com.bookingcore.modules.merchant;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceItemRepository extends JpaRepository<ResourceItem, Long> {
  List<ResourceItem> findByMerchantId(Long merchantId);

  Optional<ResourceItem> findByIdAndMerchantId(Long id, Long merchantId);
}
