package com.bookingcore.api;

import com.bookingcore.api.ApiDtos.ApiEnvelope;
import com.bookingcore.api.ApiDtos.HomepageConfigResponse;
import com.bookingcore.api.ApiDtos.HomepageSeoResponse;
import com.bookingcore.api.ApiDtos.HomepageTrackingAcceptResponse;
import com.bookingcore.api.ApiDtos.HomepageTrackingBatchRequest;
import com.bookingcore.service.homepage.HomepagePublicService;
import com.bookingcore.service.homepage.HomepageTrackingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicHomepageController {

  private final HomepagePublicService homepagePublicService;
  private final HomepageTrackingService homepageTrackingService;

  public PublicHomepageController(
      HomepagePublicService homepagePublicService, HomepageTrackingService homepageTrackingService) {
    this.homepagePublicService = homepagePublicService;
    this.homepageTrackingService = homepageTrackingService;
  }

  @GetMapping("/api/public/homepage-config")
  public ApiEnvelope<HomepageConfigResponse> homepageConfig(
      @RequestParam("tenantId") String tenantId,
      @RequestParam("locale") String locale,
      @RequestParam(value = "pageVariant", required = false, defaultValue = "default") String pageVariant) {
    return ApiDtos.success(homepagePublicService.buildConfig(tenantId, locale, pageVariant));
  }

  @GetMapping("/api/public/homepage-seo")
  public ApiEnvelope<HomepageSeoResponse> homepageSeo(
      @RequestParam("tenantId") String tenantId,
      @RequestParam("locale") String locale,
      @RequestParam(value = "variant", required = false, defaultValue = "default") String variant) {
    return ApiDtos.success(homepagePublicService.buildSeo(tenantId, locale, variant));
  }

  @PostMapping("/api/public/tracking/events")
  public ApiEnvelope<HomepageTrackingAcceptResponse> tracking(
      @Valid @RequestBody HomepageTrackingBatchRequest request) {
    return ApiDtos.success(homepageTrackingService.accept(request));
  }
}
