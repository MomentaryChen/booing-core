package com.bookingcore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "booking.platform.auth.login.rate-limit-enabled=true",
      "booking.platform.auth.login.max-attempts-per-ip-per-window=3",
      "booking.platform.auth.login.window-minutes=10"
    })
class LoginRateLimitApiTest {

  /** Isolate rate-limit bucket from other tests that log in from 127.0.0.1. */
  private static final String TEST_CLIENT_IP = "10.66.99.17";

  @Autowired private MockMvc mockMvc;

  @Test
  void fourthLoginAttemptFromSameClientReturns429() throws Exception {
    RequestPostProcessor clientIp =
        request -> {
          request.setRemoteAddr(TEST_CLIENT_IP);
          return request;
        };
    String body = "{\"username\":\"nouser\",\"password\":\"bad\"}";
    for (int i = 0; i < 3; i++) {
      mockMvc
          .perform(
              post("/api/auth/login")
                  .with(clientIp)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isUnauthorized());
    }
    mockMvc
        .perform(
            post("/api/auth/login")
                .with(clientIp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  void bootstrapAdminLoginIsAlsoRateLimitedByClientIp() throws Exception {
    RequestPostProcessor clientIp =
        request -> {
          request.setRemoteAddr("10.66.99.18");
          return request;
        };
    String body = "{\"username\":\"admin\",\"password\":\"wrong-admin-password\"}";
    for (int i = 0; i < 3; i++) {
      mockMvc
          .perform(
              post("/api/auth/login")
                  .with(clientIp)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isUnauthorized());
    }
    mockMvc
        .perform(
            post("/api/auth/login")
                .with(clientIp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isTooManyRequests());
  }
}
