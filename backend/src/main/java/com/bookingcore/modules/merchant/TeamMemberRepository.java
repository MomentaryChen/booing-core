package com.bookingcore.modules.merchant;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
  List<TeamMember> findByMerchantIdAndTeamIdOrderByIdAsc(Long merchantId, Long teamId);

  Optional<TeamMember> findByMerchantIdAndTeamIdAndPlatformUserId(
      Long merchantId, Long teamId, Long platformUserId);
}
