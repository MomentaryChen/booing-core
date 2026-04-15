package com.bookingcore.modules.merchant;

import java.util.UUID;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceTeamRepository extends JpaRepository<ServiceTeam, Long> {
  List<ServiceTeam> findByMerchantIdOrderByIdDesc(UUID merchantId);

  Optional<ServiceTeam> findByMerchantIdAndCode(UUID merchantId, String code);
}
