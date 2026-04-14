package com.bookingcore;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MeNavigationApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  /**
   * Dev-login user {@code merchant} resolves {@code merchantId} from the first merchant row.
   * This test class must work when run alone (no dependency on other test classes).
   */
  @BeforeEach
  void ensureAtLeastOneMerchantExists() throws Exception {
    String adminToken = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    var result =
        mockMvc
            .perform(
                post("/api/merchant/merchants")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"MeNav Seed Merchant\",\"slug\":\"me-nav-seed-merchant\"}"))
            .andReturn();
    int status = result.getResponse().getStatus();
    if (status != 200 && status != 400) {
      throw new AssertionError(
          "Unexpected status creating seed merchant: "
              + status
              + " body="
              + result.getResponse().getContentAsString());
    }
  }

  @Test
  void adminNavigationIncludesSystemDashboard() throws Exception {
    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    mockMvc
        .perform(get("/api/me/navigation").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routeKeys").isArray())
        .andExpect(jsonPath("$.routeKeys", hasItem("nav.system.dashboard")))
        .andExpect(jsonPath("$.routeKeys", hasItem("nav.system.users")))
        .andExpect(jsonPath("$.routeKeys", hasItem("nav.client.todo")));
  }

  @Test
  void merchantNavigationExcludesSystemDashboard() throws Exception {
    String token = TestJwtHelper.login(mockMvc, objectMapper, "merchant@example.com", "merchant");
    mockMvc
        .perform(get("/api/me/navigation").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routeKeys", not(hasItem("nav.system.dashboard"))))
        .andExpect(jsonPath("$.routeKeys", not(hasItem("nav.system.users"))))
        .andExpect(jsonPath("$.routeKeys", hasItem("nav.merchant.dashboard")));
  }

  @Test
  void clientNavigationIncludesClientTodoExcludesMerchantDashboard() throws Exception {
    String token = TestJwtHelper.login(mockMvc, objectMapper, "client@example.com", "client");
    mockMvc
        .perform(get("/api/me/navigation").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routeKeys", hasItem("nav.client.todo")))
        .andExpect(jsonPath("$.routeKeys", hasItem("nav.store.public")))
        .andExpect(jsonPath("$.routeKeys", not(hasItem("nav.merchant.dashboard"))))
        .andExpect(jsonPath("$.routeKeys", not(hasItem("nav.system.dashboard"))))
        .andExpect(jsonPath("$.routeKeys", not(hasItem("nav.system.users"))));
  }

  @Test
  void navigationWithoutTokenReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/me/navigation")).andExpect(status().isUnauthorized());
  }
}
