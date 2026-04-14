package com.bookingcore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookingcore.service.PublicRegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void shouldCreateMerchantAndReadDefaultCustomization() throws Exception {
    String adminToken = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    mockMvc
        .perform(
            post("/api/merchant/merchants")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Merchant\",\"slug\":\"test-merchant\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.slug").value("test-merchant"));

    String merchantToken =
        TestJwtHelper.login(mockMvc, objectMapper, "merchant@example.com", "merchant");
    String meJson =
        mockMvc
            .perform(get("/api/auth/me").header("Authorization", "Bearer " + merchantToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long merchantId = objectMapper.readTree(meJson).path("merchantId").asLong();
    mockMvc
        .perform(
            get("/api/merchant/" + merchantId + "/customization")
                .header("Authorization", "Bearer " + merchantToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.themeColor").exists())
        .andExpect(jsonPath("$.homepageSectionsJson").exists());
  }

  @Test
  void shouldRegisterMerchantViaMerchantNamespace() throws Exception {
    mockMvc.perform(post("/api/merchant/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Portal Merchant\",\"slug\":\"portal-merchant-reg\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.slug").value("portal-merchant-reg"))
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.nextDestination").value(PublicRegistrationService.NEXT_DESTINATION_MERCHANT_LOGIN));
  }

  @Test
  void merchantRoleCannotCreateMerchantViaAdminEndpoint() throws Exception {
    String adminToken = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    mockMvc
        .perform(
            post("/api/merchant/merchants")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Seed For Merchant Test\",\"slug\":\"seed-merchant-merch-test\"}"))
        .andExpect(status().isOk());

    String merchantToken =
        TestJwtHelper.login(mockMvc, objectMapper, "merchant@example.com", "merchant");
    mockMvc
        .perform(
            post("/api/merchant/merchants")
                .header("Authorization", "Bearer " + merchantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Should Fail\",\"slug\":\"should-fail-merch\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void loginResponseShouldKeepLegacyRoleAndExposeUnifiedRbacFields() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.role").value("SYSTEM_ADMIN"))
        .andExpect(jsonPath("$.roles[0]").value("SYSTEM_ADMIN"))
        .andExpect(jsonPath("$.permissions").isArray())
        .andExpect(jsonPath("$.permissions[0]").exists());
  }

  @Test
  void nonAdminLoginRejectsNonEmailIdentifier() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"merchant\",\"password\":\"merchant\"}"))
        .andExpect(status().isUnauthorized());
  }
}
