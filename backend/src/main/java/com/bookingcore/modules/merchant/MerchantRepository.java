package com.bookingcore.modules.merchant;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
  Optional<Merchant> findBySlug(String slug);

  Optional<Merchant> findFirstByOrderByIdAsc();

  List<Merchant> findByActiveTrueOrderByIdAsc();
}
