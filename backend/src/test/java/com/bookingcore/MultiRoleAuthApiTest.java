package com.bookingcore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.platform.rbac.PlatformRbacBindingStatus;
import com.bookingcore.modules.platform.rbac.PlatformUserRbacBinding;
import com.bookingcore.modules.platform.rbac.RbacPermission;
import com.bookingcore.modules.platform.rbac.RbacRole;
import com.bookingcore.security.PlatformUserRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
class MultiRoleAuthApiTest {

  @PersistenceContext private EntityManager entityManager;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void userWithMerchantAndClientBindings_seesBothRolesAndCanSelectClientContext() throws Exception {
    RbacPermission nav = new RbacPermission();
    nav.setCode("me.navigation.read");
    entityManager.persist(nav);

    RbacRole merchantRole = new RbacRole();
    merchantRole.setCode("MERCHANT");
    merchantRole.getPermissions().add(nav);
    entityManager.persist(merchantRole);

    RbacRole clientRole = new RbacRole();
    clientRole.setCode("CLIENT");
    clientRole.getPermissions().add(nav);
    entityManager.persist(clientRole);

    Merchant merchant = new Merchant();
    merchant.setName("Multi-role Merchant");
    merchant.setSlug("multi-role-m-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setServiceLimit(5);
    entityManager.persist(merchant);

    String username = "multi-role-user-" + System.nanoTime();
    String password = "secret-pass";
    PlatformUser user = new PlatformUser();
    user.setUsername(username);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setRole(PlatformUserRole.MERCHANT);
    user.setMerchant(merchant);
    user.setEnabled(true);
    entityManager.persist(user);

    PlatformUserRbacBinding merchantBinding = new PlatformUserRbacBinding();
    merchantBinding.setPlatformUser(user);
    merchantBinding.setRbacRole(merchantRole);
    merchantBinding.setMerchant(merchant);
    merchantBinding.setStatus(PlatformRbacBindingStatus.ACTIVE);
    entityManager.persist(merchantBinding);

    PlatformUserRbacBinding clientBinding = new PlatformUserRbacBinding();
    clientBinding.setPlatformUser(user);
    clientBinding.setRbacRole(clientRole);
    clientBinding.setMerchant(null);
    clientBinding.setStatus(PlatformRbacBindingStatus.ACTIVE);
    entityManager.persist(clientBinding);

    entityManager.flush();

    String loginJson =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"username\":\""
                            + username
                            + "\",\"password\":\""
                            + password
                            + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("MERCHANT"))
            .andExpect(jsonPath("$.canonicalRole").value("MERCHANT_OWNER"))
            .andExpect(jsonPath("$.roleAliases").isArray())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode loginNode = objectMapper.readTree(loginJson);
    assertThat(loginNode.get("roles")).hasSize(2);
    String token = loginNode.get("accessToken").asText();

    String meJson =
        mockMvc
            .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("MERCHANT"))
            .andExpect(jsonPath("$.canonicalRole").value("MERCHANT_OWNER"))
            .andExpect(jsonPath("$.roles.length()").value(2))
            .andExpect(jsonPath("$.canonicalRoles.length()").value(2))
            .andExpect(jsonPath("$.roleAliases").isArray())
            .andExpect(jsonPath("$.availableContexts.length()").value(2))
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode meNode = objectMapper.readTree(meJson);
    assertThat(meNode.get("roles").toString()).contains("CLIENT").contains("MERCHANT");

    mockMvc
        .perform(
            post("/api/auth/context/select")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"CLIENT_USER\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("CLIENT"))
        .andExpect(jsonPath("$.canonicalRole").value("CLIENT_USER"))
        .andExpect(jsonPath("$.roles.length()").value(2));
  }
}
