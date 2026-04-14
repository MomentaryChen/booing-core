package com.bookingcore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.MerchantMembership;
import com.bookingcore.modules.merchant.MerchantMembershipStatus;
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
class MerchantProfileLogoStorageApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordEncoder passwordEncoder;
  @PersistenceContext private EntityManager entityManager;

  @Test
  void putProfile_acceptsDataImageUrlAndReturnsSamePayload() throws Exception {
    Merchant merchant = createMerchant("logo-data");
    PlatformUser owner = createMerchantUser("logo-owner", merchant);
    createActiveMembership(owner, merchant);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, owner.getUsername(), "secret-pass");
    String logoData = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB";

    mockMvc
        .perform(
            put("/api/merchant/" + merchant.getId() + "/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "description", "logo in db",
                            "logoUrl", logoData,
                            "address", "",
                            "phone", "",
                            "email", "",
                            "website", ""))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.logoUrl").value(logoData));

    mockMvc
        .perform(
            get("/api/merchant/" + merchant.getId() + "/profile")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.logoUrl").value(logoData));
  }

  @Test
  void putProfile_rejectsNonDataImageUrl() throws Exception {
    Merchant merchant = createMerchant("logo-http");
    PlatformUser owner = createMerchantUser("logo-http-owner", merchant);
    createActiveMembership(owner, merchant);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, owner.getUsername(), "secret-pass");

    mockMvc
        .perform(
            put("/api/merchant/" + merchant.getId() + "/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "description", "bad logo",
                            "logoUrl", "https://example.com/logo.png",
                            "address", "",
                            "phone", "",
                            "email", "",
                            "website", ""))))
        .andExpect(status().isBadRequest());
  }

  private Merchant createMerchant(String key) {
    Merchant merchant = new Merchant();
    merchant.setName("Merchant " + key + "-" + System.nanoTime());
    merchant.setSlug("merchant-" + key + "-" + System.nanoTime());
    merchant.setActive(true);
    entityManager.persist(merchant);
    return merchant;
  }

  private PlatformUser createMerchantUser(String key, Merchant merchant) {
    PlatformUser user = new PlatformUser();
    user.setUsername(key + "-" + System.nanoTime());
    user.setPasswordHash(passwordEncoder.encode("secret-pass"));
    user.setRole(PlatformUserRole.MERCHANT);
    user.setMerchant(merchant);
    user.setEnabled(true);
    entityManager.persist(user);
    return user;
  }

  private void createActiveMembership(PlatformUser user, Merchant merchant) {
    MerchantMembership membership = new MerchantMembership();
    membership.setMerchant(merchant);
    membership.setPlatformUser(user);
    membership.setMembershipStatus(MerchantMembershipStatus.ACTIVE);
    entityManager.persist(membership);
  }
}
