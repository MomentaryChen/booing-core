package com.bookingcore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookingcore.service.homepage.HomepageTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PublicHomepageApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private HomepageTrackingService homepageTrackingService;

  @Test
  void homepageConfigReturnsSectionsAndNotice() throws Exception {
    mockMvc
        .perform(get("/api/public/homepage-config").param("tenantId", "default").param("locale", "zh-TW"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.tenantId").value("default"))
        .andExpect(jsonPath("$.data.sections.length()").value(8))
        .andExpect(jsonPath("$.data.fallbackPolicy.crossTenantFallback").value(false))
        .andExpect(jsonPath("$.data.stateLegalityNotice").isString());
  }

  @Test
  void homepageConfigUnknownTenant() throws Exception {
    mockMvc
        .perform(get("/api/public/homepage-config").param("tenantId", "unknown").param("locale", "en-US"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.data.errorCode").value("TENANT_NOT_FOUND"));
  }

  @Test
  void homepageConfigUnsupportedLocale() throws Exception {
    mockMvc
        .perform(get("/api/public/homepage-config").param("tenantId", "default").param("locale", "fr-FR"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.data.errorCode").value("LOCALE_NOT_SUPPORTED"));
  }

  @Test
  void homepageSeoDiffersByTenant() throws Exception {
    mockMvc
        .perform(get("/api/public/homepage-seo").param("tenantId", "tnt_a").param("locale", "zh-TW"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("Tenant A — Resource/Slot 預約"));
    mockMvc
        .perform(get("/api/public/homepage-seo").param("tenantId", "tnt_b").param("locale", "zh-TW"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("Tenant B — 另一品牌"));
  }

  @Test
  void trackingAcceptsValidEvents() throws Exception {
    homepageTrackingService.drainTestBuffer();
    String body =
        objectMapper.writeValueAsString(
            Map.of(
                "events",
                java.util.List.of(
                    Map.of(
                        "eventType",
                        "cta_click",
                        "tenantId",
                        "tnt_a",
                        "locale",
                        "en-US",
                        "sectionId",
                        "hero",
                        "campaign",
                        "homepage_intro",
                        "pageVariant",
                        "default",
                        "occurredAt",
                        Instant.parse("2026-04-14T10:20:30Z").toString(),
                        "metadata",
                        Map.of("ctaId", "hero_primary")))));
    mockMvc
        .perform(post("/api/public/tracking/events").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.accepted").value(1))
        .andExpect(jsonPath("$.data.rejected").value(0));
    assertThat(homepageTrackingService.drainTestBuffer()).hasSize(1);
  }

  @Test
  void trackingRejectsUnknownTenant() throws Exception {
    String body =
        objectMapper.writeValueAsString(
            Map.of(
                "events",
                java.util.List.of(
                    Map.of(
                        "eventType",
                        "section_view",
                        "tenantId",
                        "no_such_tenant",
                        "locale",
                        "en-US",
                        "sectionId",
                        "hero",
                        "campaign",
                        "homepage_intro",
                        "occurredAt",
                        Instant.now().toString()))));
    mockMvc
        .perform(post("/api/public/tracking/events").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accepted").value(0))
        .andExpect(jsonPath("$.data.rejected").value(1));
  }
}
