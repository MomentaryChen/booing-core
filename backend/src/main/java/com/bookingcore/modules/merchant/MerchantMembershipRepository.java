package com.bookingcore.modules.merchant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantMembershipRepository extends JpaRepository<MerchantMembership, Long> {
  List<MerchantMembership> findByPlatformUserIdAndMembershipStatus(UUID platformUserId, MerchantMembershipStatus membershipStatus);

  Optional<MerchantMembership> findByMerchantIdAndPlatformUserId(UUID merchantId, UUID platformUserId);
}
