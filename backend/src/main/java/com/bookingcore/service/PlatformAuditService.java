package com.bookingcore.service;

import com.bookingcore.modules.admin.AuditLog;
import com.bookingcore.modules.admin.AuditLogRepository;
import com.bookingcore.security.PlatformPrincipal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class PlatformAuditService {

  private final AuditLogRepository auditLogRepository;

  public PlatformAuditService(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  /** Uses the current security context actor when present; otherwise {@code fallbackActor}. */
  public void record(String action, String targetType, Long targetId, String detail, String fallbackActor) {
    record(action, targetType, targetId, null, null, detail, fallbackActor);
  }

  public void record(
      String action,
      String targetType,
      Long targetId,
      String beforeState,
      String afterState,
      String detail,
      String fallbackActor) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String actor = resolveActor(auth, fallbackActor);
    AuditLog log = new AuditLog();
    log.setActor(actor);
    log.setAction(action);
    log.setTargetType(targetType);
    log.setTargetId(targetId);
    log.setCorrelationId(resolveCorrelationId());
    log.setDetail(detail);
    log.setBeforeState(beforeState);
    log.setAfterState(afterState);
    log.setCreatedAt(LocalDateTime.now());
    auditLogRepository.save(log);
  }

  public void recordForCurrentUser(String action, String targetType, Long targetId, String detail) {
    record(action, targetType, targetId, detail, "system");
  }

  public void recordForCurrentUser(
      String action,
      String targetType,
      Long targetId,
      String beforeState,
      String afterState,
      String detail) {
    record(action, targetType, targetId, beforeState, afterState, detail, "system");
  }

  private static String resolveActor(Authentication auth, String fallbackActor) {
    if (auth == null || !auth.isAuthenticated()) {
      return fallbackActor;
    }
    Object p = auth.getPrincipal();
    if (p instanceof PlatformPrincipal pp) {
      return pp.username();
    }
    if (p != null) {
      return String.valueOf(p);
    }
    return fallbackActor;
  }

  private static String resolveCorrelationId() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes instanceof ServletRequestAttributes servletAttributes) {
      String fromHeader = servletAttributes.getRequest().getHeader("X-Request-Id");
      if (fromHeader != null && !fromHeader.isBlank()) {
        return fromHeader;
      }
    }
    return UUID.randomUUID().toString();
  }
}
