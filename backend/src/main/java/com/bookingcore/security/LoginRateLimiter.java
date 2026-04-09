package com.bookingcore.security;

import com.bookingcore.common.ApiException;
import com.bookingcore.config.BookingPlatformProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LoginRateLimiter {

  private final BookingPlatformProperties properties;
  private final ConcurrentHashMap<String, ArrayDeque<Long>> attemptsByKey = new ConcurrentHashMap<>();

  public LoginRateLimiter(BookingPlatformProperties properties) {
    this.properties = properties;
  }

  /**
   * Counts this login attempt toward the per-IP sliding window. Call before credential checks so failed
   * attempts also consume budget.
   */
  public void consume(HttpServletRequest request) {
    var login = properties.getAuth().getLogin();
    if (!login.isRateLimitEnabled()) {
      return;
    }
    String ip = resolveClientIp(request, login.isTrustXForwardedFor());
    long windowMs = Math.max(1L, login.getWindowMinutes()) * 60_000L;
    int max = Math.max(1, login.getMaxAttemptsPerIpPerWindow());
    long now = System.currentTimeMillis();
    ArrayDeque<Long> dq =
        attemptsByKey.computeIfAbsent(ip, k -> new ArrayDeque<>());
    synchronized (dq) {
      while (!dq.isEmpty() && now - dq.peekFirst() > windowMs) {
        dq.pollFirst();
      }
      if (dq.size() >= max) {
        throw new ApiException("Too many login attempts", HttpStatus.TOO_MANY_REQUESTS);
      }
      dq.addLast(now);
    }
  }

  private static String resolveClientIp(HttpServletRequest request, boolean trustXForwardedFor) {
    if (trustXForwardedFor) {
      String xff = request.getHeader("X-Forwarded-For");
      if (StringUtils.hasText(xff)) {
        String first = xff.split(",")[0].trim();
        if (StringUtils.hasText(first)) {
          return first;
        }
      }
    }
    String remote = request.getRemoteAddr();
    return StringUtils.hasText(remote) ? remote : "unknown";
  }
}
