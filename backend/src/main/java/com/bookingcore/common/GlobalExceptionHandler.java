package com.bookingcore.common;

import com.bookingcore.api.ApiDtos;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiDtos.ApiEnvelope<Object>> handleApiException(ApiException ex) {
    return ResponseEntity
        .status(ex.getStatus())
        .body(new ApiDtos.ApiEnvelope<>(-1, ex.getMessage(), Map.of("errorCode", ex.getCode())));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiDtos.ApiEnvelope<Object>> handleValidation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(err -> err.getField() + " " + err.getDefaultMessage())
        .orElse("Validation failed");
    Map<String, Object> data = new HashMap<>();
    data.put("errorCode", "VALIDATION_FAILED");
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(new ApiDtos.ApiEnvelope<>(-1, message, data));
  }
}
