package com.bookingcore.modules.platform.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rbac_roles")
public class RbacRole {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 32)
  private String code;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "rbac_role_permissions",
      joinColumns = @JoinColumn(name = "rbac_role_id"),
      inverseJoinColumns = @JoinColumn(name = "rbac_permission_id"))
  private Set<RbacPermission> permissions = new HashSet<>();

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public Set<RbacPermission> getPermissions() {
    return permissions;
  }
}
