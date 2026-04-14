package com.bookingcore.modules.merchant;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceTeamRepository extends JpaRepository<ServiceTeam, Long> {
  List<ServiceTeam> findByMerchantIdOrderByIdDesc(Long merchantId);

  Optional<ServiceTeam> findByMerchantIdAndCode(Long merchantId, String code);
}
