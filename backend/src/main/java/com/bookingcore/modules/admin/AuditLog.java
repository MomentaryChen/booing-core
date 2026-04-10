package com.bookingcore.modules.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 80)
  private String actor = "system";

  @Column(nullable = false, length = 80)
  private String action;

  @Column(nullable = false, length = 80)
  private String targetType;

  @Column(nullable = false)
  private Long targetId;

  @Column(nullable = false, length = 80)
  private String correlationId;

  @Lob
  @Column(nullable = false)
  private String detail;

  @Lob
  @Column(name = "before_state")
  private String beforeState;

  @Lob
  @Column(name = "after_state")
  private String afterState;

  @Column(nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  public Long getId() {
    return id;
  }

  public String getActor() {
    return actor;
  }

  public void setActor(String actor) {
    this.actor = actor;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getTargetType() {
    return targetType;
  }

  public void setTargetType(String targetType) {
    this.targetType = targetType;
  }

  public Long getTargetId() {
    return targetId;
  }

  public void setTargetId(Long targetId) {
    this.targetId = targetId;
  }

  public String getDetail() {
    return detail;
  }

  public void setDetail(String detail) {
    this.detail = detail;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  public String getBeforeState() {
    return beforeState;
  }

  public void setBeforeState(String beforeState) {
    this.beforeState = beforeState;
  }

  public String getAfterState() {
    return afterState;
  }

  public void setAfterState(String afterState) {
    this.afterState = afterState;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
