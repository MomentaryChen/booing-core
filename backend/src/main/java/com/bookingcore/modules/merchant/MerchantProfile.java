package com.bookingcore.modules.merchant;

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
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "merchant_profiles")
public class MerchantProfile {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "merchant_id", nullable = false, unique = true)
  @JsonIgnore
  private Merchant merchant;

  @Column(length = 500)
  private String description;

  @Lob
  @Column(name = "logo_data", columnDefinition = "LONGTEXT")
  private String logoUrl;

  @Column(length = 500)
  private String address;

  @Column(length = 120)
  private String phone;

  @Column(length = 120)
  private String email;

  @Column(length = 500)
  private String website;

  @Column(length = 120)
  private String storeCategory;

  @Column(length = 500)
  private String lineContactUrl;

  public Long getId() {
    return id;
  }

  public Merchant getMerchant() {
    return merchant;
  }

  public void setMerchant(Merchant merchant) {
    this.merchant = merchant;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getLogoUrl() {
    return logoUrl;
  }

  public void setLogoUrl(String logoUrl) {
    this.logoUrl = logoUrl;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getWebsite() {
    return website;
  }

  public void setWebsite(String website) {
    this.website = website;
  }

  public String getStoreCategory() {
    return storeCategory;
  }

  public void setStoreCategory(String storeCategory) {
    this.storeCategory = storeCategory;
  }

  public String getLineContactUrl() {
    return lineContactUrl;
  }

  public void setLineContactUrl(String lineContactUrl) {
    this.lineContactUrl = lineContactUrl;
  }
}
