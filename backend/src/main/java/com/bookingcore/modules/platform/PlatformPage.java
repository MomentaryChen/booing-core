package com.bookingcore.modules.platform;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Registered UI route; visibility per role is defined in {@link RolePageGrant}. */
@Entity
@Table(name = "platform_pages")
public class PlatformPage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 80)
  private String routeKey;

  @Column(nullable = false, length = 255)
  private String frontendPath;

  @Column(nullable = false, length = 80)
  private String labelKey;

  @Column(nullable = false)
  private Integer sortOrder = 0;

  @Column(nullable = false)
  private Boolean active = true;

  public Long getId() {
    return id;
  }

  public String getRouteKey() {
    return routeKey;
  }

  public void setRouteKey(String routeKey) {
    this.routeKey = routeKey;
  }

  public String getFrontendPath() {
    return frontendPath;
  }

  public void setFrontendPath(String frontendPath) {
    this.frontendPath = frontendPath;
  }

  public String getLabelKey() {
    return labelKey;
  }

  public void setLabelKey(String labelKey) {
    this.labelKey = labelKey;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }
}
