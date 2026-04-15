package com.bookingcore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookingcore.modules.booking.Booking;
import com.bookingcore.modules.booking.BookingStatus;
import com.bookingcore.modules.booking.BusinessHours;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
class ClientResourceAvailabilityApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordEncoder passwordEncoder;
  @PersistenceContext private EntityManager entityManager;

  @Test
  void resourceAvailability_returnsSlotsForClient() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Public Merchant " + System.nanoTime());
    merchant.setSlug("public-merchant-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setVisibility(MerchantVisibility.PUBLIC);
    entityManager.persist(merchant);

    ResourceItem resource = new ResourceItem();
    resource.setMerchant(merchant);
    resource.setName("Room A");
    resource.setType("ROOM");
    resource.setCategory("GENERAL");
    resource.setCapacity(2);
    resource.setServiceItemsJson("[]");
    resource.setPrice(BigDecimal.TEN);
    resource.setActive(true);
    entityManager.persist(resource);

    LocalDate date = LocalDate.now().plusDays(1);
    BusinessHours hours = new BusinessHours();
    hours.setMerchant(merchant);
    hours.setDayOfWeek(date.getDayOfWeek());
    hours.setStartTime(LocalTime.of(9, 0));
    hours.setEndTime(LocalTime.of(10, 0));
    entityManager.persist(hours);
    entityManager.flush();

    String clientToken = loginClientUser();
    mockMvc
        .perform(
            get("/api/client/resources/" + resource.getId() + "/availability")
                .header("Authorization", "Bearer " + clientToken)
                .param("date", date.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.date").value(date.toString()))
        .andExpect(jsonPath("$.slots[0].startAt").exists())
        .andExpect(jsonPath("$.slots[0].endAt").exists())
        .andExpect(jsonPath("$.slots[0].isAvailable").isBoolean())
        .andExpect(jsonPath("$.slots[0].capacityRemaining").value(2));
  }

  @Test
  void resourceAvailability_invalidDate_returns400() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Invalid Date Merchant " + System.nanoTime());
    merchant.setSlug("invalid-date-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setVisibility(MerchantVisibility.PUBLIC);
    entityManager.persist(merchant);

    ResourceItem resource = new ResourceItem();
    resource.setMerchant(merchant);
    resource.setName("Room B");
    resource.setType("ROOM");
    resource.setCategory("GENERAL");
    resource.setCapacity(1);
    resource.setServiceItemsJson("[]");
    resource.setPrice(BigDecimal.ONE);
    resource.setActive(true);
    entityManager.persist(resource);
    entityManager.flush();

    String clientToken = loginClientUser();
    mockMvc
        .perform(
            get("/api/client/resources/" + resource.getId() + "/availability")
                .header("Authorization", "Bearer " + clientToken)
                .param("date", "2026/04/10"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void resourceAvailability_crossTenantMerchantContext_returns404() throws Exception {
    Merchant tenantA = new Merchant();
    tenantA.setName("Tenant A " + System.nanoTime());
    tenantA.setSlug("tenant-a-" + System.nanoTime());
    tenantA.setActive(true);
    tenantA.setVisibility(MerchantVisibility.PUBLIC);
    entityManager.persist(tenantA);

    ResourceItem resource = new ResourceItem();
    resource.setMerchant(tenantA);
    resource.setName("Tenant A Resource");
    resource.setType("ROOM");
    resource.setCategory("GENERAL");
    resource.setCapacity(1);
    resource.setServiceItemsJson("[]");
    resource.setPrice(BigDecimal.ONE);
    resource.setActive(true);
    entityManager.persist(resource);

    Merchant tenantB = new Merchant();
    tenantB.setName("Tenant B " + System.nanoTime());
    tenantB.setSlug("tenant-b-" + System.nanoTime());
    tenantB.setActive(true);
    entityManager.persist(tenantB);

    PlatformUser merchantUser = new PlatformUser();
    merchantUser.setUsername("merchant-cross-" + System.nanoTime() + "@example.com");
    merchantUser.setPasswordHash(passwordEncoder.encode("secret-pass"));
    merchantUser.setRole(PlatformUserRole.MERCHANT);
    merchantUser.setMerchant(tenantB);
    merchantUser.setEnabled(true);
    entityManager.persist(merchantUser);

    ServiceItem service = new ServiceItem();
    service.setMerchant(tenantA);
    service.setName("General");
    service.setCategory("GENERAL");
    service.setDurationMinutes(30);
    service.setPrice(BigDecimal.TEN);
    entityManager.persist(service);

    Booking booking = new Booking();
    booking.setMerchant(tenantA);
    booking.setServiceItem(service);
    booking.setStartAt(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0));
    booking.setEndAt(booking.getStartAt().plusMinutes(30));
    booking.setCustomerName("Cross Tenant");
    booking.setCustomerContact("0900");
    booking.setStatus(BookingStatus.CONFIRMED);
    entityManager.persist(booking);
    entityManager.flush();

    String merchantToken = TestJwtHelper.login(mockMvc, objectMapper, merchantUser.getUsername(), "secret-pass");
    mockMvc
        .perform(
            get("/api/client/resources/" + resource.getId() + "/availability")
                .header("Authorization", "Bearer " + merchantToken)
                .param("date", LocalDate.now().plusDays(1).toString()))
        .andExpect(status().isNotFound());
  }

  @Test
  void resourceAvailability_privateMerchantWithoutMembership_returns404() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Private Merchant " + System.nanoTime());
    merchant.setSlug("private-merchant-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setVisibility(MerchantVisibility.INVITE_ONLY);
    entityManager.persist(merchant);

    ResourceItem resource = new ResourceItem();
    resource.setMerchant(merchant);
    resource.setName("Private Resource");
    resource.setType("ROOM");
    resource.setCategory("GENERAL");
    resource.setCapacity(1);
    resource.setServiceItemsJson("[]");
    resource.setPrice(BigDecimal.ONE);
    resource.setActive(true);
    entityManager.persist(resource);

    PlatformUser client = new PlatformUser();
    client.setUsername("client-no-membership-" + System.nanoTime() + "@example.com");
    client.setPasswordHash(passwordEncoder.encode("secret-pass"));
    client.setRole(PlatformUserRole.CLIENT);
    client.setEnabled(true);
    entityManager.persist(client);
    entityManager.flush();

    String clientToken = TestJwtHelper.login(mockMvc, objectMapper, client.getUsername(), "secret-pass");
    mockMvc
        .perform(
            get("/api/client/resources/" + resource.getId() + "/availability")
                .header("Authorization", "Bearer " + clientToken)
                .param("date", LocalDate.now().plusDays(1).toString()))
        .andExpect(status().isNotFound());
  }

  @Test
  void resourceAvailability_onlyCountsBookingsForResourceServiceScope() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Scoped Occupancy " + System.nanoTime());
    merchant.setSlug("scoped-occupancy-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setVisibility(MerchantVisibility.PUBLIC);
    entityManager.persist(merchant);

    ResourceItem resource = new ResourceItem();
    resource.setMerchant(merchant);
    resource.setName("Scoped Resource");
    resource.setType("ROOM");
    resource.setCategory("GENERAL");
    resource.setCapacity(1);
    resource.setServiceItemsJson("[]");
    resource.setPrice(BigDecimal.TEN);
    resource.setActive(true);
    entityManager.persist(resource);

    ServiceItem serviceForResource = new ServiceItem();
    serviceForResource.setMerchant(merchant);
    serviceForResource.setName("For Resource");
    serviceForResource.setCategory("GENERAL");
    serviceForResource.setDurationMinutes(30);
    serviceForResource.setPrice(BigDecimal.TEN);
    entityManager.persist(serviceForResource);

    ServiceItem unrelatedService = new ServiceItem();
    unrelatedService.setMerchant(merchant);
    unrelatedService.setName("Unrelated");
    unrelatedService.setCategory("GENERAL");
    unrelatedService.setDurationMinutes(30);
    unrelatedService.setPrice(BigDecimal.TEN);
    entityManager.persist(unrelatedService);

    entityManager.flush();
    resource.setServiceItemsJson("[\"" + serviceForResource.getId() + "\"]");

    LocalDate date = LocalDate.now().plusDays(1);
    BusinessHours hours = new BusinessHours();
    hours.setMerchant(merchant);
    hours.setDayOfWeek(date.getDayOfWeek());
    hours.setStartTime(LocalTime.of(9, 0));
    hours.setEndTime(LocalTime.of(10, 0));
    entityManager.persist(hours);

    Booking unrelatedBooking = new Booking();
    unrelatedBooking.setMerchant(merchant);
    unrelatedBooking.setServiceItem(unrelatedService);
    unrelatedBooking.setStartAt(date.atTime(9, 0));
    unrelatedBooking.setEndAt(date.atTime(9, 30));
    unrelatedBooking.setCustomerName("Other");
    unrelatedBooking.setCustomerContact("0900");
    unrelatedBooking.setStatus(BookingStatus.CONFIRMED);
    entityManager.persist(unrelatedBooking);
    entityManager.flush();

    String clientToken = loginClientUser();
    mockMvc
        .perform(
            get("/api/client/resources/" + resource.getId() + "/availability")
                .header("Authorization", "Bearer " + clientToken)
                .param("date", date.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.slots[0].isAvailable").value(true))
        .andExpect(jsonPath("$.slots[0].capacityRemaining").value(1));
  }

  @Test
  void resourceAvailability_emptyServiceScopeDoesNotCountUnrelatedBookings() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Empty Scope " + System.nanoTime());
    merchant.setSlug("empty-scope-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setVisibility(MerchantVisibility.PUBLIC);
    entityManager.persist(merchant);

    ResourceItem resource = new ResourceItem();
    resource.setMerchant(merchant);
    resource.setName("Empty Scope Resource");
    resource.setType("ROOM");
    resource.setCategory("GENERAL");
    resource.setCapacity(1);
    resource.setServiceItemsJson("[]");
    resource.setPrice(BigDecimal.TEN);
    resource.setActive(true);
    entityManager.persist(resource);

    ServiceItem unrelatedService = new ServiceItem();
    unrelatedService.setMerchant(merchant);
    unrelatedService.setName("Unrelated");
    unrelatedService.setCategory("GENERAL");
    unrelatedService.setDurationMinutes(30);
    unrelatedService.setPrice(BigDecimal.TEN);
    entityManager.persist(unrelatedService);

    LocalDate date = LocalDate.now().plusDays(1);
    BusinessHours hours = new BusinessHours();
    hours.setMerchant(merchant);
    hours.setDayOfWeek(date.getDayOfWeek());
    hours.setStartTime(LocalTime.of(9, 0));
    hours.setEndTime(LocalTime.of(10, 0));
    entityManager.persist(hours);

    Booking unrelatedBooking = new Booking();
    unrelatedBooking.setMerchant(merchant);
    unrelatedBooking.setServiceItem(unrelatedService);
    unrelatedBooking.setStartAt(date.atTime(9, 0));
    unrelatedBooking.setEndAt(date.atTime(9, 30));
    unrelatedBooking.setCustomerName("Other");
    unrelatedBooking.setCustomerContact("0900");
    unrelatedBooking.setStatus(BookingStatus.CONFIRMED);
    entityManager.persist(unrelatedBooking);
    entityManager.flush();

    String clientToken = loginClientUser();
    mockMvc
        .perform(
            get("/api/client/resources/" + resource.getId() + "/availability")
                .header("Authorization", "Bearer " + clientToken)
                .param("date", date.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.slots[0].isAvailable").value(true))
        .andExpect(jsonPath("$.slots[0].capacityRemaining").value(1));
  }

  private String loginClientUser() throws Exception {
    PlatformUser client = new PlatformUser();
    client.setUsername("availability-client-" + System.nanoTime() + "@example.com");
    client.setPasswordHash(passwordEncoder.encode("secret-pass"));
    client.setRole(PlatformUserRole.CLIENT);
    client.setEnabled(true);
    entityManager.persist(client);
    entityManager.flush();
    return TestJwtHelper.login(mockMvc, objectMapper, client.getUsername(), "secret-pass");
  }
}
