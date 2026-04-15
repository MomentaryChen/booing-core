package com.bookingcore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookingcore.modules.client.ClientProfileRepository;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.platform.PlatformUserRepository;
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
  @Autowired private PlatformUserRepository platformUserRepository;
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
        .andExpect(jsonPath("$.role", anyOf(is("CLIENT"), is("CLIENT_USER"))))
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

  @Test
  void getClientProfilePreferences_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/client/profile/preferences")).andExpect(status().isUnauthorized());
  }

  @Test
  void getClientProfilePreferences_client_returnsNullDefaults() throws Exception {
    PlatformUser user = persistClientUser("pref-get-" + System.nanoTime());
    String token = TestJwtHelper.login(mockMvc, objectMapper, user.getUsername(), "secret-pass");

    mockMvc
        .perform(get("/api/client/profile/preferences").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.language", nullValue()))
        .andExpect(jsonPath("$.timezone", nullValue()))
        .andExpect(jsonPath("$.currency", nullValue()))
        .andExpect(jsonPath("$.emailNotifications", nullValue()))
        .andExpect(jsonPath("$.smsNotifications", nullValue()));
  }

  @Test
  void putClientProfilePreferences_client_persistsFields() throws Exception {
    PlatformUser user = persistClientUser("pref-put-" + System.nanoTime());
    String token = TestJwtHelper.login(mockMvc, objectMapper, user.getUsername(), "secret-pass");

    mockMvc
        .perform(
            put("/api/client/profile/preferences")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "language", " zh-TW ",
                            "timezone", " Asia/Taipei ",
                            "currency", " TWD ",
                            "emailNotifications", true,
                            "smsNotifications", false))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.language").value("zh-TW"))
        .andExpect(jsonPath("$.timezone").value("Asia/Taipei"))
        .andExpect(jsonPath("$.currency").value("TWD"))
        .andExpect(jsonPath("$.emailNotifications").value(true))
        .andExpect(jsonPath("$.smsNotifications").value(false));

    assertThat(clientProfileRepository.findByPlatformUserId(user.getId()))
        .isPresent()
        .get()
        .satisfies(
            p -> {
              assertThat(p.getLanguage()).isEqualTo("zh-TW");
              assertThat(p.getTimezone()).isEqualTo("Asia/Taipei");
              assertThat(p.getCurrency()).isEqualTo("TWD");
              assertThat(p.getEmailNotifications()).isTrue();
              assertThat(p.getSmsNotifications()).isFalse();
            });
  }

  @Test
  void putClientProfilePreferences_merchant_returns403() throws Exception {
    String token = loginMerchantUser();
    mockMvc
        .perform(
            put("/api/client/profile/preferences")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("language", "en-US"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void patchClientProfilePassword_unauthenticated_returns401() throws Exception {
    mockMvc
        .perform(
            patch("/api/client/profile/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("currentPassword", "secret-pass", "newPassword", "new-secret-pass"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void patchClientProfilePassword_wrongCurrent_returns422() throws Exception {
    PlatformUser user = persistClientUser("profile-pass-fail-" + System.nanoTime());
    String token = TestJwtHelper.login(mockMvc, objectMapper, user.getUsername(), "secret-pass");

    mockMvc
        .perform(
            patch("/api/client/profile/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("currentPassword", "wrong-pass", "newPassword", "new-secret-pass"))))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void patchClientProfilePassword_client_success() throws Exception {
    PlatformUser user = persistClientUser("profile-pass-ok-" + System.nanoTime());
    String token = TestJwtHelper.login(mockMvc, objectMapper, user.getUsername(), "secret-pass");

    mockMvc
        .perform(
            patch("/api/client/profile/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("currentPassword", "secret-pass", "newPassword", "new-secret-pass"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.updatedAt").isNotEmpty());

    PlatformUser updated = platformUserRepository.findById(user.getId()).orElseThrow();
    assertThat(passwordEncoder.matches("new-secret-pass", updated.getPasswordHash())).isTrue();
    assertThat(updated.getPasswordUpdatedAt()).isNotNull();
    TestJwtHelper.login(mockMvc, objectMapper, user.getUsername(), "new-secret-pass");
  }

  @Test
  void patchClientProfilePassword_invalidatesOldJwtAndAcceptsNewJwt() throws Exception {
    PlatformUser user = persistClientUser("profile-pass-jwt-" + System.nanoTime());
    String oldToken = TestJwtHelper.login(mockMvc, objectMapper, user.getUsername(), "secret-pass");

    mockMvc
        .perform(get("/api/client/bookings").header("Authorization", "Bearer " + oldToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            patch("/api/client/profile/password")
                .header("Authorization", "Bearer " + oldToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("currentPassword", "secret-pass", "newPassword", "new-secret-pass"))))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/client/bookings").header("Authorization", "Bearer " + oldToken))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(get("/api/client/profile/preferences").header("Authorization", "Bearer " + oldToken))
        .andExpect(status().isUnauthorized());

    String newToken = TestJwtHelper.login(mockMvc, objectMapper, user.getUsername(), "new-secret-pass");
    mockMvc
        .perform(get("/api/client/bookings").header("Authorization", "Bearer " + newToken))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/client/profile/preferences").header("Authorization", "Bearer " + newToken))
        .andExpect(status().isOk());
  }

  private PlatformUser persistClientUser(String username) {
    PlatformUser user = new PlatformUser();
    user.setUsername(username + "@example.com");
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
    user.setUsername("merch-prof-" + System.nanoTime() + "@example.com");
    user.setPasswordHash(passwordEncoder.encode("secret-pass"));
    user.setRole(PlatformUserRole.MERCHANT);
    user.setMerchant(merchant);
    user.setEnabled(true);
    entityManager.persist(user);
    entityManager.flush();
    return TestJwtHelper.login(mockMvc, objectMapper, user.getUsername(), "secret-pass");
  }
}
