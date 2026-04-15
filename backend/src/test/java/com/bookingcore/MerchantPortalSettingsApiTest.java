package com.bookingcore;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookingcore.config.TestIntegrationUserSeed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MerchantPortalSettingsApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void putCustomization_persistsNotificationFlags() throws Exception {
    String token =
        TestJwtHelper.login(
            mockMvc, objectMapper, TestIntegrationUserSeed.MERCHANT_LOGIN_EMAIL, TestIntegrationUserSeed.MERCHANT_LOGIN_PASSWORD);
    String meJson =
        mockMvc
            .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String merchantId = objectMapper.readTree(meJson).path("merchantId").asText();

    String getJson =
        mockMvc
            .perform(
                get("/api/merchant/" + merchantId + "/customization")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    var root = objectMapper.readTree(getJson);
    ObjectNode putBody = objectMapper.createObjectNode();
    putBody.put("themePreset", root.path("themePreset").asText());
    putBody.put("themeColor", root.path("themeColor").asText());
    putBody.put("heroTitle", root.path("heroTitle").asText());
    putBody.put("bookingFlowText", root.path("bookingFlowText").asText());
    putBody.put("inviteCode", root.path("inviteCode").asText(""));
    putBody.put("termsText", root.path("termsText").asText(""));
    putBody.put("announcementText", root.path("announcementText").asText(""));
    putBody.put("faqJson", root.path("faqJson").asText());
    putBody.put("bufferMinutes", root.path("bufferMinutes").asInt());
    putBody.put("homepageSectionsJson", root.path("homepageSectionsJson").asText());
    putBody.put("categoryOrderJson", root.path("categoryOrderJson").asText());
    putBody.put("notificationNewBooking", false);
    putBody.put("notificationCancellation", false);
    putBody.put("notificationDailySummary", true);
    String body = objectMapper.writeValueAsString(putBody);

    mockMvc
        .perform(
            put("/api/merchant/" + merchantId + "/customization")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notificationNewBooking").value(false))
        .andExpect(jsonPath("$.notificationCancellation").value(false))
        .andExpect(jsonPath("$.notificationDailySummary").value(true));

    mockMvc
        .perform(
            get("/api/merchant/" + merchantId + "/customization")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notificationNewBooking").value(false))
        .andExpect(jsonPath("$.notificationCancellation").value(false))
        .andExpect(jsonPath("$.notificationDailySummary").value(true));
  }

  @Test
  void putBusinessHours_emptyArray_clearsHours() throws Exception {
    String token =
        TestJwtHelper.login(
            mockMvc, objectMapper, TestIntegrationUserSeed.MERCHANT_LOGIN_EMAIL, TestIntegrationUserSeed.MERCHANT_LOGIN_PASSWORD);
    String merchantId =
        objectMapper
            .readTree(
                mockMvc
                    .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .path("merchantId")
            .asText();

    mockMvc
        .perform(
            put("/api/merchant/" + merchantId + "/business-hours")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        List.of(
                            Map.of(
                                "dayOfWeek",
                                DayOfWeek.MONDAY.name(),
                                "startTime",
                                "09:00:00",
                                "endTime",
                                "12:00:00")))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            put("/api/merchant/" + merchantId + "/business-hours")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/merchant/" + merchantId + "/business-hours")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void putBusinessHours_invalidInterval_rejectsWithoutClearingPriorRows() throws Exception {
    String token =
        TestJwtHelper.login(
            mockMvc, objectMapper, TestIntegrationUserSeed.MERCHANT_LOGIN_EMAIL, TestIntegrationUserSeed.MERCHANT_LOGIN_PASSWORD);
    String merchantId =
        objectMapper
            .readTree(
                mockMvc
                    .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .path("merchantId")
            .asText();

    mockMvc
        .perform(
            put("/api/merchant/" + merchantId + "/business-hours")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        List.of(
                            Map.of(
                                "dayOfWeek",
                                DayOfWeek.TUESDAY.name(),
                                "startTime",
                                "10:00:00",
                                "endTime",
                                "14:00:00")))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            put("/api/merchant/" + merchantId + "/business-hours")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        List.of(
                            Map.of(
                                "dayOfWeek",
                                DayOfWeek.WEDNESDAY.name(),
                                "startTime",
                                "10:00:00",
                                "endTime",
                                "10:00:00")))))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            get("/api/merchant/" + merchantId + "/business-hours")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].dayOfWeek").value(DayOfWeek.TUESDAY.name()));
  }

  @Test
  void merchantCannotReadCustomizationForOtherMerchant() throws Exception {
    String adminToken = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    String slug = "other-merchant-st-" + System.nanoTime();
    String created =
        mockMvc
            .perform(
                post("/api/merchant/merchants")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"Other Merchant\",\"slug\":\"" + slug + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID otherMerchantId = UUID.fromString(objectMapper.readTree(created).path("id").asText());

    String merchantToken =
        TestJwtHelper.login(
            mockMvc, objectMapper, TestIntegrationUserSeed.MERCHANT_LOGIN_EMAIL, TestIntegrationUserSeed.MERCHANT_LOGIN_PASSWORD);

    mockMvc
        .perform(
            get("/api/merchant/" + otherMerchantId + "/customization")
                .header("Authorization", "Bearer " + merchantToken))
        .andExpect(status().isForbidden());
  }
}
