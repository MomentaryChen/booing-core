package com.bookingcore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.platform.PlatformUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CredentialRevocationApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PlatformUserRepository platformUserRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void logoutIncrementsCredentialVersionAndInvalidatesAccessToken() throws Exception {
    String username = "revoke-test-" + System.nanoTime();
    PlatformUser u = new PlatformUser();
    u.setUsername(username);
    u.setPasswordHash(passwordEncoder.encode("secret-pass"));
    u.setRole(com.bookingcore.security.PlatformUserRole.SYSTEM_ADMIN);
    u.setEnabled(true);
    platformUserRepository.save(u);

    String loginBody =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"username\":\"" + username + "\",\"password\":\"secret-pass\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode loginJson = objectMapper.readTree(loginBody);
    String token = loginJson.get("accessToken").asText();

    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    mockMvc
        .perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }
}
