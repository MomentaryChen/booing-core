package com.bookingcore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "booking.platform.time-zone=UTC",
      "booking.platform.system-admin-token=integration-test-secret"
    })
class SystemAdminTokenFilterTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void systemApisRejectWithoutToken() throws Exception {
    mockMvc
        .perform(get("/api/system/command-center"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("System admin token required"));
  }

  @Test
  void systemApisAllowWithHeaderToken() throws Exception {
    mockMvc
        .perform(
            get("/api/system/command-center").header("X-System-Admin-Token", "integration-test-secret"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.liveFeed").isArray());
  }

  @Test
  void systemApisAllowWithBearerToken() throws Exception {
    mockMvc
        .perform(
            get("/api/system/overview")
                .header("Authorization", "Bearer integration-test-secret"))
        .andExpect(status().isOk());
  }

  @Test
  void systemAdminJwtCannotAccessMerchantScopedApis() throws Exception {
    String adminToken = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    mockMvc
        .perform(
            get("/api/merchant/99999/profile")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void merchantNamespaceRejectsWithoutJwt() throws Exception {
    mockMvc.perform(get("/api/merchant/1/profile")).andExpect(status().isUnauthorized());
  }

  @Test
  void merchantJwtGets403OnSystemApiButAdminJwtCanAccess() throws Exception {
    String merchantToken =
        TestJwtHelper.login(mockMvc, objectMapper, "merchant@example.com", "merchant");
    mockMvc
        .perform(
            post("/api/system/domain-templates")
                .header("Authorization", "Bearer " + merchantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"domainName\":\"forbidden-test\",\"fieldsJson\":\"{}\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Forbidden"));

    String adminToken = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    mockMvc
        .perform(
            post("/api/system/domain-templates")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"domainName\":\"allowed-test\",\"fieldsJson\":\"{}\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.domainName").value("allowed-test"));
  }
}
