package com.bookingcore.service.homepage;

import com.bookingcore.api.ApiDtos.HomepageTrackingAcceptResponse;
import com.bookingcore.api.ApiDtos.HomepageTrackingBatchRequest;
import com.bookingcore.api.ApiDtos.HomepageTrackingEventRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomepageTrackingService {

  private static final Logger log = LoggerFactory.getLogger(HomepageTrackingService.class);
  private static final Set<String> SUPPORTED_LOCALES = Set.of("zh-TW", "en-US");

  private final HomepageTenantRegistry tenantRegistry;

  @Value("${booking.homepage.tracking-test-buffer:false}")
  private boolean trackingTestBuffer;

  private final List<HomepageTrackingEventRequest> testBuffer = new ArrayList<>();

  public HomepageTrackingService(HomepageTenantRegistry tenantRegistry) {
    this.tenantRegistry = tenantRegistry;
  }

  @Transactional
  public HomepageTrackingAcceptResponse accept(HomepageTrackingBatchRequest request) {
    int accepted = 0;
    int rejected = 0;
    for (HomepageTrackingEventRequest event : request.events()) {
      if (!isValid(event)) {
        rejected++;
        continue;
      }
      accepted++;
      log.info(
          "homepage_tracking tenantId={} locale={} sectionId={} campaign={} type={}",
          event.tenantId(),
          event.locale(),
          event.sectionId(),
          event.campaign(),
          event.eventType());
      if (trackingTestBuffer) {
        synchronized (testBuffer) {
          testBuffer.add(event);
        }
      }
    }
    return new HomepageTrackingAcceptResponse(accepted, rejected);
  }

  public List<HomepageTrackingEventRequest> drainTestBuffer() {
    synchronized (testBuffer) {
      List<HomepageTrackingEventRequest> copy = List.copyOf(testBuffer);
      testBuffer.clear();
      return copy;
    }
  }

  private boolean isValid(HomepageTrackingEventRequest e) {
    if (e == null) {
      return false;
    }
    if (isBlank(e.eventType())
        || isBlank(e.tenantId())
        || isBlank(e.locale())
        || isBlank(e.sectionId())
        || isBlank(e.campaign())
        || e.occurredAt() == null) {
      return false;
    }
    if (!SUPPORTED_LOCALES.contains(e.locale())) {
      return false;
    }
    return tenantRegistry.hasTenant(e.tenantId());
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
