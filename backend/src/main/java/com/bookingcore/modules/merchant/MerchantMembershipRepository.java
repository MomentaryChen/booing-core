package com.bookingcore.modules.merchant;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantMembershipRepository extends JpaRepository<MerchantMembership, Long> {
  List<MerchantMembership> findByPlatformUserIdAndMembershipStatus(Long platformUserId, MerchantMembershipStatus membershipStatus);

  Optional<MerchantMembership> findByMerchantIdAndPlatformUserId(Long merchantId, Long platformUserId);
}
