package com.bookingcore.modules.merchant;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantInvitationRepository extends JpaRepository<MerchantInvitation, Long> {
  List<MerchantInvitation> findByMerchantIdOrderByIdDesc(UUID merchantId);

  Optional<MerchantInvitation> findByInviteCode(String inviteCode);

  List<MerchantInvitation> findByInviteeUserIdAndStatusAndExpiresAtAfter(
      UUID inviteeUserId, MerchantInvitationStatus status, LocalDateTime now);
}
