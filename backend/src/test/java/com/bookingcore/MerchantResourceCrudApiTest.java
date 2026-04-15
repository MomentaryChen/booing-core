package com.bookingcore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookingcore.modules.booking.Booking;
import com.bookingcore.modules.booking.BookingStatus;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.MerchantMembership;
import com.bookingcore.modules.merchant.MerchantMembershipStatus;
import com.bookingcore.modules.merchant.ServiceTeam;
import com.bookingcore.modules.merchant.ServiceTeamStatus;
import com.bookingcore.modules.merchant.TeamMember;
import com.bookingcore.modules.merchant.TeamMemberStatus;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.security.PlatformUserRole;
import com.bookingcore.modules.service.ServiceItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
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
class MerchantResourceCrudApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordEncoder passwordEncoder;
  @PersistenceContext private EntityManager entityManager;

  @Test
  void merchantCanUpdateResourceByPatch() throws Exception {
    MerchantAuthContext auth = createMerchantAuthContext();
    String merchantToken = TestJwtHelper.login(mockMvc, objectMapper, auth.username(), auth.password());
    UUID merchantId = auth.merchantId();

    String createdJson =
        mockMvc
            .perform(
                post("/api/merchant/" + merchantId + "/resources")
                    .header("Authorization", "Bearer " + merchantToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Room A",
                          "type": "SERVICE",
                          "category": "WELLNESS",
                          "capacity": 1,
                          "active": true,
                          "serviceItemsJson": "[]",
                          "price": 300
                        }
                        """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID resourceId = UUID.fromString(objectMapper.readTree(createdJson).path("id").asText());

    mockMvc
        .perform(
            patch("/api/merchant/" + merchantId + "/resources/" + resourceId)
                .header("Authorization", "Bearer " + merchantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Room A Prime",
                      "price": 450,
                      "capacity": 2,
                      "active": false
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Room A Prime"))
        .andExpect(jsonPath("$.price").value(450))
        .andExpect(jsonPath("$.capacity").value(2))
        .andExpect(jsonPath("$.active").value(false));
  }

  @Test
  void deleteResourceReturnsConflictWhenUnfinishedBookingExists() throws Exception {
    MerchantAuthContext auth = createMerchantAuthContext();
    String merchantToken = TestJwtHelper.login(mockMvc, objectMapper, auth.username(), auth.password());
    UUID merchantId = auth.merchantId();

    String serviceJson =
        mockMvc
            .perform(
                post("/api/merchant/" + merchantId + "/services")
                    .header("Authorization", "Bearer " + merchantToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Conflict Service",
                          "durationMinutes": 60,
                          "price": 500,
                          "category": "WELLNESS"
                        }
                        """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID serviceId = UUID.fromString(objectMapper.readTree(serviceJson).path("id").asText());

    String resourceJson =
        mockMvc
            .perform(
                post("/api/merchant/" + merchantId + "/resources")
                    .header("Authorization", "Bearer " + merchantToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Shared Room",
                          "type": "SERVICE",
                          "category": "WELLNESS",
                          "capacity": 1,
                          "active": true,
                          "serviceItemsJson": "[\\"%s\\"]",
                          "price": 500
                        }
                        """
                            .formatted(serviceId)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID resourceId = UUID.fromString(objectMapper.readTree(resourceJson).path("id").asText());

    Merchant merchant = entityManager.find(Merchant.class, merchantId);
    ServiceItem serviceItem = entityManager.find(ServiceItem.class, serviceId);
    Booking booking = new Booking();
    booking.setMerchant(merchant);
    booking.setServiceItem(serviceItem);
    booking.setStartAt(LocalDateTime.now().plusDays(1));
    booking.setEndAt(LocalDateTime.now().plusDays(1).plusHours(1));
    booking.setCustomerName("Test User");
    booking.setCustomerContact("0900");
    booking.setStatus(BookingStatus.PENDING);
    entityManager.persist(booking);
    entityManager.flush();

    mockMvc
        .perform(
            delete("/api/merchant/" + merchantId + "/resources/" + resourceId)
                .header("Authorization", "Bearer " + merchantToken))
        .andExpect(status().isConflict());
  }

  @Test
  void merchantCanAssignActiveStaffIdsToResource() throws Exception {
    MerchantAuthContext auth = createMerchantAuthContext();
    String merchantToken = TestJwtHelper.login(mockMvc, objectMapper, auth.username(), auth.password());
    UUID merchantId = auth.merchantId();
    UUID staffUserId = createActiveTeamMember(merchantId, "staff-active-" + System.nanoTime() + "@example.com");

    mockMvc
        .perform(
            post("/api/merchant/" + merchantId + "/resources")
                .header("Authorization", "Bearer " + merchantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Staff Assign Resource",
                      "type": "SERVICE",
                      "category": "WELLNESS",
                      "capacity": 1,
                      "active": true,
                      "serviceItemsJson": "[]",
                      "assignedStaffIds": ["%s"],
                      "price": 300
                    }
                    """
                        .formatted(staffUserId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.assignedStaffIds[0]").value(staffUserId.toString()));
  }

  @Test
  void assignStaffIdsRejectsNonMemberOrInactiveUser() throws Exception {
    MerchantAuthContext auth = createMerchantAuthContext();
    String merchantToken = TestJwtHelper.login(mockMvc, objectMapper, auth.username(), auth.password());
    UUID merchantId = auth.merchantId();

    PlatformUser outsider = new PlatformUser();
    outsider.setUsername("outsider-" + System.nanoTime() + "@example.com");
    outsider.setPasswordHash(passwordEncoder.encode("secret-pass"));
    outsider.setRole(PlatformUserRole.MERCHANT);
    outsider.setEnabled(true);
    entityManager.persist(outsider);
    entityManager.flush();

    mockMvc
        .perform(
            post("/api/merchant/" + merchantId + "/resources")
                .header("Authorization", "Bearer " + merchantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Invalid Assign Resource",
                      "type": "SERVICE",
                      "category": "WELLNESS",
                      "capacity": 1,
                      "active": true,
                      "serviceItemsJson": "[]",
                      "assignedStaffIds": ["%s"],
                      "price": 300
                    }
                    """
                        .formatted(outsider.getId().toString())))
        .andExpect(status().isBadRequest());
  }

  private MerchantAuthContext createMerchantAuthContext() {
    Merchant merchant = new Merchant();
    merchant.setName("Resource CRUD Merchant " + System.nanoTime());
    merchant.setSlug("resource-crud-" + System.nanoTime());
    merchant.setActive(true);
    entityManager.persist(merchant);

    String username = "merchant-" + System.nanoTime() + "@example.com";
    String rawPassword = "secret-pass";
    PlatformUser user = new PlatformUser();
    user.setUsername(username);
    user.setPasswordHash(passwordEncoder.encode(rawPassword));
    user.setRole(PlatformUserRole.MERCHANT);
    user.setMerchant(merchant);
    user.setEnabled(true);
    entityManager.persist(user);

    MerchantMembership membership = new MerchantMembership();
    membership.setMerchant(merchant);
    membership.setPlatformUser(user);
    membership.setMembershipStatus(MerchantMembershipStatus.ACTIVE);
    entityManager.persist(membership);
    entityManager.flush();

    return new MerchantAuthContext(merchant.getId(), username, rawPassword);
  }

  private UUID createActiveTeamMember(UUID merchantId, String username) {
    Merchant merchant = entityManager.find(Merchant.class, merchantId);
    PlatformUser user = new PlatformUser();
    user.setUsername(username);
    user.setPasswordHash(passwordEncoder.encode("secret-pass"));
    user.setRole(PlatformUserRole.MERCHANT);
    user.setMerchant(merchant);
    user.setEnabled(true);
    entityManager.persist(user);

    MerchantMembership membership = new MerchantMembership();
    membership.setMerchant(merchant);
    membership.setPlatformUser(user);
    membership.setMembershipStatus(MerchantMembershipStatus.ACTIVE);
    entityManager.persist(membership);

    ServiceTeam team = new ServiceTeam();
    team.setMerchant(merchant);
    team.setName("Ops Team " + System.nanoTime());
    team.setCode("ops-" + System.nanoTime());
    team.setStatus(ServiceTeamStatus.ACTIVE);
    entityManager.persist(team);

    TeamMember teamMember = new TeamMember();
    teamMember.setMerchant(merchant);
    teamMember.setTeam(team);
    teamMember.setPlatformUser(user);
    teamMember.setRole("STAFF");
    teamMember.setStatus(TeamMemberStatus.ACTIVE);
    entityManager.persist(teamMember);
    entityManager.flush();
    return user.getId();
  }

  private record MerchantAuthContext(UUID merchantId, String username, String password) {}
}
