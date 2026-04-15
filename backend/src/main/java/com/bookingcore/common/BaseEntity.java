package com.bookingcore.common;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {
  @Id
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @PrePersist
  protected void assignIdIfMissing() {
    if (id == null) {
      id = UuidUtils.uuidV7();
    }
  }

  public UUID getId() {
    return id;
  }
}
