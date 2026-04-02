package com.bookingcore.modules.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_settings")
public class SystemSettings {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 2000)
  private String emailTemplate = "";

  @Column(length = 2000)
  private String smsTemplate = "";

  @Column(length = 2000)
  private String maintenanceAnnouncement = "";

  public Long getId() {
    return id;
  }

  public String getEmailTemplate() {
    return emailTemplate;
  }

  public void setEmailTemplate(String emailTemplate) {
    this.emailTemplate = emailTemplate;
  }

  public String getSmsTemplate() {
    return smsTemplate;
  }

  public void setSmsTemplate(String smsTemplate) {
    this.smsTemplate = smsTemplate;
  }

  public String getMaintenanceAnnouncement() {
    return maintenanceAnnouncement;
  }

  public void setMaintenanceAnnouncement(String maintenanceAnnouncement) {
    this.maintenanceAnnouncement = maintenanceAnnouncement;
  }
}
