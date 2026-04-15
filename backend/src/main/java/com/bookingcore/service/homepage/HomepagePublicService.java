package com.bookingcore.service.homepage;

import com.bookingcore.api.ApiDtos.HomepageConfigResponse;
import com.bookingcore.api.ApiDtos.HomepageConfigSection;
import com.bookingcore.api.ApiDtos.HomepageFallbackPolicy;
import com.bookingcore.api.ApiDtos.HomepageSeoResponse;
import com.bookingcore.common.ApiException;
import com.bookingcore.service.homepage.HomepageTenantRegistry.HomepageSeoEntry;
import com.bookingcore.service.homepage.HomepageTenantRegistry.HomepageTenantPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class HomepagePublicService {

  private static final Set<String> SUPPORTED_LOCALES = Set.of("zh-TW", "en-US");

  private final HomepageTenantRegistry tenantRegistry;
  private final HomepageGuardrailService guardrailService;

  public HomepagePublicService(
      HomepageTenantRegistry tenantRegistry, HomepageGuardrailService guardrailService) {
    this.tenantRegistry = tenantRegistry;
    this.guardrailService = guardrailService;
  }

  public HomepageConfigResponse buildConfig(String tenantId, String locale, String pageVariant) {
    requireSupportedLocale(locale);
    HomepageTenantPayload payload = tenantRegistry.requireTenant(tenantId);
    if (payload == null) {
      throw new ApiException("Unknown tenant", HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND");
    }
    List<HomepageConfigSection> sections = new ArrayList<>();
    for (var row : tenantRegistry.sortedSections(payload)) {
      sections.add(new HomepageConfigSection(row.id(), row.enabled(), row.order(), row.contentKey()));
    }
    return new HomepageConfigResponse(
        tenantId,
        locale,
        pageVariant == null || pageVariant.isBlank() ? "default" : pageVariant,
        sections,
        new HomepageFallbackPolicy(true, false),
        stateLegalityNotice(locale));
  }

  public HomepageSeoResponse buildSeo(String tenantId, String locale, String variant) {
    requireSupportedLocale(locale);
    HomepageTenantPayload payload = tenantRegistry.requireTenant(tenantId);
    if (payload == null) {
      throw new ApiException("Unknown tenant", HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND");
    }
    HomepageSeoEntry entry = resolveSeo(payload, locale);
    guardrailService.assertCopyAllowed(List.of(entry.title(), entry.description()));
    String v = variant == null || variant.isBlank() ? "default" : variant;
    return new HomepageSeoResponse(
        tenantId,
        locale,
        v,
        entry.title(),
        entry.description(),
        entry.canonicalUrl(),
        entry.robots(),
        entry.structuredData());
  }

  private HomepageSeoEntry resolveSeo(HomepageTenantPayload payload, String locale) {
    HomepageSeoEntry direct = payload.seo().get(locale);
    if (direct != null) {
      return direct;
    }
    HomepageSeoEntry fallback = payload.seo().get(payload.primaryLocale());
    if (fallback != null) {
      return fallback;
    }
    throw new ApiException("SEO not configured for tenant", HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
  }

  private void requireSupportedLocale(String locale) {
    if (locale == null || !SUPPORTED_LOCALES.contains(locale)) {
      throw new ApiException("Unsupported locale", HttpStatus.BAD_REQUEST, "LOCALE_NOT_SUPPORTED");
    }
  }

  private String stateLegalityNotice(String locale) {
    if ("zh-TW".equals(locale)) {
      return "實際狀態依租戶規則與資源策略。";
    }
    return "Actual booking states follow tenant rules and resource policies.";
  }
}
