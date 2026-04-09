package com.bookingcore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.platform.PlatformUserRepository;
import com.bookingcore.security.PlatformUserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "booking.platform.auth.login.lockout-enabled=true",
      "booking.platform.auth.login.max-failed-attempts-per-user=3",
      "booking.platform.auth.login.failure-window-minutes=60",
      "booking.platform.auth.login.lockout-duration-minutes=60"
    })
@Transactional
class AccountLockoutApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private PlatformUserRepository platformUserRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void repeatedWrongPasswordThenLockoutReturnsSameUnauthorizedAsInvalidLogin() throws Exception {
    String username = "lockout-test-" + System.nanoTime();
    String goodPassword = "good-secret";
    PlatformUser u = new PlatformUser();
    u.setUsername(username);
    u.setPasswordHash(passwordEncoder.encode(goodPassword));
    u.setRole(PlatformUserRole.SYSTEM_ADMIN);
    u.setEnabled(true);
    platformUserRepository.save(u);

    String badBody = "{\"username\":\"" + username + "\",\"password\":\"wrong\"}";
    for (int i = 0; i < 3; i++) {
      mockMvc
          .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(badBody))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }
    mockMvc
        .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(badBody))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid credentials"));

    String goodBody = "{\"username\":\"" + username + "\",\"password\":\"" + goodPassword + "\"}";
    mockMvc
        .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(goodBody))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid credentials"));

    PlatformUser locked =
        platformUserRepository.findByUsername(username).orElseThrow();
    locked.setLockedUntil(null);
    locked.setFailedLoginCount(0);
    locked.setFailedLoginWindowStartedAt(null);
    platformUserRepository.save(locked);

    mockMvc
        .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(goodBody))
        .andExpect(status().isOk());
  }

  @Test
  void disabledUserLoginReturnsGenericInvalidCredentialsMessage() throws Exception {
    String username = "disabled-test-" + System.nanoTime();
    String password = "secret";
    PlatformUser u = new PlatformUser();
    u.setUsername(username);
    u.setPasswordHash(passwordEncoder.encode(password));
    u.setRole(PlatformUserRole.SYSTEM_ADMIN);
    u.setEnabled(false);
    platformUserRepository.save(u);

    String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    mockMvc
        .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid credentials"));
  }

  @Test
  void bootstrapAdminAccountIsAlsoSubjectToLockout() throws Exception {
    PlatformUser admin = platformUserRepository.findByUsername("admin").orElseThrow();
    admin.setFailedLoginCount(0);
    admin.setFailedLoginWindowStartedAt(null);
    admin.setLockedUntil(null);
    platformUserRepository.save(admin);

    String badBody = "{\"username\":\"admin\",\"password\":\"wrong-admin-password\"}";
    for (int i = 0; i < 3; i++) {
      mockMvc
          .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(badBody))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid credentials"));
  }
}
