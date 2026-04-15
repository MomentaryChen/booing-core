package com.bookingcore.modules.merchant;

import java.util.UUID;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
  List<TeamMember> findByMerchantIdAndTeamIdOrderByIdAsc(UUID merchantId, Long teamId);

  Optional<TeamMember> findByMerchantIdAndTeamIdAndPlatformUserId(
      UUID merchantId, Long teamId, UUID platformUserId);

  List<TeamMember> findByMerchantIdAndPlatformUserIdInAndStatus(
      UUID merchantId, List<UUID> platformUserIds, TeamMemberStatus status);
}
