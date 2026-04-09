package com.bookingcore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthMeApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void authMeWithoutTokenReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void authMeReturnsPrincipalForAdmin() throws Exception {
    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("admin"))
        .andExpect(jsonPath("$.role").value("SYSTEM_ADMIN"))
        .andExpect(jsonPath("$.sessionState").value("CONTEXT_SET"))
        .andExpect(jsonPath("$.availableContexts").isArray())
        .andExpect(jsonPath("$.availableContexts[0]").exists())
        .andExpect(jsonPath("$.permissions").isArray());
  }

  @Test
  void refreshReturnsNewAccessToken() throws Exception {
    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    mockMvc
        .perform(post("/api/auth/refresh").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(jsonPath("$.role").value("SYSTEM_ADMIN"));
  }

  @Test
  void logoutReturnsNoContent() throws Exception {
    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    mockMvc
        .perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
  }

  @Test
  void contextSelectWithoutRoleBehavesAsRefresh() throws Exception {
    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    mockMvc
        .perform(post("/api/auth/context/select").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(jsonPath("$.role").value("SYSTEM_ADMIN"));
  }

  @Test
  void contextSelectForbiddenWhenContextNotAllowed() throws Exception {
    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    mockMvc
        .perform(
            post("/api/auth/context/select")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"MERCHANT\",\"merchantId\":999999}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void contextSelectReturnsMerchantTokenWhenMerchantOptionExists() throws Exception {
    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    String body =
        mockMvc
            .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode root = objectMapper.readTree(body);
    JsonNode contexts = root.path("availableContexts");
    if (contexts.size() < 2) {
      return;
    }
    Long merchantId = null;
    for (JsonNode c : contexts) {
      if ("MERCHANT".equals(c.path("role").asText()) && c.path("merchantId").isNumber()) {
        merchantId = c.path("merchantId").asLong();
        break;
      }
    }
    if (merchantId == null) {
      return;
    }
    mockMvc
        .perform(
            post("/api/auth/context/select")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"role\":\"MERCHANT\",\"merchantId\":"
                        + merchantId
                        + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("MERCHANT"))
        .andExpect(jsonPath("$.accessToken").isString());
  }
}
