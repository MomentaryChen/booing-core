package com.bookingcore.service;

import com.bookingcore.common.ApiException;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.MerchantInvitation;
import com.bookingcore.modules.merchant.MerchantInvitationRepository;
import com.bookingcore.modules.merchant.MerchantInvitationStatus;
import com.bookingcore.modules.merchant.MerchantMembership;
import com.bookingcore.modules.merchant.MerchantMembershipRepository;
import com.bookingcore.modules.merchant.MerchantMembershipStatus;
import com.bookingcore.modules.merchant.MerchantVisibility;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.platform.PlatformUserRepository;
import com.bookingcore.security.PlatformPrincipal;
import com.bookingcore.security.PlatformUserRole;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MerchantAccessService {
  private final PlatformUserRepository platformUserRepository;
  private final MerchantMembershipRepository merchantMembershipRepository;
  private final MerchantInvitationRepository merchantInvitationRepository;

  public MerchantAccessService(
      PlatformUserRepository platformUserRepository,
      MerchantMembershipRepository merchantMembershipRepository,
      MerchantInvitationRepository merchantInvitationRepository) {
    this.platformUserRepository = platformUserRepository;
    this.merchantMembershipRepository = merchantMembershipRepository;
    this.merchantInvitationRepository = merchantInvitationRepository;
  }

  public void assertClientCanAccessMerchant(Merchant merchant) {
    if (merchant.getVisibility() == MerchantVisibility.PUBLIC) {
      return;
    }
    PlatformUser user = currentPlatformUserOrNull();
    if (user == null) {
      throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
    }
    boolean allowed =
        merchantMembershipRepository
            .findByMerchantIdAndPlatformUserId(merchant.getId(), user.getId())
            .map(m -> m.getMembershipStatus() == MerchantMembershipStatus.ACTIVE)
            .orElse(false);
    if (!allowed) {
      throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
    }
  }

  public String currentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return "system";
    }
    Object principal = auth.getPrincipal();
    if (principal instanceof PlatformPrincipal p) {
      return p.username();
    }
    return String.valueOf(principal);
  }

  public PlatformUser currentPlatformUserOrNull() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return null;
    }
    Object principal = auth.getPrincipal();
    if (!(principal instanceof PlatformPrincipal p)) {
      return null;
    }
    return platformUserRepository.findByUsername(p.username()).orElse(null);
  }

  public boolean isSystemAdmin() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return false;
    }
    return auth.getAuthorities().stream().anyMatch(a -> "ROLE_SYSTEM_ADMIN".equals(a.getAuthority()));
  }

  public boolean isMerchantScopedTo(UUID merchantId) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return false;
    }
    Object principal = auth.getPrincipal();
    if (!(principal instanceof PlatformPrincipal p)) {
      return false;
    }
    return (p.role() == PlatformUserRole.MERCHANT || p.role() == PlatformUserRole.SUB_MERCHANT)
        && merchantId.equals(p.merchantId());
  }

  public boolean hasActiveMembershipForCurrentUser(UUID merchantId) {
    PlatformUser user = currentPlatformUserOrNull();
    if (user == null || merchantId == null) {
      return false;
    }
    var membership =
        merchantMembershipRepository.findByMerchantIdAndPlatformUserId(merchantId, user.getId());
    return membership.isPresent()
        && membership.get().getMembershipStatus() == MerchantMembershipStatus.ACTIVE;
  }

  public boolean hasActiveMembership(UUID merchantId, UUID platformUserId) {
    if (merchantId == null || platformUserId == null) {
      return false;
    }
    var membership =
        merchantMembershipRepository.findByMerchantIdAndPlatformUserId(merchantId, platformUserId);
    if (membership.isPresent()) {
      return membership.get().getMembershipStatus() == MerchantMembershipStatus.ACTIVE;
    }
    return platformUserRepository
        .findById(platformUserId)
        .map(user -> user.getMerchant() != null && merchantId.equals(user.getMerchant().getId()))
        .orElse(false);
  }

  public boolean hasStrictActiveMembership(UUID merchantId, UUID platformUserId) {
    if (merchantId == null || platformUserId == null) {
      return false;
    }
    return merchantMembershipRepository
        .findByMerchantIdAndPlatformUserId(merchantId, platformUserId)
        .map(membership -> membership.getMembershipStatus() == MerchantMembershipStatus.ACTIVE)
        .orElse(false);
  }

  @Transactional
  public MerchantMembership acceptInvitationByCode(String inviteCode) {
    if (!StringUtils.hasText(inviteCode)) {
      throw new ApiException("Invite code is required", HttpStatus.BAD_REQUEST);
    }
    PlatformUser user = currentPlatformUserOrNull();
    if (user == null) {
      throw new ApiException("Unauthorized", HttpStatus.UNAUTHORIZED);
    }
    MerchantInvitation invitation =
        merchantInvitationRepository
            .findByInviteCode(inviteCode.trim())
            .orElseThrow(() -> new ApiException("Invite code not found", HttpStatus.NOT_FOUND));
    if (invitation.getStatus() != MerchantInvitationStatus.PENDING) {
      throw new ApiException("Invite code is no longer active", HttpStatus.CONFLICT);
    }
    if (invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
      invitation.setStatus(MerchantInvitationStatus.EXPIRED);
      merchantInvitationRepository.save(invitation);
      throw new ApiException("Invite code expired", HttpStatus.GONE);
    }
    if (!invitation.getInviteeUser().getId().equals(user.getId())) {
      throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
    }

    MerchantMembership membership =
        merchantMembershipRepository
            .findByMerchantIdAndPlatformUserId(invitation.getMerchant().getId(), user.getId())
            .orElseGet(
                () -> {
                  MerchantMembership created = new MerchantMembership();
                  created.setMerchant(invitation.getMerchant());
                  created.setPlatformUser(user);
                  return created;
                });
    membership.setMembershipStatus(MerchantMembershipStatus.ACTIVE);
    invitation.setStatus(MerchantInvitationStatus.ACCEPTED);
    merchantInvitationRepository.save(invitation);
    return merchantMembershipRepository.save(membership);
  }

  public String generateInviteCode() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
  }
}
