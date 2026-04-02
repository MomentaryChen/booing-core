package com.bookingcore;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MeNavigationApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void adminNavigationIncludesSystemDashboard() throws Exception {
    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    mockMvc
        .perform(get("/api/me/navigation").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routeKeys").isArray())
        .andExpect(jsonPath("$.routeKeys", hasItem("nav.system.dashboard")))
        .andExpect(jsonPath("$.routeKeys", hasItem("nav.client.todo")));
  }

  @Test
  void merchantNavigationExcludesSystemDashboard() throws Exception {
    String token = TestJwtHelper.login(mockMvc, objectMapper, "merchant", "merchant");
    mockMvc
        .perform(get("/api/me/navigation").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routeKeys", not(hasItem("nav.system.dashboard"))))
        .andExpect(jsonPath("$.routeKeys", hasItem("nav.merchant.dashboard")));
  }

  @Test
  void navigationWithoutTokenReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/me/navigation")).andExpect(status().isUnauthorized());
  }
}
