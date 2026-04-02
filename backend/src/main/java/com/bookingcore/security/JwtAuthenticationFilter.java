package com.bookingcore.security;

import com.bookingcore.config.BookingPlatformProperties;
import com.bookingcore.config.JwtEnabledCondition;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Conditional(JwtEnabledCondition.class)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final BookingPlatformProperties properties;
  private final JwtService jwtService;

  public JwtAuthenticationFilter(BookingPlatformProperties properties, JwtService jwtService) {
    this.properties = properties;
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      filterChain.doFilter(request, response);
      return;
    }
    try {
      Authentication auth;
      try {
        auth = resolveAuthentication(request);
      } catch (JwtException | IllegalArgumentException ex) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"Invalid or expired token\"}");
        return;
      }
      if (auth != null) {
        if (auth instanceof UsernamePasswordAuthenticationToken token) {
          token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        }
        SecurityContextHolder.getContext().setAuthentication(auth);
      }
      filterChain.doFilter(request, response);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private Authentication resolveAuthentication(HttpServletRequest request) {
    String configured = properties.getSystemAdminToken();
    if (StringUtils.hasText(configured) && isSystemPath(request)) {
      String presented = readSystemAdminCredential(request);
      if (configured.equals(presented)) {
        return new UsernamePasswordAuthenticationToken(
            "system-admin",
            null,
            List.of(new SimpleGrantedAuthority(PlatformUserRole.SYSTEM_ADMIN.authority())));
      }
    }

    String bearer = bearerToken(request);
    if (!StringUtils.hasText(bearer)) {
      return null;
    }
    return jwtService.parseToken(bearer);
  }

  private static boolean isSystemPath(HttpServletRequest request) {
    String context = request.getContextPath() == null ? "" : request.getContextPath();
    return request.getRequestURI().startsWith(context + "/api/system/");
  }

  private static String readSystemAdminCredential(HttpServletRequest request) {
    String presented = request.getHeader("X-System-Admin-Token");
    if (StringUtils.hasText(presented)) {
      return presented.trim();
    }
    String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (StringUtils.hasText(auth) && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
      return auth.substring(7).trim();
    }
    return null;
  }

  private static String bearerToken(HttpServletRequest request) {
    String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (StringUtils.hasText(auth) && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
      return auth.substring(7).trim();
    }
    return null;
  }
}
