package com.bookingcore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookingcore.modules.admin.AuditLogRepository;
import com.bookingcore.modules.booking.Booking;
import com.bookingcore.modules.booking.BookingStatus;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.MerchantMembership;
import com.bookingcore.modules.merchant.MerchantMembershipStatus;
import com.bookingcore.modules.merchant.ResourceItem;
import com.bookingcore.modules.merchant.ServiceTeam;
import com.bookingcore.modules.merchant.ServiceTeamStatus;
import com.bookingcore.modules.merchant.TeamMember;
import com.bookingcore.modules.merchant.TeamMemberStatus;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.service.ServiceItem;
import com.bookingcore.security.PlatformUserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class MerchantBookingAssignmentApiTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AuditLogRepository auditLogRepository;
  @PersistenceContext private EntityManager entityManager;

  @Test
  void assignReassignReleaseFlow_andAuditRecorded() throws Exception {
    MerchantAuthContext auth = createMerchantAuthContext();
    UUID staffA = createActiveMember(auth.merchantId(), "assign-a-" + System.nanoTime() + "@example.com");
    UUID staffB = createActiveMember(auth.merchantId(), "assign-b-" + System.nanoTime() + "@example.com");
    ResourceItem resource = createResource(auth.merchantId(), jsonUuidArray(staffA, staffB));
    Booking booking = createBooking(auth.merchantId(), auth.serviceId(), LocalDateTime.now().plusDays(1));
    String token = TestJwtHelper.login(mockMvc, objectMapper, auth.username(), auth.password());

    mockMvc
        .perform(
            post("/api/merchant/" + auth.merchantId() + "/bookings/" + booking.getId() + "/assign")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("resourceId", resource.getId(), "staffId", staffA))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bookingId").value(booking.getId().toString()))
        .andExpect(jsonPath("$.staffId").value(staffA.toString()))
        .andExpect(jsonPath("$.status").value("RESERVED"));

    mockMvc
        .perform(
            post("/api/merchant/" + auth.merchantId() + "/bookings/" + booking.getId() + "/reassign")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "resourceId",
                            resource.getId(),
                            "newStaffId",
                            staffB,
                            "reason",
                            "manual-adjust"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.staffId").value(staffB.toString()))
        .andExpect(jsonPath("$.reason").value("manual-adjust"));

    mockMvc
        .perform(
            post("/api/merchant/" + auth.merchantId() + "/bookings/" + booking.getId() + "/release")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("resourceId", resource.getId(), "reason", "cancelled-by-client"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RELEASED"));

    boolean hasReassignAudit =
        auditLogRepository.findTop100ByOrderByCreatedAtDesc().stream()
            .anyMatch(log -> "booking.assignment.reassign".equals(log.getAction()));
    org.junit.jupiter.api.Assertions.assertTrue(hasReassignAudit);
  }

  @Test
  void assignConflict_returns409() throws Exception {
    MerchantAuthContext auth = createMerchantAuthContext();
    UUID staffA = createActiveMember(auth.merchantId(), "conflict-" + System.nanoTime() + "@example.com");
    ResourceItem resource = createResource(auth.merchantId(), jsonUuidArray(staffA));
    LocalDateTime start = LocalDateTime.now().plusDays(2);
    Booking bookingA = createBooking(auth.merchantId(), auth.serviceId(), start);
    Booking bookingB = createBooking(auth.merchantId(), auth.serviceId(), start.plusMinutes(20));
    String token = TestJwtHelper.login(mockMvc, objectMapper, auth.username(), auth.password());

    mockMvc
        .perform(
            post("/api/merchant/" + auth.merchantId() + "/bookings/" + bookingA.getId() + "/assign")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("resourceId", resource.getId(), "staffId", staffA))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/merchant/" + auth.merchantId() + "/bookings/" + bookingB.getId() + "/assign")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("resourceId", resource.getId(), "staffId", staffA))))
        .andExpect(status().isConflict());
  }

  @Test
  void staffCandidates_andRoleBoundary() throws Exception {
    MerchantAuthContext auth = createMerchantAuthContext();
    UUID staffA = createActiveMember(auth.merchantId(), "candidate-" + System.nanoTime() + "@example.com");
    ResourceItem resource = createResource(auth.merchantId(), jsonUuidArray(staffA));
    LocalDateTime start = LocalDateTime.now().plusDays(3);
    Booking booking = createBooking(auth.merchantId(), auth.serviceId(), start);
    String merchantToken = TestJwtHelper.login(mockMvc, objectMapper, auth.username(), auth.password());

    mockMvc
        .perform(
            post("/api/merchant/" + auth.merchantId() + "/bookings/" + booking.getId() + "/assign")
                .header("Authorization", "Bearer " + merchantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("resourceId", resource.getId(), "staffId", staffA))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/merchant/" + auth.merchantId() + "/resources/" + resource.getId() + "/staff-candidates")
                .header("Authorization", "Bearer " + merchantToken)
                .param("startAt", start.plusMinutes(10).toString())
                .param("endAt", start.plusMinutes(40).toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].available").value(false))
        .andExpect(jsonPath("$[0].unavailableReason").value("STAFF_SLOT_CONFLICT"));

    PlatformUser client = createClientUser();
    String clientToken = TestJwtHelper.login(mockMvc, objectMapper, client.getUsername(), "secret-pass");
    mockMvc
        .perform(
            post("/api/merchant/" + auth.merchantId() + "/bookings/" + booking.getId() + "/assign")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("resourceId", resource.getId(), "staffId", staffA))))
        .andExpect(status().isForbidden());
  }

  private MerchantAuthContext createMerchantAuthContext() {
    Merchant merchant = new Merchant();
    merchant.setName("Assignment Merchant " + System.nanoTime());
    merchant.setSlug("assignment-" + System.nanoTime());
    merchant.setActive(true);
    entityManager.persist(merchant);

    ServiceItem service = new ServiceItem();
    service.setMerchant(merchant);
    service.setName("Massage");
    service.setCategory("WELLNESS");
    service.setDurationMinutes(60);
    service.setPrice(BigDecimal.valueOf(500));
    entityManager.persist(service);

    String username = "assignment-owner-" + System.nanoTime() + "@example.com";
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
    return new MerchantAuthContext(merchant.getId(), service.getId(), username, rawPassword);
  }

  private UUID createActiveMember(UUID merchantId, String username) {
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
    team.setName("Team " + System.nanoTime());
    team.setCode("team-" + System.nanoTime());
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

  private PlatformUser createClientUser() {
    PlatformUser user = new PlatformUser();
    user.setUsername("client-" + System.nanoTime() + "@example.com");
    user.setPasswordHash(passwordEncoder.encode("secret-pass"));
    user.setRole(PlatformUserRole.CLIENT);
    user.setEnabled(true);
    entityManager.persist(user);
    entityManager.flush();
    return user;
  }

  private ResourceItem createResource(UUID merchantId, String assignedStaffIdsJson) {
    Merchant merchant = entityManager.find(Merchant.class, merchantId);
    ResourceItem resource = new ResourceItem();
    resource.setMerchant(merchant);
    resource.setName("Room " + System.nanoTime());
    resource.setType("SERVICE");
    resource.setCategory("WELLNESS");
    resource.setCapacity(1);
    resource.setActive(true);
    resource.setServiceItemsJson("[]");
    resource.setAssignedStaffIdsJson(assignedStaffIdsJson);
    resource.setPrice(BigDecimal.valueOf(300));
    entityManager.persist(resource);
    entityManager.flush();
    return resource;
  }

  private Booking createBooking(UUID merchantId, UUID serviceId, LocalDateTime startAt) {
    Merchant merchant = entityManager.find(Merchant.class, merchantId);
    ServiceItem service = entityManager.find(ServiceItem.class, serviceId);
    Booking booking = new Booking();
    booking.setMerchant(merchant);
    booking.setServiceItem(service);
    booking.setStartAt(startAt);
    booking.setEndAt(startAt.plusMinutes(service.getDurationMinutes()));
    booking.setCustomerName("Test");
    booking.setCustomerContact("0900");
    booking.setStatus(BookingStatus.PENDING);
    entityManager.persist(booking);
    entityManager.flush();
    return booking;
  }

  private record MerchantAuthContext(UUID merchantId, UUID serviceId, String username, String password) {}

  private static String jsonUuidArray(UUID... ids) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < ids.length; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append('"').append(ids[i]).append('"');
    }
    sb.append(']');
    return sb.toString();
  }
}

