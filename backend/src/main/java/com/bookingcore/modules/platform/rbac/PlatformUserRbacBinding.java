package com.bookingcore.modules.platform.rbac;

import com.bookingcore.modules.merchant.Merchant;
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
import jakarta.persistence.Table;

@Entity
@Table(name = "platform_user_rbac_bindings")
public class PlatformUserRbacBinding {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "platform_user_id", nullable = false)
  private PlatformUser platformUser;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "rbac_role_id", nullable = false)
  private RbacRole rbacRole;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "merchant_id")
  private Merchant merchant;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private PlatformRbacBindingStatus status = PlatformRbacBindingStatus.ACTIVE;

  public Long getId() {
    return id;
  }

  public PlatformUser getPlatformUser() {
    return platformUser;
  }

  public void setPlatformUser(PlatformUser platformUser) {
    this.platformUser = platformUser;
  }

  public RbacRole getRbacRole() {
    return rbacRole;
  }

  public void setRbacRole(RbacRole rbacRole) {
    this.rbacRole = rbacRole;
  }

  public Merchant getMerchant() {
    return merchant;
  }

  public void setMerchant(Merchant merchant) {
    this.merchant = merchant;
  }

  public PlatformRbacBindingStatus getStatus() {
    return status;
  }

  public void setStatus(PlatformRbacBindingStatus status) {
    this.status = status;
  }
}
