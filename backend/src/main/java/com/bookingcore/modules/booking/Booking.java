package com.bookingcore.modules.booking;

import java.util.UUID;
import com.bookingcore.common.BaseEntity;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.service.ServiceItem;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "merchant_id", nullable = false, columnDefinition = "UUID")
  @JsonIgnore
  private Merchant merchant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "service_item_id", nullable = false, columnDefinition = "UUID")
  private ServiceItem serviceItem;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "platform_user_id", columnDefinition = "UUID")
  @JsonIgnore
  private PlatformUser platformUser;

  @Column(nullable = false)
  private LocalDateTime startAt;

  @Column(nullable = false)
  private LocalDateTime endAt;

  @Column(nullable = false, length = 120)
  private String customerName;

  @Column(nullable = false, length = 120)
  private String customerContact;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private BookingStatus status = BookingStatus.PENDING;

  public Merchant getMerchant() {
    return merchant;
  }

  public void setMerchant(Merchant merchant) {
    this.merchant = merchant;
  }

  public ServiceItem getServiceItem() {
    return serviceItem;
  }

  public void setServiceItem(ServiceItem serviceItem) {
    this.serviceItem = serviceItem;
  }

  public PlatformUser getPlatformUser() {
    return platformUser;
  }

  public void setPlatformUser(PlatformUser platformUser) {
    this.platformUser = platformUser;
  }

  public LocalDateTime getStartAt() {
    return startAt;
  }

  public void setStartAt(LocalDateTime startAt) {
    this.startAt = startAt;
  }

  public LocalDateTime getEndAt() {
    return endAt;
  }

  public void setEndAt(LocalDateTime endAt) {
    this.endAt = endAt;
  }

  public String getCustomerName() {
    return customerName;
  }

  public void setCustomerName(String customerName) {
    this.customerName = customerName;
  }

  public String getCustomerContact() {
    return customerContact;
  }

  public void setCustomerContact(String customerContact) {
    this.customerContact = customerContact;
  }

  public BookingStatus getStatus() {
    return status;
  }

  public void setStatus(BookingStatus status) {
    this.status = status;
  }
}
