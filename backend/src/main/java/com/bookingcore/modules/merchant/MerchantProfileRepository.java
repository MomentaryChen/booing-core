package com.bookingcore.modules.merchant;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantProfileRepository extends JpaRepository<MerchantProfile, Long> {
  Optional<MerchantProfile> findByMerchantId(Long merchantId);
}
