package com.bookingcore.modules.merchant;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DynamicFieldConfigRepository extends JpaRepository<DynamicFieldConfig, Long> {
  List<DynamicFieldConfig> findByMerchantId(Long merchantId);

  Optional<DynamicFieldConfig> findByIdAndMerchantId(Long id, Long merchantId);
}
