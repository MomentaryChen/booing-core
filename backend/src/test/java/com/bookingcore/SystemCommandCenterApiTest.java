package com.bookingcore;

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
class SystemCommandCenterApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void commandCenterReturnsPayload() throws Exception {
    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    mockMvc
        .perform(get("/api/system/command-center").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.timeZone").value("UTC"))
        .andExpect(jsonPath("$.todayBookings").exists())
        .andExpect(jsonPath("$.weekBookings").exists())
        .andExpect(jsonPath("$.pendingActions").exists())
        .andExpect(jsonPath("$.pendingActionsTotal").exists())
        .andExpect(jsonPath("$.pendingActionsThisWeek").exists())
        .andExpect(jsonPath("$.revenueToday").exists())
        .andExpect(jsonPath("$.occupancyRate").exists())
        .andExpect(jsonPath("$.heatMap").isArray())
        .andExpect(jsonPath("$.sparklineWeek").isArray())
        .andExpect(jsonPath("$.liveFeed").isArray())
        .andExpect(jsonPath("$.weekStart").exists());
  }
}
