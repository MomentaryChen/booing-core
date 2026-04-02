package com.bookingcore.modules.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "domain_templates")
public class DomainTemplate {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 80)
  private String domainName;

  @Lob
  @Column(nullable = false)
  private String fieldsJson = "[]";

  public Long getId() {
    return id;
  }

  public String getDomainName() {
    return domainName;
  }

  public void setDomainName(String domainName) {
    this.domainName = domainName;
  }

  public String getFieldsJson() {
    return fieldsJson;
  }

  public void setFieldsJson(String fieldsJson) {
    this.fieldsJson = fieldsJson;
  }
}
