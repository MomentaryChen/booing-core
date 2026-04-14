package com.bookingcore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.MerchantMembership;
import com.bookingcore.modules.merchant.MerchantMembershipStatus;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.security.PlatformUserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MerchantTenantIsolationGuardApiTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordEncoder passwordEncoder;
  @PersistenceContext private EntityManager entityManager;

  @Test
  void merchantEndpointDeniedWhenMembershipMissingEvenWithMerchantToken() throws Exception {
    Merchant homeMerchant = new Merchant();
    homeMerchant.setName("Home Merchant " + System.nanoTime());
    homeMerchant.setSlug("home-merchant-" + System.nanoTime());
    homeMerchant.setActive(true);
    entityManager.persist(homeMerchant);

    Merchant merchant = new Merchant();
    merchant.setName("Isolation Merchant " + System.nanoTime());
    merchant.setSlug("isolation-merchant-" + System.nanoTime());
    merchant.setActive(true);
    entityManager.persist(merchant);

    PlatformUser user = new PlatformUser();
    user.setUsername("iso-user-" + System.nanoTime());
    user.setPasswordHash(passwordEncoder.encode("secret-pass"));
    user.setRole(PlatformUserRole.MERCHANT);
    user.setMerchant(homeMerchant);
    user.setEnabled(true);
    entityManager.persist(user);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, user.getUsername(), "secret-pass");
    mockMvc
        .perform(
            get("/api/merchant/" + merchant.getId() + "/profile")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void merchantEndpointDeniedWhenMembershipSuspendedEvenIfLegacyMerchantMatches() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Suspended Merchant " + System.nanoTime());
    merchant.setSlug("suspended-merchant-" + System.nanoTime());
    merchant.setActive(true);
    entityManager.persist(merchant);

    PlatformUser user = new PlatformUser();
    user.setUsername("suspended-user-" + System.nanoTime());
    user.setPasswordHash(passwordEncoder.encode("secret-pass"));
    user.setRole(PlatformUserRole.MERCHANT);
    user.setMerchant(merchant);
    user.setEnabled(true);
    entityManager.persist(user);

    MerchantMembership membership = new MerchantMembership();
    membership.setMerchant(merchant);
    membership.setPlatformUser(user);
    membership.setMembershipStatus(MerchantMembershipStatus.SUSPENDED);
    entityManager.persist(membership);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, user.getUsername(), "secret-pass");
    mockMvc
        .perform(
            get("/api/merchant/" + merchant.getId() + "/profile")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }
}
