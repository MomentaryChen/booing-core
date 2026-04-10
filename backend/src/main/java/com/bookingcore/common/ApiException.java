package com.bookingcore.common;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
  private final HttpStatus status;
  private final String code;

  public ApiException(String message) {
    this(message, HttpStatus.BAD_REQUEST, "API_BAD_REQUEST");
  }

  public ApiException(String message, HttpStatus status) {
    this(message, status, defaultCode(status));
  }

  public ApiException(String message, HttpStatus status, String code) {
    super(message);
    this.status = status;
    this.code = code == null ? defaultCode(status) : code;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getCode() {
    return code;
  }

  private static String defaultCode(HttpStatus status) {
    if (status == HttpStatus.FORBIDDEN) {
      return "AUTH_FORBIDDEN";
    }
    if (status == HttpStatus.NOT_FOUND) {
      return "RESOURCE_NOT_FOUND";
    }
    if (status == HttpStatus.CONFLICT) {
      return "STATE_CONFLICT";
    }
    return "API_BAD_REQUEST";
  }
}
