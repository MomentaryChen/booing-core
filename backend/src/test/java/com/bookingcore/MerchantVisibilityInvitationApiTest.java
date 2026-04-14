package com.bookingcore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.MerchantInvitation;
import com.bookingcore.modules.merchant.MerchantInvitationStatus;
import com.bookingcore.modules.merchant.MerchantMembership;
import com.bookingcore.modules.merchant.MerchantMembershipStatus;
import com.bookingcore.modules.merchant.MerchantVisibility;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.security.PlatformUserRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
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
class MerchantVisibilityInvitationApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordEncoder passwordEncoder;
  @PersistenceContext private EntityManager entityManager;

  @Test
  void clientCanJoinInviteOnlyMerchantByInviteCode() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Invite Only Merchant " + System.nanoTime());
    merchant.setSlug("invite-only-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setVisibility(MerchantVisibility.INVITE_ONLY);
    entityManager.persist(merchant);

    String username = "client-join-" + System.nanoTime();
    String password = "secret-pass";
    PlatformUser client = new PlatformUser();
    client.setUsername(username);
    client.setPasswordHash(passwordEncoder.encode(password));
    client.setRole(PlatformUserRole.CLIENT);
    client.setEnabled(true);
    entityManager.persist(client);

    MerchantInvitation invitation = new MerchantInvitation();
    invitation.setMerchant(merchant);
    invitation.setInviteeUser(client);
    invitation.setInviteCode("JOINCODE" + System.nanoTime());
    invitation.setStatus(MerchantInvitationStatus.PENDING);
    invitation.setExpiresAt(LocalDateTime.now().plusDays(1));
    invitation.setCreatedBy("test");
    entityManager.persist(invitation);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, username, password);

    String merchantsJson =
        mockMvc
            .perform(get("/api/client/merchants").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode cards = objectMapper.readTree(merchantsJson);
    boolean containsInviteOnly = false;
    for (JsonNode card : cards) {
      if (card.get("merchantId").asLong() == merchant.getId()) {
        containsInviteOnly = "INVITE_ONLY".equals(card.get("visibility").asText());
        break;
      }
    }
    org.assertj.core.api.Assertions.assertThat(containsInviteOnly).isTrue();

    mockMvc
        .perform(
            post("/api/client/merchant-memberships/join-code")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"inviteCode\":\"" + invitation.getInviteCode() + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.merchantId").value(merchant.getId()))
        .andExpect(jsonPath("$.membershipStatus").value("ACTIVE"));

    mockMvc
        .perform(get("/api/client/merchants/joined").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].merchantId").value(merchant.getId()));
  }

  @Test
  void anonymousCannotCheckAvailabilityOrBookInviteOnlyMerchant() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Invite Only Merchant A " + System.nanoTime());
    merchant.setSlug("invite-only-a-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setVisibility(MerchantVisibility.INVITE_ONLY);
    entityManager.persist(merchant);

    com.bookingcore.modules.service.ServiceItem service = new com.bookingcore.modules.service.ServiceItem();
    service.setMerchant(merchant);
    service.setName("General");
    service.setCategory("GENERAL");
    service.setDurationMinutes(30);
    service.setPrice(java.math.BigDecimal.TEN);
    entityManager.persist(service);
    entityManager.flush();

    mockMvc
        .perform(
            get("/api/client/availability")
                .param("merchantId", String.valueOf(merchant.getId()))
                .param("serviceItemId", String.valueOf(service.getId()))
                .param("date", java.time.LocalDate.now().plusDays(1).toString()))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/client/public/" + merchant.getSlug() + "/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"serviceItemId\":"
                        + service.getId()
                        + ",\"startAt\":\""
                        + java.time.LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0)
                        + "\",\"customerName\":\"Anon\",\"customerContact\":\"0900\"}"))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/client/booking/lock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"merchantId\":"
                        + merchant.getId()
                        + ",\"serviceItemId\":"
                        + service.getId()
                        + ",\"startAt\":\""
                        + java.time.LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0)
                        + "\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void merchantCannotMarkInvitationAcceptedDirectly() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Invite Only Merchant B " + System.nanoTime());
    merchant.setSlug("invite-only-b-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setVisibility(MerchantVisibility.INVITE_ONLY);
    entityManager.persist(merchant);

    String merchantUsername = "merchant-owner-" + System.nanoTime();
    PlatformUser merchantUser = new PlatformUser();
    merchantUser.setUsername(merchantUsername);
    merchantUser.setPasswordHash(passwordEncoder.encode("secret-pass"));
    merchantUser.setRole(PlatformUserRole.MERCHANT);
    merchantUser.setMerchant(merchant);
    merchantUser.setEnabled(true);
    entityManager.persist(merchantUser);

    MerchantMembership ownerMembership = new MerchantMembership();
    ownerMembership.setMerchant(merchant);
    ownerMembership.setPlatformUser(merchantUser);
    ownerMembership.setMembershipStatus(MerchantMembershipStatus.ACTIVE);
    entityManager.persist(ownerMembership);

    PlatformUser invitee = new PlatformUser();
    invitee.setUsername("invitee-" + System.nanoTime());
    invitee.setPasswordHash(passwordEncoder.encode("secret-pass"));
    invitee.setRole(PlatformUserRole.CLIENT);
    invitee.setEnabled(true);
    entityManager.persist(invitee);

    MerchantInvitation invitation = new MerchantInvitation();
    invitation.setMerchant(merchant);
    invitation.setInviteeUser(invitee);
    invitation.setInviteCode("INV" + System.nanoTime());
    invitation.setStatus(MerchantInvitationStatus.PENDING);
    invitation.setCreatedBy("test");
    entityManager.persist(invitation);
    entityManager.flush();

    String merchantToken = TestJwtHelper.login(mockMvc, objectMapper, merchantUsername, "secret-pass");
    mockMvc
        .perform(
            patch("/api/merchant/" + merchant.getId() + "/invitations/" + invitation.getId())
                .header("Authorization", "Bearer " + merchantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACCEPTED\"}"))
        .andExpect(status().isConflict());
  }
}
