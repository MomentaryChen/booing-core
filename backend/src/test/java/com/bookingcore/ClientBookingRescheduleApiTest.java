package com.bookingcore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class ClientBookingRescheduleApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private BookingRepository bookingRepository;
  @PersistenceContext private EntityManager entityManager;

  @Test
  void rescheduleOwnBooking_happyPath_returns200AndUpdatesTime() throws Exception {
    Fixture fixture = createBookableFixture("ROOM", MerchantVisibility.PUBLIC);
    String clientToken = loginClient();

    LocalDateTime originalStart = normalizedFutureTime(1, 10, 0);
    UUID bookingId = createBooking(clientToken, fixture.resource.getId(), originalStart);
    LocalDateTime newStart = normalizedFutureTime(1, 12, 0);

    mockMvc
        .perform(
            patch("/api/client/bookings/" + bookingId + "/reschedule")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("newStartAt", newStart, "reason", "need later time"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.id").value(bookingId.toString()))
        .andExpect(jsonPath("$.data.status").value("PENDING"));

    Booking saved = bookingRepository.findById(bookingId).orElseThrow();
    assertThat(saved.getStartAt()).isEqualTo(newStart);
    assertThat(saved.getEndAt()).isEqualTo(newStart.plusMinutes(fixture.serviceItem.getDurationMinutes()));
    assertThat(saved.getCustomerContact()).contains("rescheduleReason=need later time");
  }

  @Test
  void rescheduleBooking_requiresAuthenticationAndClientRole() throws Exception {
    Fixture fixture = createBookableFixture("ROOM", MerchantVisibility.PUBLIC);
    String clientToken = loginClient();
    String merchantToken = loginMerchant();
    UUID bookingId = createBooking(clientToken, fixture.resource.getId(), normalizedFutureTime(1, 9, 0));

    LocalDateTime target = normalizedFutureTime(1, 13, 0);
    String body = objectMapper.writeValueAsString(Map.of("newStartAt", target, "reason", "auth test"));

    mockMvc
        .perform(
            patch("/api/client/bookings/" + bookingId + "/reschedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            patch("/api/client/bookings/" + bookingId + "/reschedule")
                .header("Authorization", "Bearer " + merchantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden());
  }

  @Test
  void rescheduleBooking_notOwner_returns404() throws Exception {
    Fixture fixture = createBookableFixture("ROOM", MerchantVisibility.PUBLIC);
    String ownerToken = loginClient();
    String otherClientToken = loginClient();
    UUID bookingId = createBooking(ownerToken, fixture.resource.getId(), normalizedFutureTime(1, 11, 0));

    mockMvc
        .perform(
            patch("/api/client/bookings/" + bookingId + "/reschedule")
                .header("Authorization", "Bearer " + otherClientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("newStartAt", normalizedFutureTime(1, 15, 0), "reason", "not owner"))))
        .andExpect(status().isNotFound());
  }

  @Test
  void rescheduleBooking_conflict_returns409() throws Exception {
    Fixture fixture = createBookableFixture("ROOM", MerchantVisibility.PUBLIC);
    String clientToken = loginClient();
    LocalDateTime firstStart = normalizedFutureTime(2, 9, 0);
    LocalDateTime secondStart = normalizedFutureTime(2, 10, 0);
    UUID firstBookingId = createBooking(clientToken, fixture.resource.getId(), firstStart);
    UUID secondBookingId = createBooking(clientToken, fixture.resource.getId(), secondStart);

    mockMvc
        .perform(
            patch("/api/client/bookings/" + secondBookingId + "/reschedule")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("newStartAt", firstStart.plusMinutes(10), "reason", "overlap"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.data.errorCode").value("BOOKING_SLOT_CONFLICT"));

    Booking unchanged = bookingRepository.findById(secondBookingId).orElseThrow();
    assertThat(unchanged.getStartAt()).isEqualTo(secondStart);
    assertThat(unchanged.getEndAt()).isEqualTo(secondStart.plusMinutes(fixture.serviceItem.getDurationMinutes()));
    assertThat(firstBookingId).isNotNull();
  }

  @Test
  void rescheduleBooking_stateLegality_cancelledReturns409_completedReturns422() throws Exception {
    Fixture fixture = createBookableFixture("ROOM", MerchantVisibility.PUBLIC);
    String clientToken = loginClient();
    LocalDateTime start = normalizedFutureTime(3, 9, 0);

    UUID cancelledBookingId = createBooking(clientToken, fixture.resource.getId(), start);
    Booking cancelled = bookingRepository.findById(cancelledBookingId).orElseThrow();
    cancelled.setStatus(BookingStatus.CANCELLED);
    entityManager.flush();

    mockMvc
        .perform(
            patch("/api/client/bookings/" + cancelledBookingId + "/reschedule")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("newStartAt", normalizedFutureTime(3, 11, 0), "reason", "cancelled"))))
        .andExpect(status().isConflict());

    UUID completedBookingId = createBooking(clientToken, fixture.resource.getId(), normalizedFutureTime(3, 12, 0));
    Booking completed = bookingRepository.findById(completedBookingId).orElseThrow();
    completed.setStatus(BookingStatus.COMPLETED);
    entityManager.flush();

    mockMvc
        .perform(
            patch("/api/client/bookings/" + completedBookingId + "/reschedule")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("newStartAt", normalizedFutureTime(3, 13, 0), "reason", "completed"))))
        .andExpect(status().isUnprocessableEntity());
  }

  private UUID createBooking(String token, UUID resourceId, LocalDateTime startAt) throws Exception {
    String createdJson =
        mockMvc
            .perform(
                post("/api/client/bookings")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("resourceId", resourceId, "startAt", startAt))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(objectMapper.readTree(createdJson).path("data").path("id").asText());
  }

  private Fixture createBookableFixture(String resourceType, MerchantVisibility visibility) {
    Merchant merchant = new Merchant();
    merchant.setName("Reschedule Merchant " + System.nanoTime());
    merchant.setSlug("reschedule-merchant-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setVisibility(visibility);
    entityManager.persist(merchant);

    ServiceItem serviceItem = new ServiceItem();
    serviceItem.setMerchant(merchant);
    serviceItem.setName("Reschedule Service");
    serviceItem.setCategory("GENERAL");
    serviceItem.setDurationMinutes(30);
    serviceItem.setPrice(BigDecimal.TEN);
    entityManager.persist(serviceItem);
    entityManager.flush();

    ResourceItem resource = new ResourceItem();
    resource.setMerchant(merchant);
    resource.setName("Reschedule Resource");
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

  private LocalDateTime normalizedFutureTime(int plusDays, int hour, int minute) {
    return LocalDateTime.now()
        .plusDays(plusDays)
        .withHour(hour)
        .withMinute(minute)
        .withSecond(0)
        .withNano(0);
  }

  private String loginClient() throws Exception {
    PlatformUser user = new PlatformUser();
    user.setUsername("reschedule-client-" + System.nanoTime() + "@example.com");
    user.setPasswordHash(passwordEncoder.encode("secret-pass"));
    user.setRole(PlatformUserRole.CLIENT);
    user.setEnabled(true);
    entityManager.persist(user);
    entityManager.flush();
    return TestJwtHelper.login(mockMvc, objectMapper, user.getUsername(), "secret-pass");
  }

  private String loginMerchant() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Reschedule Merchant User " + System.nanoTime());
    merchant.setSlug("reschedule-merchant-user-" + System.nanoTime());
    merchant.setActive(true);
    entityManager.persist(merchant);

    PlatformUser user = new PlatformUser();
    user.setUsername("reschedule-merchant-" + System.nanoTime() + "@example.com");
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
