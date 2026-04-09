package com.bookingcore.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Public signup persona (decoupled from stored RBAC codes). Only some values are allowed for
 * unauthenticated {@code /api/auth/register}; others are recognized so the API can reject them
 * explicitly.
 */
public enum PublicRegisterType {
  MERCHANT,
  SYSTEM_ADMIN,
  SUB_MERCHANT,
  CLIENT;

  @JsonValue
  public String toJson() {
    return name();
  }

  @JsonCreator
  public static PublicRegisterType fromJson(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return PublicRegisterType.valueOf(value.trim().toUpperCase());
  }
}
