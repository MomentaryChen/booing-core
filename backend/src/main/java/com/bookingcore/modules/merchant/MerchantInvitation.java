package com.bookingcore.modules.merchant;

import com.bookingcore.modules.platform.PlatformUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_invitations")
public class MerchantInvitation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "merchant_id", nullable = false)
  private Merchant merchant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invitee_user_id", nullable = false)
  private PlatformUser inviteeUser;

  @Column(name = "invite_code", nullable = false, unique = true, length = 80)
  private String inviteCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private MerchantInvitationStatus status = MerchantInvitationStatus.PENDING;

  @Column(name = "expires_at")
  private LocalDateTime expiresAt;

  @Column(name = "created_by", nullable = false, length = 120)
  private String createdBy;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public Long getId() {
    return id;
  }

  public Merchant getMerchant() {
    return merchant;
  }

  public void setMerchant(Merchant merchant) {
    this.merchant = merchant;
  }

  public PlatformUser getInviteeUser() {
    return inviteeUser;
  }

  public void setInviteeUser(PlatformUser inviteeUser) {
    this.inviteeUser = inviteeUser;
  }

  public String getInviteCode() {
    return inviteCode;
  }

  public void setInviteCode(String inviteCode) {
    this.inviteCode = inviteCode;
  }

  public MerchantInvitationStatus getStatus() {
    return status;
  }

  public void setStatus(MerchantInvitationStatus status) {
    this.status = status;
  }

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(LocalDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }
}
