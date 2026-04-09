package com.bookingcore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
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
      "booking.platform.auth.bootstrap-system-admin.enabled=false",
      "booking.platform.auth.bootstrap-default-client.enabled=false",
      "booking.platform.auth.bootstrap-default-merchant.enabled=false",
      "booking.platform.auth.bootstrap-default-merchant-user.enabled=false",
      "booking.platform.dev-users[0].username=dev-only-user",
      "booking.platform.dev-users[0].password=dev-only-pass",
      "booking.platform.dev-users[0].role=SYSTEM_ADMIN"
    })
class DevUsersFallbackProfileGuardTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void nonDevProfileCannotUseDevUsersFallback() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"dev-only-user\",\"password\":\"dev-only-pass\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid credentials"));
  }
}
