package com.bookingcore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookingcore.modules.booking.Booking;
import com.bookingcore.modules.booking.BookingRepository;
import com.bookingcore.modules.booking.BookingStatus;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.MerchantVisibility;
import com.bookingcore.modules.merchant.ResourceItem;
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
class ClientBookingCreateApiTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private BookingRepository bookingRepository;
  @PersistenceContext private EntityManager entityManager;

  @Test
  void createClientBooking_happyPath_returns201WithPending() throws Exception {
    Fixture fixture = createBookableFixture("ROOM", MerchantVisibility.PUBLIC);
    String clientToken = loginClient();

    LocalDateTime startAt = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
    mockMvc
        .perform(
            post("/api/client/bookings")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "resourceId", fixture.resource.getId(),
                            "startAt", startAt,
                            "notes", "book now"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.message").value("success"))
        .andExpect(jsonPath("$.data.id").isString())
        .andExpect(jsonPath("$.data.bookingNo").isString())
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andExpect(jsonPath("$.data.resourceId").value(fixture.resource.getId().toString()))
        .andExpect(jsonPath("$.data.tenantId").value(fixture.merchant.getId().toString()));
  }

  @Test
  void createClientBooking_conflict_returns409AndNoSideEffect() throws Exception {
    Fixture fixture = createBookableFixture("ROOM", MerchantVisibility.PUBLIC);
    String clientToken = loginClient();
    LocalDateTime startAt = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0).withSecond(0).withNano(0);

    Booking existing = new Booking();
    existing.setMerchant(fixture.merchant);
    existing.setServiceItem(fixture.serviceItem);
    existing.setStartAt(startAt);
    existing.setEndAt(startAt.plusMinutes(fixture.serviceItem.getDurationMinutes()));
    existing.setCustomerName("already");
    existing.setCustomerContact("0900");
    existing.setStatus(BookingStatus.PENDING);
    entityManager.persist(existing);
    entityManager.flush();

    long beforeCount = bookingRepository.count();
    mockMvc
        .perform(
            post("/api/client/bookings")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("resourceId", fixture.resource.getId(), "startAt", startAt))))
        .andExpect(status().isConflict());
    assertThat(bookingRepository.count()).isEqualTo(beforeCount);
  }

  @Test
  void createClientBooking_differentResourceSameMerchantSameTime_returns201() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Shared Merchant " + System.nanoTime());
    merchant.setSlug("shared-merchant-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setVisibility(MerchantVisibility.PUBLIC);
    entityManager.persist(merchant);

    ServiceItem serviceA = new ServiceItem();
    serviceA.setMerchant(merchant);
    serviceA.setName("Service A");
    serviceA.setCategory("GENERAL");
    serviceA.setDurationMinutes(30);
    serviceA.setPrice(BigDecimal.TEN);
    entityManager.persist(serviceA);

    ServiceItem serviceB = new ServiceItem();
    serviceB.setMerchant(merchant);
    serviceB.setName("Service B");
    serviceB.setCategory("GENERAL");
    serviceB.setDurationMinutes(30);
    serviceB.setPrice(BigDecimal.TEN);
    entityManager.persist(serviceB);
    entityManager.flush();

    ResourceItem resourceA = new ResourceItem();
    resourceA.setMerchant(merchant);
    resourceA.setName("Resource A");
    resourceA.setType("ROOM");
    resourceA.setCategory("GENERAL");
    resourceA.setCapacity(1);
    resourceA.setServiceItemsJson("[\"" + serviceA.getId() + "\"]");
    resourceA.setPrice(BigDecimal.TEN);
    resourceA.setActive(true);
    entityManager.persist(resourceA);

    ResourceItem resourceB = new ResourceItem();
    resourceB.setMerchant(merchant);
    resourceB.setName("Resource B");
    resourceB.setType("ROOM");
    resourceB.setCategory("GENERAL");
    resourceB.setCapacity(1);
    resourceB.setServiceItemsJson("[\"" + serviceB.getId() + "\"]");
    resourceB.setPrice(BigDecimal.TEN);
    resourceB.setActive(true);
    entityManager.persist(resourceB);
    entityManager.flush();

    LocalDateTime startAt = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0).withSecond(0).withNano(0);
    Booking existing = new Booking();
    existing.setMerchant(merchant);
    existing.setServiceItem(serviceA);
    existing.setStartAt(startAt);
    existing.setEndAt(startAt.plusMinutes(30));
    existing.setCustomerName("existing");
    existing.setCustomerContact("0900");
    existing.setStatus(BookingStatus.PENDING);
    entityManager.persist(existing);
    entityManager.flush();

    String clientToken = loginClient();
    mockMvc
        .perform(
            post("/api/client/bookings")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("resourceId", resourceB.getId(), "startAt", startAt))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andExpect(jsonPath("$.data.resourceId", is(resourceB.getId().toString())));
  }

  @Test
  void createClientBooking_strategyReject_returns422AndNoSideEffect() throws Exception {
    Fixture fixture = createBookableFixture("STRICT_HALF_HOUR", MerchantVisibility.PUBLIC);
    String clientToken = loginClient();
    LocalDateTime startAt = LocalDateTime.now().plusDays(1).withHour(11).withMinute(15).withSecond(0).withNano(0);

    long beforeCount = bookingRepository.count();
    mockMvc
        .perform(
            post("/api/client/bookings")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("resourceId", fixture.resource.getId(), "startAt", startAt))))
        .andExpect(status().isUnprocessableEntity());
    assertThat(bookingRepository.count()).isEqualTo(beforeCount);
  }

  @Test
  void createClientBooking_unregisteredStrategy_returns422() throws Exception {
    Fixture fixture = createBookableFixture("UNREGISTERED_TYPE", MerchantVisibility.PUBLIC);
    String clientToken = loginClient();
    LocalDateTime startAt = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0).withSecond(0).withNano(0);
    long beforeCount = bookingRepository.count();

    mockMvc
        .perform(
            post("/api/client/bookings")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("resourceId", fixture.resource.getId(), "startAt", startAt))))
        .andExpect(status().isUnprocessableEntity());
    assertThat(bookingRepository.count()).isEqualTo(beforeCount);
  }

  @Test
  void createClientBooking_resourceNotFound_returns404() throws Exception {
    String clientToken = loginClient();
    LocalDateTime startAt = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0).withSecond(0).withNano(0);

    mockMvc
        .perform(
            post("/api/client/bookings")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "resourceId",
                            UUID.fromString("01234567-89ab-7def-0123-456789abcdef"),
                            "startAt",
                            startAt))))
        .andExpect(status().isNotFound());
  }

  @Test
  void createClientBooking_crossTenantPrivateResource_returns404() throws Exception {
    Fixture fixture = createBookableFixture("ROOM", MerchantVisibility.INVITE_ONLY);
    String clientToken = loginClient();
    LocalDateTime startAt = LocalDateTime.now().plusDays(1).withHour(13).withMinute(0).withSecond(0).withNano(0);

    mockMvc
        .perform(
            post("/api/client/bookings")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("resourceId", fixture.resource.getId(), "startAt", startAt))))
        .andExpect(status().isNotFound());
  }

  @Test
  void createClientBooking_requiresClientRoleAndAuthentication() throws Exception {
    Fixture fixture = createBookableFixture("ROOM", MerchantVisibility.PUBLIC);
    LocalDateTime startAt = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0);
    String merchantToken = loginMerchant();

    mockMvc
        .perform(
            post("/api/client/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("resourceId", fixture.resource.getId(), "startAt", startAt))))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            post("/api/client/bookings")
                .header("Authorization", "Bearer " + merchantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("resourceId", fixture.resource.getId(), "startAt", startAt))))
        .andExpect(status().isForbidden());
  }

  @Test
  void createClientBooking_invalidPayload_returns400() throws Exception {
    Fixture fixture = createBookableFixture("ROOM", MerchantVisibility.PUBLIC);
    String clientToken = loginClient();
    String longNotes = "x".repeat(501);

    mockMvc
        .perform(
            post("/api/client/bookings")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "resourceId", fixture.resource.getId(),
                            "startAt", LocalDateTime.now().minusMinutes(1),
                            "notes", longNotes))))
        .andExpect(status().isBadRequest());
  }

  private Fixture createBookableFixture(String resourceType, MerchantVisibility visibility) {
    Merchant merchant = new Merchant();
    merchant.setName("Merchant " + System.nanoTime());
    merchant.setSlug("merchant-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setVisibility(visibility);
    entityManager.persist(merchant);

    ServiceItem serviceItem = new ServiceItem();
    serviceItem.setMerchant(merchant);
    serviceItem.setName("Service");
    serviceItem.setCategory("GENERAL");
    serviceItem.setDurationMinutes(30);
    serviceItem.setPrice(BigDecimal.TEN);
    entityManager.persist(serviceItem);
    entityManager.flush();

    ResourceItem resource = new ResourceItem();
    resource.setMerchant(merchant);
    resource.setName("Resource");
    resource.setType(resourceType);
    resource.setCategory("GENERAL");
    resource.setCapacity(1);
    resource.setServiceItemsJson("[\"" + serviceItem.getId() + "\"]");
    resource.setPrice(BigDecimal.TEN);
    resource.setActive(true);
    entityManager.persist(resource);
    entityManager.flush();

    return new Fixture(merchant, serviceItem, resource);
  }

  private String loginClient() throws Exception {
    PlatformUser user = new PlatformUser();
    user.setUsername("client-" + System.nanoTime() + "@example.com");
    user.setPasswordHash(passwordEncoder.encode("secret-pass"));
    user.setRole(PlatformUserRole.CLIENT);
    user.setEnabled(true);
    entityManager.persist(user);
    entityManager.flush();
    return TestJwtHelper.login(mockMvc, objectMapper, user.getUsername(), "secret-pass");
  }

  private String loginMerchant() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Merchant User " + System.nanoTime());
    merchant.setSlug("merchant-user-" + System.nanoTime());
    merchant.setActive(true);
    entityManager.persist(merchant);

    PlatformUser user = new PlatformUser();
    user.setUsername("merchant-" + System.nanoTime() + "@example.com");
    user.setPasswordHash(passwordEncoder.encode("secret-pass"));
    user.setRole(PlatformUserRole.MERCHANT);
    user.setMerchant(merchant);
    user.setEnabled(true);
    entityManager.persist(user);
    entityManager.flush();
    return TestJwtHelper.login(mockMvc, objectMapper, user.getUsername(), "secret-pass");
  }

  private record Fixture(Merchant merchant, ServiceItem serviceItem, ResourceItem resource) {}
}
