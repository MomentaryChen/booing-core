package com.bookingcore.service;

import com.bookingcore.modules.admin.AuditLog;
import com.bookingcore.modules.admin.AuditLogRepository;
import com.bookingcore.security.PlatformPrincipal;
import java.time.LocalDateTime;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class PlatformAuditService {

  private final AuditLogRepository auditLogRepository;

  public PlatformAuditService(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  /** Uses the current security context actor when present; otherwise {@code fallbackActor}. */
  public void record(String action, String targetType, Long targetId, String detail, String fallbackActor) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String actor = resolveActor(auth, fallbackActor);
    AuditLog log = new AuditLog();
    log.setActor(actor);
    log.setAction(action);
    log.setTargetType(targetType);
    log.setTargetId(targetId);
    log.setDetail(detail);
    log.setCreatedAt(LocalDateTime.now());
    auditLogRepository.save(log);
  }

  public void recordForCurrentUser(String action, String targetType, Long targetId, String detail) {
    record(action, targetType, targetId, detail, "system");
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
}
