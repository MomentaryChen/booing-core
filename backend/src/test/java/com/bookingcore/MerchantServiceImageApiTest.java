package com.bookingcore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class MerchantServiceImageApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordEncoder passwordEncoder;
  @PersistenceContext private EntityManager entityManager;

  @Test
  void createService_acceptsDataImageUrlAndListReturnsIt() throws Exception {
    Merchant merchant = createMerchant("service-image");
    PlatformUser owner = createMerchantUser("service-owner", merchant);
    createActiveMembership(owner, merchant);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, owner.getUsername(), "secret-pass");
    String imageData = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD";

    mockMvc
        .perform(
            post("/api/merchant/" + merchant.getId() + "/services")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "name", "Hair Cut",
                            "durationMinutes", 30,
                            "price", 500,
                            "category", "beauty",
                            "imageUrl", imageData))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.imageUrl").value(imageData));

    mockMvc
        .perform(
            get("/api/merchant/" + merchant.getId() + "/services")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].imageUrl").value(imageData));
  }

  @Test
  void createService_rejectsExternalUrlImage() throws Exception {
    Merchant merchant = createMerchant("service-image-invalid");
    PlatformUser owner = createMerchantUser("service-owner-invalid", merchant);
    createActiveMembership(owner, merchant);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, owner.getUsername(), "secret-pass");

    mockMvc
        .perform(
            post("/api/merchant/" + merchant.getId() + "/services")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "name", "Nail Care",
                            "durationMinutes", 30,
                            "price", 400,
                            "category", "beauty",
                            "imageUrl", "https://example.com/service.png"))))
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
