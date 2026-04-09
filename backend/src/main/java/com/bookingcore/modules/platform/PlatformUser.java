package com.bookingcore.modules.platform;

import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.security.PlatformUserRole;
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
@Table(name = "platform_users")
public class PlatformUser {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 120)
  private String username;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "platform_role", nullable = false, length = 32)
  private PlatformUserRole role;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "merchant_id")
  private Merchant merchant;

  @Column(nullable = false)
  private Boolean enabled = true;

  @Column(name = "credential_version", nullable = false)
  private int credentialVersion = 0;

  @Column(name = "failed_login_count", nullable = false)
  private int failedLoginCount = 0;

  @Column(name = "failed_login_window_started_at")
  private LocalDateTime failedLoginWindowStartedAt;

  @Column(name = "locked_until")
  private LocalDateTime lockedUntil;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "last_login_at")
  private LocalDateTime lastLoginAt;

  @PrePersist
  public void prePersist() {
    LocalDateTime n = LocalDateTime.now();
    if (createdAt == null) {
      createdAt = n;
    }
    if (updatedAt == null) {
      updatedAt = n;
    }
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public PlatformUserRole getRole() {
    return role;
  }

  public void setRole(PlatformUserRole role) {
    this.role = role;
  }

  public Merchant getMerchant() {
    return merchant;
  }

  public void setMerchant(Merchant merchant) {
    this.merchant = merchant;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public int getCredentialVersion() {
    return credentialVersion;
  }

  public void setCredentialVersion(int credentialVersion) {
    this.credentialVersion = credentialVersion;
  }

  public int getFailedLoginCount() {
    return failedLoginCount;
  }

  public void setFailedLoginCount(int failedLoginCount) {
    this.failedLoginCount = failedLoginCount;
  }

  public LocalDateTime getFailedLoginWindowStartedAt() {
    return failedLoginWindowStartedAt;
  }

  public void setFailedLoginWindowStartedAt(LocalDateTime failedLoginWindowStartedAt) {
    this.failedLoginWindowStartedAt = failedLoginWindowStartedAt;
  }

  public LocalDateTime getLockedUntil() {
    return lockedUntil;
  }

  public void setLockedUntil(LocalDateTime lockedUntil) {
    this.lockedUntil = lockedUntil;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public LocalDateTime getLastLoginAt() {
    return lastLoginAt;
  }

  public void setLastLoginAt(LocalDateTime lastLoginAt) {
    this.lastLoginAt = lastLoginAt;
  }
}

