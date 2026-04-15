package com.bookingcore.service.homepage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class HomepageTenantRegistry implements InitializingBean {

  private final ObjectMapper objectMapper;
  private Map<String, HomepageTenantPayload> tenants = Map.of();

  public HomepageTenantRegistry(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void afterPropertiesSet() throws IOException {
    ClassPathResource resource = new ClassPathResource("homepage/tenants.json");
    try (InputStream in = resource.getInputStream()) {
      HomepageTenantsFile file = objectMapper.readValue(in, HomepageTenantsFile.class);
      this.tenants = Collections.unmodifiableMap(new LinkedHashMap<>(file.tenants()));
    }
  }

  public boolean hasTenant(String tenantId) {
    return tenants.containsKey(tenantId);
  }

  public HomepageTenantPayload requireTenant(String tenantId) {
    HomepageTenantPayload payload = tenants.get(tenantId);
    if (payload == null) {
      return null;
    }
    return payload;
  }

  public List<HomepageSectionDefinition> sortedSections(HomepageTenantPayload payload) {
    return payload.sections().stream()
        .sorted(Comparator.comparingInt(HomepageSectionDefinition::order))
        .collect(Collectors.toList());
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record HomepageTenantsFile(Map<String, HomepageTenantPayload> tenants) {
    public HomepageTenantsFile {
      tenants = tenants == null ? Map.of() : tenants;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record HomepageTenantPayload(
      String primaryLocale, List<HomepageSectionDefinition> sections, Map<String, HomepageSeoEntry> seo) {
    public HomepageTenantPayload {
      primaryLocale = primaryLocale == null ? "en-US" : primaryLocale;
      sections = sections == null ? List.of() : List.copyOf(sections);
      seo = seo == null ? Map.of() : Map.copyOf(seo);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record HomepageSectionDefinition(String id, boolean enabled, int order, String contentKey) {
    public HomepageSectionDefinition {
      id = id == null ? "" : id;
      contentKey = contentKey == null ? "" : contentKey;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record HomepageSeoEntry(
      String title, String description, String canonicalUrl, String robots, Map<String, Object> structuredData) {
    public HomepageSeoEntry {
      title = title == null ? "" : title;
      description = description == null ? "" : description;
      canonicalUrl = canonicalUrl == null ? "" : canonicalUrl;
      robots = robots == null ? "index,follow" : robots;
      structuredData = structuredData == null ? Map.of() : Map.copyOf(structuredData);
    }
  }
}
