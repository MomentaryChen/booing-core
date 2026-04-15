package com.bookingcore.modules.customization;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomizationConfigRepository extends JpaRepository<CustomizationConfig, Long> {
  Optional<CustomizationConfig> findByMerchantId(UUID merchantId);
}
