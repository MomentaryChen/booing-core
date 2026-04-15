package com.bookingcore.config;

import com.bookingcore.api.ApiDtos;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;

public class BookingAccessDeniedHandler implements AccessDeniedHandler {

  private static final Logger log = LoggerFactory.getLogger(BookingAccessDeniedHandler.class);

  private final ObjectMapper objectMapper;

  public BookingAccessDeniedHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
      throws IOException {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String principal =
        auth != null && auth.isAuthenticated() ? String.valueOf(auth.getPrincipal()) : "anonymous";
    log.warn(
        "access_denied method={} uri={} principal={} message={}",
        request.getMethod(),
        request.getRequestURI(),
        principal,
        accessDeniedException.getMessage());
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getOutputStream(), new ApiDtos.ApiEnvelope<>(-1, "Forbidden", java.util.Map.of("errorCode", "FORBIDDEN")));
  }
}
