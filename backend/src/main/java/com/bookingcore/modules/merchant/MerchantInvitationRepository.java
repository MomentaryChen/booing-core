package com.bookingcore.modules.merchant;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantInvitationRepository extends JpaRepository<MerchantInvitation, Long> {
  List<MerchantInvitation> findByMerchantIdOrderByIdDesc(Long merchantId);

  Optional<MerchantInvitation> findByInviteCode(String inviteCode);

  List<MerchantInvitation> findByInviteeUserIdAndStatusAndExpiresAtAfter(
      Long inviteeUserId, MerchantInvitationStatus status, LocalDateTime now);
}
