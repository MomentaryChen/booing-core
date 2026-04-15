package com.bookingcore.modules.merchant;

import com.bookingcore.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "merchants")
public class Merchant extends BaseEntity {

  @Column(nullable = false, unique = true, length = 120)
  private String name;

  @Column(nullable = false, unique = true, length = 120)
  private String slug;

  @Column(nullable = false)
  private Boolean active = true;

  @Column(nullable = false)
  private Integer serviceLimit = 5;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private MerchantVisibility visibility = MerchantVisibility.PUBLIC;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public Integer getServiceLimit() {
    return serviceLimit;
  }

  public void setServiceLimit(Integer serviceLimit) {
    this.serviceLimit = serviceLimit;
  }

  public MerchantVisibility getVisibility() {
    return visibility;
  }

  public void setVisibility(MerchantVisibility visibility) {
    this.visibility = visibility;
  }
}
