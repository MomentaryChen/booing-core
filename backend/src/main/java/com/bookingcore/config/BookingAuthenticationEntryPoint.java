package com.bookingcore.config;

import com.bookingcore.api.ApiDtos;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

public class BookingAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  public BookingAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
      throws IOException {
    String ctx = request.getContextPath() == null ? "" : request.getContextPath();
    String uri = request.getRequestURI();
    String message =
        uri.startsWith(ctx + "/api/system/") ? "System admin token required" : "Authentication required";
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getOutputStream(), new ApiDtos.ApiEnvelope<>(-1, message, java.util.Map.of("errorCode", "UNAUTHORIZED")));
  }
}
