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
@Table(name = "merchant_memberships")
public class MerchantMembership {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "merchant_id", nullable = false)
  private Merchant merchant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "platform_user_id", nullable = false)
  private PlatformUser platformUser;

  @Enumerated(EnumType.STRING)
  @Column(name = "membership_status", nullable = false, length = 32)
  private MerchantMembershipStatus membershipStatus = MerchantMembershipStatus.ACTIVE;

  @Column(name = "joined_at", nullable = false)
  private LocalDateTime joinedAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    if (joinedAt == null) {
      joinedAt = now;
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

  public PlatformUser getPlatformUser() {
    return platformUser;
  }

  public void setPlatformUser(PlatformUser platformUser) {
    this.platformUser = platformUser;
  }

  public MerchantMembershipStatus getMembershipStatus() {
    return membershipStatus;
  }

  public void setMembershipStatus(MerchantMembershipStatus membershipStatus) {
    this.membershipStatus = membershipStatus;
  }
}
