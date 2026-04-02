package com.bookingcore.api;

import com.bookingcore.api.ApiDtos.DomainTemplateRequest;
import com.bookingcore.api.ApiDtos.MerchantLimitRequest;
import com.bookingcore.api.ApiDtos.MerchantStatusRequest;
import com.bookingcore.api.ApiDtos.SystemSettingsRequest;
import com.bookingcore.config.BookingPlatformProperties;
import com.bookingcore.modules.admin.AuditLog;
import com.bookingcore.modules.admin.AuditLogRepository;
import com.bookingcore.modules.admin.DomainTemplate;
import com.bookingcore.modules.admin.DomainTemplateRepository;
import com.bookingcore.modules.admin.SystemSettings;
import com.bookingcore.modules.admin.SystemSettingsRepository;
import com.bookingcore.modules.booking.Booking;
import com.bookingcore.modules.booking.BookingRepository;
import com.bookingcore.modules.booking.BookingStatus;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.MerchantRepository;
import com.bookingcore.service.BookingCommandService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemAdminController {
  private final MerchantRepository merchantRepository;
  private final BookingRepository bookingRepository;
  private final DomainTemplateRepository domainTemplateRepository;
  private final SystemSettingsRepository systemSettingsRepository;
  private final AuditLogRepository auditLogRepository;
  private final BookingPlatformProperties platformProperties;
  private final BookingCommandService bookingCommandService;

  public SystemAdminController(
      MerchantRepository merchantRepository,
      BookingRepository bookingRepository,
      DomainTemplateRepository domainTemplateRepository,
      SystemSettingsRepository systemSettingsRepository,
      AuditLogRepository auditLogRepository,
      BookingPlatformProperties platformProperties,
      BookingCommandService bookingCommandService) {
    this.merchantRepository = merchantRepository;
    this.bookingRepository = bookingRepository;
    this.domainTemplateRepository = domainTemplateRepository;
    this.systemSettingsRepository = systemSettingsRepository;
    this.auditLogRepository = auditLogRepository;
    this.platformProperties = platformProperties;
    this.bookingCommandService = bookingCommandService;
  }

  @GetMapping("/overview")
  public Map<String, Object> adminOverview() {
    long activeMerchants = merchantRepository.findAll().stream().filter(Merchant::getActive).count();
    return Map.of(
        "totalMerchants", merchantRepository.count(),
        "activeMerchants", activeMerchants,
        "totalBookings", bookingRepository.count(),
        "domainTemplates", domainTemplateRepository.count());
  }

  @GetMapping("/merchants")
  public List<Merchant> adminMerchants() {
    return merchantRepository.findAll();
  }

  @PutMapping("/merchants/{merchantId}/status")
  public Merchant adminUpdateMerchantStatus(@PathVariable Long merchantId, @RequestBody MerchantStatusRequest request) {
    Merchant merchant = bookingCommandService.ensureMerchant(merchantId);
    merchant.setActive(request.active());
    Merchant saved = merchantRepository.save(merchant);
    createAuditLog("merchant.status.updated", "merchant", merchantId, "active=" + request.active());
    return saved;
  }

  @PutMapping("/merchants/{merchantId}/service-limit")
  public Merchant adminUpdateServiceLimit(@PathVariable Long merchantId, @RequestBody MerchantLimitRequest request) {
    Merchant merchant = bookingCommandService.ensureMerchant(merchantId);
    merchant.setServiceLimit(request.serviceLimit());
    Merchant saved = merchantRepository.save(merchant);
    createAuditLog("merchant.limit.updated", "merchant", merchantId, "serviceLimit=" + request.serviceLimit());
    return saved;
  }

  @GetMapping("/domain-templates")
  public List<DomainTemplate> adminGetDomainTemplates() {
    return domainTemplateRepository.findAll();
  }

  @PostMapping("/domain-templates")
  public DomainTemplate adminCreateDomainTemplate(@Valid @RequestBody DomainTemplateRequest request) {
    DomainTemplate template = new DomainTemplate();
    template.setDomainName(request.domainName());
    template.setFieldsJson(request.fieldsJson());
    DomainTemplate saved = domainTemplateRepository.save(template);
    createAuditLog("domain-template.created", "domain_template", saved.getId(), request.domainName());
    return saved;
  }

  @DeleteMapping("/domain-templates/{templateId}")
  public void adminDeleteDomainTemplate(@PathVariable Long templateId) {
    domainTemplateRepository.deleteById(templateId);
    createAuditLog("domain-template.deleted", "domain_template", templateId, "deleted");
  }

  @GetMapping("/system-settings")
  public SystemSettings adminGetSystemSettings() {
    return systemSettingsRepository.findAll().stream().findFirst().orElseGet(() -> {
      SystemSettings settings = new SystemSettings();
      return systemSettingsRepository.save(settings);
    });
  }

  @PutMapping("/system-settings")
  public SystemSettings adminUpdateSystemSettings(@RequestBody SystemSettingsRequest request) {
    SystemSettings settings = systemSettingsRepository.findAll().stream().findFirst().orElseGet(SystemSettings::new);
    settings.setEmailTemplate(request.emailTemplate());
    settings.setSmsTemplate(request.smsTemplate());
    settings.setMaintenanceAnnouncement(request.maintenanceAnnouncement());
    SystemSettings saved = systemSettingsRepository.save(settings);
    createAuditLog("system-settings.updated", "system_settings", saved.getId(), "templates updated");
    return saved;
  }

  @GetMapping("/audit-logs")
  public List<AuditLog> adminGetAuditLogs() {
    return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
  }

  /**
   * Aggregated metrics, weekly hour heat-map, sparkline, and live feed for the System Command Center UI.
   */
  @GetMapping("/command-center")
  public Map<String, Object> systemCommandCenter() {
    ZoneId zone = resolvePlatformZoneId();
    LocalDate today = LocalDate.now(zone);
    LocalDateTime dayStart = today.atStartOfDay(zone).toLocalDateTime();
    LocalDateTime dayEnd = today.plusDays(1).atStartOfDay(zone).toLocalDateTime();
    LocalDate weekStart = today.with(DayOfWeek.MONDAY);
    LocalDateTime weekStartDt = weekStart.atStartOfDay(zone).toLocalDateTime();
    LocalDateTime weekEndDt = weekStart.plusWeeks(1).atStartOfDay(zone).toLocalDateTime();

    long todayBookingsActive =
        bookingRepository.countByStartAtGreaterThanEqualAndStartAtLessThanAndStatusNot(
            dayStart, dayEnd, BookingStatus.CANCELLED);
    long pendingActionsTotal = bookingRepository.countByStatus(BookingStatus.PENDING);
    long pendingActionsThisWeek =
        bookingRepository.countByStatusAndStartAtGreaterThanEqualAndStartAtLessThan(
            BookingStatus.PENDING, weekStartDt, weekEndDt);

    List<Booking> todayDetail =
        bookingRepository.findByStartAtInRangeWithDetailsExcludingCancelled(dayStart, dayEnd, BookingStatus.CANCELLED);
    BigDecimal revenueToday =
        todayDetail.stream().map(b -> b.getServiceItem().getPrice()).reduce(BigDecimal.ZERO, BigDecimal::add);

    List<Booking> weekDetail =
        bookingRepository.findByStartAtInRangeWithDetailsExcludingCancelled(weekStartDt, weekEndDt, BookingStatus.CANCELLED);
    int bookedMinutesWeek =
        weekDetail.stream().mapToInt(b -> b.getServiceItem().getDurationMinutes()).sum();
    long activeMerchantCount = merchantRepository.findAll().stream().filter(Merchant::getActive).count();
    int assumedAvailableMinutes = (int) Math.max(1L, activeMerchantCount * 7L * 8 * 60);
    double occupancyRate = Math.min(100.0, (bookedMinutesWeek * 100.0) / assumedAvailableMinutes);

    int[][] heat = new int[7][24];
    int heatMax = 0;
    for (Booking b : weekDetail) {
      LocalDate d = b.getStartAt().toLocalDate();
      long dayIndex = ChronoUnit.DAYS.between(weekStart, d);
      if (dayIndex < 0 || dayIndex > 6) {
        continue;
      }
      int h = b.getStartAt().getHour();
      int v = ++heat[(int) dayIndex][h];
      if (v > heatMax) {
        heatMax = v;
      }
    }
    List<List<Integer>> heatMap = new ArrayList<>(7);
    for (int d = 0; d < 7; d++) {
      List<Integer> row = new ArrayList<>(24);
      for (int h = 0; h < 24; h++) {
        row.add(heat[d][h]);
      }
      heatMap.add(row);
    }

    List<Integer> sparklineWeek = new ArrayList<>(7);
    for (int i = 6; i >= 0; i--) {
      LocalDate d = today.minusDays(i);
      LocalDateTime d0 = d.atStartOfDay(zone).toLocalDateTime();
      LocalDateTime d1 = d.plusDays(1).atStartOfDay(zone).toLocalDateTime();
      sparklineWeek.add(
          (int)
              bookingRepository.countByStartAtGreaterThanEqualAndStartAtLessThanAndStatusNot(
                  d0, d1, BookingStatus.CANCELLED));
    }

    List<Booking> recent =
        bookingRepository.findRecentWithDetailsExcludingCancelled(
            PageRequest.of(0, 40), BookingStatus.CANCELLED);
    List<Map<String, Object>> liveFeed = new ArrayList<>();
    DateTimeFormatter iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    for (Booking b : recent) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("bookingId", b.getId());
      row.put("merchantId", b.getMerchant().getId());
      row.put("merchantName", b.getMerchant().getName());
      row.put("merchantSlug", b.getMerchant().getSlug());
      row.put("customerName", b.getCustomerName());
      row.put("startAt", b.getStartAt().format(iso));
      row.put("status", b.getStatus().name());
      row.put("serviceName", b.getServiceItem().getName());
      row.put("amount", b.getServiceItem().getPrice());
      liveFeed.add(row);
    }

    return Map.ofEntries(
        Map.entry("timeZone", zone.getId()),
        Map.entry("todayBookings", todayBookingsActive),
        Map.entry("weekBookings", weekDetail.size()),
        Map.entry("pendingActions", pendingActionsTotal),
        Map.entry("pendingActionsTotal", pendingActionsTotal),
        Map.entry("pendingActionsThisWeek", pendingActionsThisWeek),
        Map.entry("revenueToday", revenueToday),
        Map.entry("occupancyRate", Math.round(occupancyRate * 10.0) / 10.0),
        Map.entry(
            "occupancyNote",
            "Booked minutes / (active merchants × 7d × 8h); heuristic. Dates use zone "
                + zone.getId()
                + "."),
        Map.entry("heatMap", heatMap),
        Map.entry("heatMax", heatMax),
        Map.entry("heatDayLabels", List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")),
        Map.entry("sparklineWeek", sparklineWeek),
        Map.entry("liveFeed", liveFeed),
        Map.entry("weekStart", weekStart.toString()),
        Map.entry("weekEndExclusive", weekEndDt.toLocalDate().toString()));
  }

  private ZoneId resolvePlatformZoneId() {
    try {
      return ZoneId.of(platformProperties.getTimeZone());
    } catch (Exception e) {
      return ZoneId.of("Asia/Taipei");
    }
  }

  private void createAuditLog(String action, String targetType, Long targetId, String detail) {
    AuditLog log = new AuditLog();
    log.setAction(action);
    log.setTargetType(targetType);
    log.setTargetId(targetId);
    log.setDetail(detail);
    auditLogRepository.save(log);
  }
}
