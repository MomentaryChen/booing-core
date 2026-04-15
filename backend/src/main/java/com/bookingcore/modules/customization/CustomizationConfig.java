package com.bookingcore.modules.customization;

import com.bookingcore.modules.merchant.Merchant;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "customization_configs")
public class CustomizationConfig {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "merchant_id", nullable = false, unique = true)
  @JsonIgnore
  private Merchant merchant;

  @Column(length = 40)
  private String themeColor = "#2563eb";

  @Column(length = 40, nullable = false)
  private String themePreset = "minimal";

  @Column(length = 80)
  private String heroTitle = "Welcome";

  @Column(length = 300)
  private String bookingFlowText = "Please choose service and timeslot.";

  @Column(length = 80)
  private String inviteCode = "";

  @Column(length = 2000)
  private String termsText = "";

  @Column(length = 2000)
  private String announcementText = "";

  @Lob
  @Column(nullable = false)
  private String faqJson = "[]";

  @Column(nullable = false)
  private Integer bufferMinutes = 0;

  @Lob
  @Column(nullable = false)
  private String homepageSectionsJson = "[\"hero\",\"services\",\"booking\"]";

  @Lob
  @Column(nullable = false)
  private String categoryOrderJson = "[]";

  @Column(name = "notification_new_booking", nullable = false)
  private Boolean notificationNewBooking = true;

  @Column(name = "notification_cancellation", nullable = false)
  private Boolean notificationCancellation = true;

  @Column(name = "notification_daily_summary", nullable = false)
  private Boolean notificationDailySummary = false;

  public Long getId() {
    return id;
  }

  public Merchant getMerchant() {
    return merchant;
  }

  public void setMerchant(Merchant merchant) {
    this.merchant = merchant;
  }

  public String getThemeColor() {
    return themeColor;
  }

  public void setThemeColor(String themeColor) {
    this.themeColor = themeColor;
  }

  public String getThemePreset() {
    return themePreset;
  }

  public void setThemePreset(String themePreset) {
    this.themePreset = themePreset;
  }

  public String getHeroTitle() {
    return heroTitle;
  }

  public void setHeroTitle(String heroTitle) {
    this.heroTitle = heroTitle;
  }

  public String getBookingFlowText() {
    return bookingFlowText;
  }

  public void setBookingFlowText(String bookingFlowText) {
    this.bookingFlowText = bookingFlowText;
  }

  public String getTermsText() {
    return termsText;
  }

  public String getInviteCode() {
    return inviteCode;
  }

  public void setInviteCode(String inviteCode) {
    this.inviteCode = inviteCode;
  }

  public void setTermsText(String termsText) {
    this.termsText = termsText;
  }

  public String getAnnouncementText() {
    return announcementText;
  }

  public void setAnnouncementText(String announcementText) {
    this.announcementText = announcementText;
  }

  public String getFaqJson() {
    return faqJson;
  }

  public void setFaqJson(String faqJson) {
    this.faqJson = faqJson;
  }

  public Integer getBufferMinutes() {
    return bufferMinutes;
  }

  public void setBufferMinutes(Integer bufferMinutes) {
    this.bufferMinutes = bufferMinutes;
  }

  public String getHomepageSectionsJson() {
    return homepageSectionsJson;
  }

  public void setHomepageSectionsJson(String homepageSectionsJson) {
    this.homepageSectionsJson = homepageSectionsJson;
  }

  public String getCategoryOrderJson() {
    return categoryOrderJson;
  }

  public void setCategoryOrderJson(String categoryOrderJson) {
    this.categoryOrderJson = categoryOrderJson;
  }

  public Boolean getNotificationNewBooking() {
    return notificationNewBooking;
  }

  public void setNotificationNewBooking(Boolean notificationNewBooking) {
    this.notificationNewBooking = notificationNewBooking;
  }

  public Boolean getNotificationCancellation() {
    return notificationCancellation;
  }

  public void setNotificationCancellation(Boolean notificationCancellation) {
    this.notificationCancellation = notificationCancellation;
  }

  public Boolean getNotificationDailySummary() {
    return notificationDailySummary;
  }

  public void setNotificationDailySummary(Boolean notificationDailySummary) {
    this.notificationDailySummary = notificationDailySummary;
  }
}
