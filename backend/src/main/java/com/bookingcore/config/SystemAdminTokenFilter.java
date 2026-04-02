package com.bookingcore.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

/**
 * Optional gate for {@code /api/system/**}. When {@link BookingPlatformProperties#getSystemAdminToken()}
 * is blank, this filter is a no-op (development default).
 */
public class SystemAdminTokenFilter implements Filter {

  private final BookingPlatformProperties properties;

  public SystemAdminTokenFilter(BookingPlatformProperties properties) {
    this.properties = properties;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
      chain.doFilter(request, response);
      return;
    }

    String configured = properties.getSystemAdminToken();
    if (!StringUtils.hasText(configured)) {
      chain.doFilter(request, response);
      return;
    }

    String uri = req.getRequestURI();
    String contextPath = req.getContextPath() == null ? "" : req.getContextPath();
    String prefix = contextPath + "/api/system/";
    if (!uri.startsWith(prefix)) {
      chain.doFilter(request, response);
      return;
    }

    String presented = req.getHeader("X-System-Admin-Token");
    if (!StringUtils.hasText(presented)) {
      String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
      if (StringUtils.hasText(auth) && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
        presented = auth.substring(7).trim();
      }
    }

    if (!configured.equals(presented)) {
      res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      res.setContentType("application/json;charset=UTF-8");
      res.getWriter().write("{\"message\":\"System admin token required\"}");
      return;
    }

    chain.doFilter(request, response);
  }
}
