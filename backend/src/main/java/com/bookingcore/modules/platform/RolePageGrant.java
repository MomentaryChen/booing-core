package com.bookingcore.modules.platform;

import com.bookingcore.security.PlatformUserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "role_page_grants",
    uniqueConstraints = @UniqueConstraint(columnNames = {"platform_role", "page_id"}))
public class RolePageGrant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "platform_role", nullable = false, length = 32)
  private PlatformUserRole role;

  @ManyToOne(optional = false)
  @JoinColumn(name = "page_id", nullable = false)
  private PlatformPage page;

  public Long getId() {
    return id;
  }

  public PlatformUserRole getRole() {
    return role;
  }

  public void setRole(PlatformUserRole role) {
    this.role = role;
  }

  public PlatformPage getPage() {
    return page;
  }

  public void setPage(PlatformPage page) {
    this.page = page;
  }
}
