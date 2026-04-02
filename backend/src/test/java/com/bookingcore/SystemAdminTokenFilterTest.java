package com.bookingcore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
  void nonSystemApisStillPublic() throws Exception {
    String adminToken = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    mockMvc
        .perform(
            get("/api/merchant/99999/profile")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isBadRequest());
  }
}
