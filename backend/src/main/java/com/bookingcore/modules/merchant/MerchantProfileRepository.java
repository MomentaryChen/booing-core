package com.bookingcore.modules.merchant;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantProfileRepository extends JpaRepository<MerchantProfile, Long> {
  Optional<MerchantProfile> findByMerchantId(UUID merchantId);
}
