package com.bookingcore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.MerchantMembership;
import com.bookingcore.modules.merchant.MerchantMembershipStatus;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.platform.rbac.PlatformRbacBindingStatus;
import com.bookingcore.modules.platform.rbac.PlatformUserRbacBinding;
import com.bookingcore.modules.platform.rbac.RbacRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Map;
import java.util.UUID;
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
class AuthMeApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordEncoder passwordEncoder;
  @PersistenceContext private EntityManager entityManager;

  private void ensureAdminUser() {
    Long existing =
        entityManager
            .createQuery(
                "select count(u) from PlatformUser u where u.username = :username", Long.class)
            .setParameter("username", "admin")
            .getSingleResult();
    if (existing != null && existing > 0) {
      return;
    }
    PlatformUser admin = new PlatformUser();
    admin.setUsername("admin");
    admin.setPasswordHash(passwordEncoder.encode("admin"));
    admin.setRole(com.bookingcore.security.PlatformUserRole.SYSTEM_ADMIN);
    admin.setEnabled(true);
    entityManager.persist(admin);
    entityManager.flush();
  }

  @Test
  void authMeWithoutTokenReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void authMeReturnsPrincipalForAdmin() throws Exception {
    ensureAdminUser();
    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("admin"))
        .andExpect(jsonPath("$.role").value("SYSTEM_ADMIN"))
        .andExpect(jsonPath("$.canonicalRole").value("SYSTEM_ADMIN"))
        .andExpect(jsonPath("$.roleAliases").isArray())
        .andExpect(jsonPath("$.roleAliases[0]").exists())
        .andExpect(jsonPath("$.sessionState").value("CONTEXT_SET"))
        .andExpect(jsonPath("$.availableContexts").isArray())
        .andExpect(jsonPath("$.availableContexts[0]").exists())
        .andExpect(jsonPath("$.permissions").isArray());
  }

  @Test
  void refreshReturnsNewAccessToken() throws Exception {
    ensureAdminUser();
    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    mockMvc
        .perform(post("/api/auth/refresh").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(jsonPath("$.role").value("SYSTEM_ADMIN"));
  }

  @Test
  void logoutReturnsNoContent() throws Exception {
    ensureAdminUser();
    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    mockMvc
        .perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
  }

  @Test
  void contextSelectWithoutRoleBehavesAsRefresh() throws Exception {
    ensureAdminUser();
    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    mockMvc
        .perform(post("/api/auth/context/select").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(jsonPath("$.role").value("SYSTEM_ADMIN"));
  }

  @Test
  void contextSelectForbiddenWhenContextNotAllowed() throws Exception {
    ensureAdminUser();
    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    mockMvc
        .perform(
            post("/api/auth/context/select")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"role\":\"MERCHANT\",\"merchantId\":\"019e0000-0000-7000-8000-000000009999\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void contextSelectReturnsMerchantTokenWhenMerchantOptionExists() throws Exception {
    ensureAdminUser();
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
    UUID merchantId = null;
    for (JsonNode c : contexts) {
      if ("MERCHANT".equals(c.path("role").asText()) && c.path("merchantId").isTextual()) {
        merchantId = UUID.fromString(c.path("merchantId").asText());
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
                .content(objectMapper.writeValueAsString(Map.of("role", "MERCHANT", "merchantId", merchantId))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("MERCHANT"))
        .andExpect(jsonPath("$.accessToken").isString());
  }

  @Test
  void contextSwitchRequiresActiveMerchantMembership() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Ctx Merchant " + System.nanoTime());
    merchant.setSlug("ctx-merchant-" + System.nanoTime());
    merchant.setActive(true);
    entityManager.persist(merchant);

    PlatformUser user = new PlatformUser();
    user.setUsername("ctx-user-" + System.nanoTime() + "@test.local");
    user.setPasswordHash(passwordEncoder.encode("secret-pass"));
    user.setRole(com.bookingcore.security.PlatformUserRole.CLIENT);
    user.setEnabled(true);
    entityManager.persist(user);

    RbacRole merchantRole =
        entityManager
            .createQuery("select r from RbacRole r where r.code = :code", RbacRole.class)
            .setParameter("code", "MERCHANT")
            .getResultStream()
            .findFirst()
            .orElseGet(
                () -> {
                  RbacRole role = new RbacRole();
                  role.setCode("MERCHANT");
                  entityManager.persist(role);
                  return role;
                });
    PlatformUserRbacBinding binding = new PlatformUserRbacBinding();
    binding.setPlatformUser(user);
    binding.setRbacRole(merchantRole);
    binding.setMerchant(merchant);
    binding.setStatus(PlatformRbacBindingStatus.ACTIVE);
    entityManager.persist(binding);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, user.getUsername(), "secret-pass");
    mockMvc
        .perform(
            post("/api/auth/context/switch")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("role", "MERCHANT", "merchantId", merchant.getId()))))
        .andExpect(status().isForbidden());

    MerchantMembership membership = new MerchantMembership();
    membership.setMerchant(merchant);
    membership.setPlatformUser(user);
    membership.setMembershipStatus(MerchantMembershipStatus.ACTIVE);
    entityManager.persist(membership);
    entityManager.flush();

    mockMvc
        .perform(
            post("/api/auth/context/switch")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("role", "MERCHANT", "merchantId", merchant.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("MERCHANT"));
  }
}
