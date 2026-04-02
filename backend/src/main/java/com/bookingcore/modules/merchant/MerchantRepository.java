package com.bookingcore.modules.merchant;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {
  Optional<Merchant> findBySlug(String slug);

  Optional<Merchant> findFirstByOrderByIdAsc();
}
