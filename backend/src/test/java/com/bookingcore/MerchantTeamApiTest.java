package com.bookingcore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.MerchantMembership;
import com.bookingcore.modules.merchant.MerchantMembershipStatus;
import com.bookingcore.modules.merchant.ServiceTeam;
import com.bookingcore.modules.merchant.ServiceTeamStatus;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.security.PlatformUserRole;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MerchantTeamApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordEncoder passwordEncoder;
  @PersistenceContext private EntityManager entityManager;

  @Test
  void createAndListTeamInOwnTenant() throws Exception {
    Merchant merchant = createMerchant("team-own");
    PlatformUser user = createMerchantUser("team-own-user", merchant);
    createActiveMembership(user, merchant);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, user.getUsername(), "secret-pass");

    mockMvc
        .perform(
            post("/api/merchant/" + merchant.getId() + "/teams")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Front Desk\",\"code\":\"FRONT_DESK\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.merchantId").value(merchant.getId()))
        .andExpect(jsonPath("$.name").value("Front Desk"))
        .andExpect(jsonPath("$.code").value("FRONT_DESK"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    mockMvc
        .perform(
            get("/api/merchant/" + merchant.getId() + "/teams")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].merchantId").value(merchant.getId()))
        .andExpect(jsonPath("$[0].code").value("FRONT_DESK"));
  }

  @Test
  void crossTenantTeamAccessDenied() throws Exception {
    Merchant merchantA = createMerchant("tenant-a");
    Merchant merchantB = createMerchant("tenant-b");
    PlatformUser userA = createMerchantUser("tenant-a-user", merchantA);
    createActiveMembership(userA, merchantA);

    ServiceTeam teamInB = new ServiceTeam();
    teamInB.setMerchant(merchantB);
    teamInB.setName("B Team");
    teamInB.setCode("B_TEAM");
    teamInB.setStatus(ServiceTeamStatus.ACTIVE);
    entityManager.persist(teamInB);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, userA.getUsername(), "secret-pass");

    mockMvc
        .perform(
            put("/api/merchant/" + merchantA.getId() + "/teams/" + teamInB.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Should Fail\",\"status\":\"ACTIVE\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void teamMemberAssignmentCrudBasicPath() throws Exception {
    Merchant merchant = createMerchant("member-crud");
    PlatformUser owner = createMerchantUser("member-owner", merchant);
    PlatformUser memberUser = createMerchantUser("member-user", merchant);
    createActiveMembership(owner, merchant);
    createActiveMembership(memberUser, merchant);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, owner.getUsername(), "secret-pass");

    String createTeamResponse =
        mockMvc
            .perform(
                post("/api/merchant/" + merchant.getId() + "/teams")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Operations\",\"code\":\"OPS\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Long teamId = objectMapper.readTree(createTeamResponse).path("id").asLong();

    MvcResult assignResult =
        mockMvc
            .perform(
                post("/api/merchant/" + merchant.getId() + "/teams/" + teamId + "/members")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"userId\":"
                            + memberUser.getId()
                            + ",\"role\":\"SCHEDULER\",\"status\":\"ACTIVE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.teamId").value(teamId))
            .andExpect(jsonPath("$.userId").value(memberUser.getId()))
            .andReturn();
    Long memberId =
        objectMapper.readTree(assignResult.getResponse().getContentAsString()).path("id").asLong();

    mockMvc
        .perform(
            get("/api/merchant/" + merchant.getId() + "/teams/" + teamId + "/members")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].userId").value(memberUser.getId()))
        .andExpect(jsonPath("$[0].role").value("SCHEDULER"));

    mockMvc
        .perform(
            delete("/api/merchant/" + merchant.getId() + "/teams/" + teamId + "/members/" + memberId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/merchant/" + merchant.getId() + "/teams/" + teamId + "/members")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void teamMemberAssignmentDeniedForUserWithoutActiveMembership() throws Exception {
    Merchant merchant = createMerchant("member-guard");
    PlatformUser owner = createMerchantUser("member-guard-owner", merchant);
    PlatformUser nonMember = createMerchantUser("member-guard-user", merchant);
    createActiveMembership(owner, merchant);
    // Intentionally do not create membership for nonMember.
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, owner.getUsername(), "secret-pass");

    String createTeamResponse =
        mockMvc
            .perform(
                post("/api/merchant/" + merchant.getId() + "/teams")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Guarded Team\",\"code\":\"GUARDED\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Long teamId = objectMapper.readTree(createTeamResponse).path("id").asLong();

    mockMvc
        .perform(
            post("/api/merchant/" + merchant.getId() + "/teams/" + teamId + "/members")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"userId\":"
                        + nonMember.getId()
                        + ",\"role\":\"SCHEDULER\",\"status\":\"ACTIVE\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void assignTeamMemberDeniedForCrossTenantOrInactiveMembership() throws Exception {
    Merchant merchantA = createMerchant("assign-a");
    Merchant merchantB = createMerchant("assign-b");
    PlatformUser ownerA = createMerchantUser("owner-a", merchantA);
    PlatformUser memberB = createMerchantUser("member-b", merchantB);
    PlatformUser memberNoMembership = createMerchantUser("member-no-membership", null);
    PlatformUser memberSuspended = createMerchantUser("member-suspended", merchantA);
    createActiveMembership(ownerA, merchantA);

    MerchantMembership suspendedMembership = new MerchantMembership();
    suspendedMembership.setMerchant(merchantA);
    suspendedMembership.setPlatformUser(memberSuspended);
    suspendedMembership.setMembershipStatus(MerchantMembershipStatus.SUSPENDED);
    entityManager.persist(suspendedMembership);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, ownerA.getUsername(), "secret-pass");
    String createTeamResponse =
        mockMvc
            .perform(
                post("/api/merchant/" + merchantA.getId() + "/teams")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"QA\",\"code\":\"QA\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Long teamId = objectMapper.readTree(createTeamResponse).path("id").asLong();

    mockMvc
        .perform(
            post("/api/merchant/" + merchantA.getId() + "/teams/" + teamId + "/members")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + memberB.getId() + ",\"role\":\"SCHEDULER\"}"))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/merchant/" + merchantA.getId() + "/teams/" + teamId + "/members")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + memberNoMembership.getId() + ",\"role\":\"SCHEDULER\"}"))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/merchant/" + merchantA.getId() + "/teams/" + teamId + "/members")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + memberSuspended.getId() + ",\"role\":\"SCHEDULER\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void merchantCallerWithoutActiveMembershipIsDeniedEvenWithLegacyMerchantBinding() throws Exception {
    Merchant merchant = createMerchant("caller-denied");
    PlatformUser caller = createMerchantUser("caller-denied", merchant);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, caller.getUsername(), "secret-pass");

    mockMvc
        .perform(
            post("/api/merchant/" + merchant.getId() + "/teams")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ops\",\"code\":\"OPS_DENIED\"}"))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            get("/api/merchant/" + merchant.getId() + "/profile")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
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
