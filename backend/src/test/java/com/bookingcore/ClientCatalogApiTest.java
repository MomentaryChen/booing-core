package com.bookingcore;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.MerchantInvitation;
import com.bookingcore.modules.merchant.MerchantInvitationStatus;
import com.bookingcore.modules.merchant.MerchantMembership;
import com.bookingcore.modules.merchant.MerchantMembershipStatus;
import com.bookingcore.modules.merchant.MerchantVisibility;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.merchant.ResourceItem;
import com.bookingcore.modules.service.ServiceItem;
import com.bookingcore.security.PlatformUserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
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
class ClientCatalogApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordEncoder passwordEncoder;
  @PersistenceContext private EntityManager entityManager;

  @Test
  void listClientResources_anonymous_onlySeesPublicMerchantResources() throws Exception {
    Fixture fixture = createVisibilityFixture();

    mockMvc
        .perform(get("/api/client/resources").param("page", "0").param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[*].id", hasItem(fixture.publicResourceId.toString())))
        .andExpect(jsonPath("$.items[*].id", not(hasItem(fixture.privateResourceId.toString()))));
  }

  @Test
  void featuredResources_anonymous_excludesInviteOnlyMerchantResources() throws Exception {
    Fixture fixture = createVisibilityFixture();

    mockMvc
        .perform(get("/api/client/resources/featured").param("limit", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].id", hasItem(fixture.publicResourceId.toString())))
        .andExpect(jsonPath("$[*].id", not(hasItem(fixture.privateResourceId.toString()))));
  }

  @Test
  void categories_anonymous_excludesInviteOnlyMerchantCategories() throws Exception {
    createVisibilityFixture();

    mockMvc
        .perform(get("/api/client/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.key=='public-cat')]").exists())
        .andExpect(jsonPath("$[?(@.key=='private-cat')]").isEmpty());
  }

  @Test
  void listClientResources_authenticatedClientWithoutMembership_cannotSeeInviteOnly() throws Exception {
    Fixture fixture = createVisibilityFixture();
    String token = loginClientUser("catalog-client-non-member-" + System.nanoTime());

    mockMvc
        .perform(
            get("/api/client/resources")
                .header("Authorization", "Bearer " + token)
                .param("page", "0")
                .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[*].id", hasItem(fixture.publicResourceId.toString())))
        .andExpect(jsonPath("$.items[*].id", not(hasItem(fixture.privateResourceId.toString()))));
  }

  @Test
  void listClientResources_authenticatedClientWithMembership_canSeeInviteOnly() throws Exception {
    Fixture fixture = createVisibilityFixture();
    PlatformUser client = persistClientUser("catalog-client-member-" + System.nanoTime());
    MerchantMembership membership = new MerchantMembership();
    membership.setMerchant(fixture.privateMerchant);
    membership.setPlatformUser(client);
    membership.setMembershipStatus(MerchantMembershipStatus.ACTIVE);
    entityManager.persist(membership);
    entityManager.flush();
    String token = TestJwtHelper.login(mockMvc, objectMapper, client.getUsername(), "secret-pass");

    mockMvc
        .perform(
            get("/api/client/resources")
                .header("Authorization", "Bearer " + token)
                .param("page", "0")
                .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[*].id", hasItem(fixture.publicResourceId.toString())))
        .andExpect(jsonPath("$.items[*].id", hasItem(fixture.privateResourceId.toString())));
  }

  @Test
  void categories_authenticatedClientWithMembership_includesInviteOnlyCategory() throws Exception {
    Fixture fixture = createVisibilityFixture();
    PlatformUser client = persistClientUser("catalog-client-cat-" + System.nanoTime());
    MerchantMembership membership = new MerchantMembership();
    membership.setMerchant(fixture.privateMerchant);
    membership.setPlatformUser(client);
    membership.setMembershipStatus(MerchantMembershipStatus.ACTIVE);
    entityManager.persist(membership);
    entityManager.flush();
    String token = TestJwtHelper.login(mockMvc, objectMapper, client.getUsername(), "secret-pass");

    mockMvc
        .perform(get("/api/client/categories").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.key=='public-cat')]").exists())
        .andExpect(jsonPath("$[?(@.key=='private-cat')]").exists());
  }

  @Test
  void joinMerchantByCode_unauthenticated_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/client/merchant-memberships/join-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"inviteCode\":\"SOME-CODE\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void joinMerchantByCode_crossTenantInviteDenied_andNoForeignMerchantLeakedInJoinedList()
      throws Exception {
    PlatformUser invitedClient = persistClientUser("catalog-invite-client-" + System.nanoTime());
    String invitedClientToken =
        TestJwtHelper.login(mockMvc, objectMapper, invitedClient.getUsername(), "secret-pass");
    PlatformUser foreignClient = persistClientUser("catalog-foreign-client-" + System.nanoTime());
    String foreignClientToken =
        TestJwtHelper.login(mockMvc, objectMapper, foreignClient.getUsername(), "secret-pass");

    Merchant invitedMerchant = new Merchant();
    invitedMerchant.setName("Invited Merchant " + System.nanoTime());
    invitedMerchant.setSlug("invited-merchant-" + System.nanoTime());
    invitedMerchant.setActive(true);
    invitedMerchant.setVisibility(MerchantVisibility.INVITE_ONLY);
    entityManager.persist(invitedMerchant);

    MerchantInvitation invitation = new MerchantInvitation();
    invitation.setMerchant(invitedMerchant);
    invitation.setInviteeUser(invitedClient);
    invitation.setInviteCode("JOIN-" + System.nanoTime());
    invitation.setStatus(MerchantInvitationStatus.PENDING);
    invitation.setExpiresAt(LocalDateTime.now().plusDays(1));
    invitation.setCreatedBy("test");
    entityManager.persist(invitation);
    entityManager.flush();

    mockMvc
        .perform(
            post("/api/client/merchant-memberships/join-code")
                .header("Authorization", "Bearer " + foreignClientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"inviteCode\":\"" + invitation.getInviteCode() + "\"}"))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(get("/api/client/merchants/joined").header("Authorization", "Bearer " + foreignClientToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].merchantId", not(hasItem(invitedMerchant.getId().toString()))));

    mockMvc
        .perform(
            post("/api/client/merchant-memberships/join-code")
                .header("Authorization", "Bearer " + invitedClientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"inviteCode\":\"" + invitation.getInviteCode() + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.merchantId").value(invitedMerchant.getId().toString()))
        .andExpect(jsonPath("$.membershipStatus").value("ACTIVE"));

    mockMvc
        .perform(get("/api/client/merchants/joined").header("Authorization", "Bearer " + invitedClientToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].merchantId", hasItem(invitedMerchant.getId().toString())));
  }

  private Fixture createVisibilityFixture() {
    Merchant publicMerchant = new Merchant();
    publicMerchant.setName("Public Merchant " + System.nanoTime());
    publicMerchant.setSlug("public-merchant-" + System.nanoTime());
    publicMerchant.setActive(true);
    publicMerchant.setVisibility(MerchantVisibility.PUBLIC);
    entityManager.persist(publicMerchant);

    Merchant privateMerchant = new Merchant();
    privateMerchant.setName("Private Merchant " + System.nanoTime());
    privateMerchant.setSlug("private-merchant-" + System.nanoTime());
    privateMerchant.setActive(true);
    privateMerchant.setVisibility(MerchantVisibility.INVITE_ONLY);
    entityManager.persist(privateMerchant);

    ServiceItem publicService = new ServiceItem();
    publicService.setMerchant(publicMerchant);
    publicService.setName("Public Service");
    publicService.setCategory("public-cat");
    publicService.setDurationMinutes(30);
    publicService.setPrice(BigDecimal.TEN);
    entityManager.persist(publicService);

    ServiceItem privateService = new ServiceItem();
    privateService.setMerchant(privateMerchant);
    privateService.setName("Private Service");
    privateService.setCategory("private-cat");
    privateService.setDurationMinutes(30);
    privateService.setPrice(BigDecimal.TEN);
    entityManager.persist(privateService);
    entityManager.flush();

    ResourceItem publicResource = new ResourceItem();
    publicResource.setMerchant(publicMerchant);
    publicResource.setName("Public Resource");
    publicResource.setType("ROOM");
    publicResource.setCategory("public-cat");
    publicResource.setCapacity(1);
    publicResource.setServiceItemsJson("[\"" + publicService.getId() + "\"]");
    publicResource.setPrice(BigDecimal.TEN);
    publicResource.setActive(true);
    entityManager.persist(publicResource);

    ResourceItem privateResource = new ResourceItem();
    privateResource.setMerchant(privateMerchant);
    privateResource.setName("Private Resource");
    privateResource.setType("ROOM");
    privateResource.setCategory("private-cat");
    privateResource.setCapacity(1);
    privateResource.setServiceItemsJson("[\"" + privateService.getId() + "\"]");
    privateResource.setPrice(BigDecimal.TEN);
    privateResource.setActive(true);
    entityManager.persist(privateResource);
    entityManager.flush();

    return new Fixture(publicResource.getId(), privateResource.getId(), privateMerchant);
  }

  private String loginClientUser(String username) throws Exception {
    PlatformUser user = persistClientUser(username);
    return TestJwtHelper.login(mockMvc, objectMapper, user.getUsername(), "secret-pass");
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

  private record Fixture(java.util.UUID publicResourceId, java.util.UUID privateResourceId, Merchant privateMerchant) {}
}
