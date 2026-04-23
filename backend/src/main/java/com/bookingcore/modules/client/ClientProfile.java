package com.bookingcore.modules.client;

import com.bookingcore.modules.platform.PlatformUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "client_profiles")
public class ClientProfile {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "platform_user_id", nullable = false, unique = true)
  private PlatformUser platformUser;

  @Column(name = "display_name", length = 120)
  private String displayName;

  @Column(name = "contact_phone", length = 120)
  private String contactPhone;

  @Column(name = "language", length = 16)
  private String language;

  @Column(name = "timezone", length = 64)
  private String timezone;

  @Column(name = "currency", length = 16)
  private String currency;

  @Column(name = "location", length = 160)
  private String location;

  @Column(name = "bio", length = 1000)
  private String bio;

  @Column(name = "theme", length = 16)
  private String theme;

  @Column(name = "email_notifications")
  private Boolean emailNotifications;

  @Column(name = "sms_notifications")
  private Boolean smsNotifications;

  @Column(name = "push_notifications")
  private Boolean pushNotifications;

  @Column(name = "marketing_emails")
  private Boolean marketingEmails;

  @Column(name = "security_alerts")
  private Boolean securityAlerts;

  @Column(name = "product_updates")
  private Boolean productUpdates;

  @Column(name = "two_factor_enabled")
  private Boolean twoFactorEnabled;

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
    updatedAt = now;
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public Long getId() {
    return id;
  }

  public PlatformUser getPlatformUser() {
    return platformUser;
  }

  public void setPlatformUser(PlatformUser platformUser) {
    this.platformUser = platformUser;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getContactPhone() {
    return contactPhone;
  }

  public void setContactPhone(String contactPhone) {
    this.contactPhone = contactPhone;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public Boolean getEmailNotifications() {
    return emailNotifications;
  }

  public void setEmailNotifications(Boolean emailNotifications) {
    this.emailNotifications = emailNotifications;
  }

  public Boolean getSmsNotifications() {
    return smsNotifications;
  }

  public void setSmsNotifications(Boolean smsNotifications) {
    this.smsNotifications = smsNotifications;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getBio() {
    return bio;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  public String getTheme() {
    return theme;
  }

  public void setTheme(String theme) {
    this.theme = theme;
  }

  public Boolean getPushNotifications() {
    return pushNotifications;
  }

  public void setPushNotifications(Boolean pushNotifications) {
    this.pushNotifications = pushNotifications;
  }

  public Boolean getMarketingEmails() {
    return marketingEmails;
  }

  public void setMarketingEmails(Boolean marketingEmails) {
    this.marketingEmails = marketingEmails;
  }

  public Boolean getSecurityAlerts() {
    return securityAlerts;
  }

  public void setSecurityAlerts(Boolean securityAlerts) {
    this.securityAlerts = securityAlerts;
  }

  public Boolean getProductUpdates() {
    return productUpdates;
  }

  public void setProductUpdates(Boolean productUpdates) {
    this.productUpdates = productUpdates;
  }

  public Boolean getTwoFactorEnabled() {
    return twoFactorEnabled;
  }

  public void setTwoFactorEnabled(Boolean twoFactorEnabled) {
    this.twoFactorEnabled = twoFactorEnabled;
  }
}
