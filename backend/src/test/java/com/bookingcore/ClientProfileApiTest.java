package com.bookingcore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookingcore.modules.client.ClientProfileRepository;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.security.PlatformUserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Map;
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
class ClientProfileApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private ClientProfileRepository clientProfileRepository;
  @PersistenceContext private EntityManager entityManager;

  @Test
  void getClientProfile_unauthenticated_returnsAnonymousPayload() throws Exception {
    mockMvc
        .perform(get("/api/client/profile"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(false))
        .andExpect(jsonPath("$.role", nullValue()))
        .andExpect(jsonPath("$.suggestedName", nullValue()))
        .andExpect(jsonPath("$.suggestedContact", nullValue()));
  }

  @Test
  void getClientProfile_authenticated_returnsProfileFields() throws Exception {
    PlatformUser user = persistClientUser("profile-get-" + System.nanoTime());
    String token = TestJwtHelper.login(mockMvc, objectMapper, user.getUsername(), "secret-pass");

    mockMvc
        .perform(get("/api/client/profile").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(true))
        .andExpect(jsonPath("$.role").value("CLIENT_USER"))
        .andExpect(jsonPath("$.suggestedName").value(user.getUsername()))
        .andExpect(jsonPath("$.suggestedContact").isEmpty());
  }

  @Test
  void putClientProfile_unauthenticated_returns401() throws Exception {
    mockMvc
        .perform(
            put("/api/client/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("suggestedName", "n", "suggestedContact", "c"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void putClientProfile_merchant_returns403() throws Exception {
    String token = loginMerchantUser();
    mockMvc
        .perform(
            put("/api/client/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("suggestedName", "Should Not Save", "suggestedContact", "000"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void putClientProfile_client_createsRowAndTrimsToNull() throws Exception {
    PlatformUser user = persistClientUser("profile-put-" + System.nanoTime());
    String token = TestJwtHelper.login(mockMvc, objectMapper, user.getUsername(), "secret-pass");

    mockMvc
        .perform(
            put("/api/client/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("suggestedName", "  Booked As  ", "suggestedContact", "   "))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(true))
        .andExpect(jsonPath("$.suggestedName").value("Booked As"))
        .andExpect(jsonPath("$.suggestedContact", nullValue()));

    assertThat(clientProfileRepository.findByPlatformUserId(user.getId()))
        .isPresent()
        .get()
        .satisfies(
            p -> {
              assertThat(p.getDisplayName()).isEqualTo("Booked As");
              assertThat(p.getContactPhone()).isNull();
            });
  }

  @Test
  void putClientProfile_client_updatesExisting() throws Exception {
    PlatformUser user = persistClientUser("profile-upd-" + System.nanoTime());
    String token = TestJwtHelper.login(mockMvc, objectMapper, user.getUsername(), "secret-pass");

    mockMvc
        .perform(
            put("/api/client/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("suggestedName", "First", "suggestedContact", "111"))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            put("/api/client/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("suggestedName", "Second", "suggestedContact", "222"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.suggestedName").value("Second"))
        .andExpect(jsonPath("$.suggestedContact").value("222"));

    assertThat(clientProfileRepository.findByPlatformUserId(user.getId()).orElseThrow().getDisplayName())
        .isEqualTo("Second");
    assertThat(clientProfileRepository.findAll()).hasSize(1);
  }

  @Test
  void putClientProfile_suggestedNameTooLong_returns400() throws Exception {
    PlatformUser user = persistClientUser("profile-bad-" + System.nanoTime());
    String token = TestJwtHelper.login(mockMvc, objectMapper, user.getUsername(), "secret-pass");
    String tooLong = "x".repeat(121);

    mockMvc
        .perform(
            put("/api/client/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("suggestedName", tooLong))))
        .andExpect(status().isBadRequest());

    assertThat(clientProfileRepository.findByPlatformUserId(user.getId())).isEmpty();
  }

  private PlatformUser persistClientUser(String username) {
    PlatformUser user = new PlatformUser();
    user.setUsername(username);
    user.setPasswordHash(passwordEncoder.encode("secret-pass"));
    user.setRole(PlatformUserRole.CLIENT);
    user.setEnabled(true);
    entityManager.persist(user);
    entityManager.flush();
    return user;
  }

  private String loginMerchantUser() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("M " + System.nanoTime());
    merchant.setSlug("m-" + System.nanoTime());
    merchant.setActive(true);
    entityManager.persist(merchant);

    PlatformUser user = new PlatformUser();
    user.setUsername("merch-prof-" + System.nanoTime());
    user.setPasswordHash(passwordEncoder.encode("secret-pass"));
    user.setRole(PlatformUserRole.MERCHANT);
    user.setMerchant(merchant);
    user.setEnabled(true);
    entityManager.persist(user);
    entityManager.flush();
    return TestJwtHelper.login(mockMvc, objectMapper, user.getUsername(), "secret-pass");
  }
}
