package com.bookingcore.modules.customization;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomizationConfigRepository extends JpaRepository<CustomizationConfig, Long> {
  Optional<CustomizationConfig> findByMerchantId(Long merchantId);
}
