package com.bookingcore;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookingcore.modules.booking.Booking;
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
class ClientBookingListApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordEncoder passwordEncoder;
  @PersistenceContext private EntityManager entityManager;

  @Test
  void listBookings_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/client/bookings")).andExpect(status().isUnauthorized());
  }

  @Test
  void listBookings_merchant_returns403() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("M " + System.nanoTime());
    merchant.setSlug("m-" + System.nanoTime());
    merchant.setActive(true);
    entityManager.persist(merchant);

    PlatformUser user = new PlatformUser();
    user.setUsername("merch-list-" + System.nanoTime() + "@example.com");
    user.setPasswordHash(passwordEncoder.encode("secret-pass"));
    user.setRole(PlatformUserRole.MERCHANT);
    user.setMerchant(merchant);
    user.setEnabled(true);
    entityManager.persist(user);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, user.getUsername(), "secret-pass");
    mockMvc
        .perform(get("/api/client/bookings").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void listBookings_afterCreate_returnsItemInUpcoming() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Pub " + System.nanoTime());
    merchant.setSlug("pub-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setVisibility(MerchantVisibility.PUBLIC);
    entityManager.persist(merchant);

    ServiceItem serviceItem = new ServiceItem();
    serviceItem.setMerchant(merchant);
    serviceItem.setName("Cut");
    serviceItem.setCategory("GENERAL");
    serviceItem.setDurationMinutes(30);
    serviceItem.setPrice(BigDecimal.TEN);
    entityManager.persist(serviceItem);
    entityManager.flush();

    ResourceItem resource = new ResourceItem();
    resource.setMerchant(merchant);
    resource.setName("Chair 1");
    resource.setType("ROOM");
    resource.setCategory("GENERAL");
    resource.setCapacity(1);
    resource.setServiceItemsJson("[\"" + serviceItem.getId() + "\"]");
    resource.setPrice(BigDecimal.TEN);
    resource.setActive(true);
    entityManager.persist(resource);
    entityManager.flush();

    PlatformUser client = new PlatformUser();
    client.setUsername("client-list-" + System.nanoTime() + "@example.com");
    client.setPasswordHash(passwordEncoder.encode("secret-pass"));
    client.setRole(PlatformUserRole.CLIENT);
    client.setEnabled(true);
    entityManager.persist(client);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, client.getUsername(), "secret-pass");
    LocalDateTime startAt = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);

    mockMvc
        .perform(
            post("/api/client/bookings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("resourceId", resource.getId(), "startAt", startAt, "notes", "hi"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.id").isString());

    mockMvc
        .perform(
            get("/api/client/bookings")
                .param("tab", "upcoming")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.items").doesNotExist())
        .andExpect(jsonPath("$.data.items.length()").value(1))
        .andExpect(jsonPath("$.data.items[0].serviceName").value("Cut"))
        .andExpect(jsonPath("$.data.items[0].providerName").value(merchant.getName()))
        .andExpect(jsonPath("$.data.items[0].status").value("PENDING"))
        .andExpect(jsonPath("$.data.total").value(1));
  }

  @Test
  void cancelOwnBooking_movesToCancelledAndAppearsInCancelledTab() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Cancel Merchant " + System.nanoTime());
    merchant.setSlug("cancel-merchant-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setVisibility(MerchantVisibility.PUBLIC);
    entityManager.persist(merchant);

    ServiceItem serviceItem = new ServiceItem();
    serviceItem.setMerchant(merchant);
    serviceItem.setName("Cancel Service");
    serviceItem.setCategory("GENERAL");
    serviceItem.setDurationMinutes(30);
    serviceItem.setPrice(BigDecimal.TEN);
    entityManager.persist(serviceItem);
    entityManager.flush();

    ResourceItem resource = new ResourceItem();
    resource.setMerchant(merchant);
    resource.setName("Cancel Resource");
    resource.setType("ROOM");
    resource.setCategory("GENERAL");
    resource.setCapacity(1);
    resource.setServiceItemsJson("[\"" + serviceItem.getId() + "\"]");
    resource.setPrice(BigDecimal.TEN);
    resource.setActive(true);
    entityManager.persist(resource);
    entityManager.flush();

    PlatformUser client = new PlatformUser();
    client.setUsername("client-cancel-" + System.nanoTime() + "@example.com");
    client.setPasswordHash(passwordEncoder.encode("secret-pass"));
    client.setRole(PlatformUserRole.CLIENT);
    client.setEnabled(true);
    entityManager.persist(client);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, client.getUsername(), "secret-pass");
    LocalDateTime startAt = LocalDateTime.now().plusDays(2).withHour(11).withMinute(0).withSecond(0).withNano(0);

    String createdJson =
        mockMvc
            .perform(
                post("/api/client/bookings")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of("resourceId", resource.getId(), "startAt", startAt))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID bookingId =
        UUID.fromString(objectMapper.readTree(createdJson).path("data").path("id").asText());

    mockMvc
        .perform(
            patch("/api/client/bookings/" + bookingId + "/cancel")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"change plan\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.id", is(bookingId.toString())))
        .andExpect(jsonPath("$.data.status").value("CANCELLED"));

    mockMvc
        .perform(
            get("/api/client/bookings")
                .param("tab", "cancelled")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.items.length()").value(1))
        .andExpect(jsonPath("$.data.items[0].status").value("CANCELLED"));
  }

  @Test
  void listBookings_negativePage_clampsToZero() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Page Merchant " + System.nanoTime());
    merchant.setSlug("page-merchant-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setVisibility(MerchantVisibility.PUBLIC);
    entityManager.persist(merchant);

    ServiceItem serviceItem = new ServiceItem();
    serviceItem.setMerchant(merchant);
    serviceItem.setName("Svc");
    serviceItem.setCategory("GENERAL");
    serviceItem.setDurationMinutes(30);
    serviceItem.setPrice(BigDecimal.TEN);
    entityManager.persist(serviceItem);
    entityManager.flush();

    ResourceItem resource = new ResourceItem();
    resource.setMerchant(merchant);
    resource.setName("Res");
    resource.setType("ROOM");
    resource.setCategory("GENERAL");
    resource.setCapacity(1);
    resource.setServiceItemsJson("[\"" + serviceItem.getId() + "\"]");
    resource.setPrice(BigDecimal.TEN);
    resource.setActive(true);
    entityManager.persist(resource);
    entityManager.flush();

    PlatformUser client = new PlatformUser();
    client.setUsername("client-page-" + System.nanoTime() + "@example.com");
    client.setPasswordHash(passwordEncoder.encode("secret-pass"));
    client.setRole(PlatformUserRole.CLIENT);
    client.setEnabled(true);
    entityManager.persist(client);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, client.getUsername(), "secret-pass");
    LocalDateTime startAt = LocalDateTime.now().plusDays(3).withHour(9).withMinute(0).withSecond(0).withNano(0);
    mockMvc
        .perform(
            post("/api/client/bookings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(Map.of("resourceId", resource.getId(), "startAt", startAt))))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            get("/api/client/bookings")
                .param("tab", "upcoming")
                .param("page", "-3")
                .param("size", "20")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.page").value(0));
  }

  @Test
  void listBookings_oversizedSize_capsAt100() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Size Merchant " + System.nanoTime());
    merchant.setSlug("size-merchant-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setVisibility(MerchantVisibility.PUBLIC);
    entityManager.persist(merchant);

    ServiceItem serviceItem = new ServiceItem();
    serviceItem.setMerchant(merchant);
    serviceItem.setName("Svc2");
    serviceItem.setCategory("GENERAL");
    serviceItem.setDurationMinutes(30);
    serviceItem.setPrice(BigDecimal.TEN);
    entityManager.persist(serviceItem);
    entityManager.flush();

    ResourceItem resource = new ResourceItem();
    resource.setMerchant(merchant);
    resource.setName("Res2");
    resource.setType("ROOM");
    resource.setCategory("GENERAL");
    resource.setCapacity(1);
    resource.setServiceItemsJson("[\"" + serviceItem.getId() + "\"]");
    resource.setPrice(BigDecimal.TEN);
    resource.setActive(true);
    entityManager.persist(resource);
    entityManager.flush();

    PlatformUser client = new PlatformUser();
    client.setUsername("client-size-" + System.nanoTime() + "@example.com");
    client.setPasswordHash(passwordEncoder.encode("secret-pass"));
    client.setRole(PlatformUserRole.CLIENT);
    client.setEnabled(true);
    entityManager.persist(client);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, client.getUsername(), "secret-pass");
    LocalDateTime startAt = LocalDateTime.now().plusDays(4).withHour(9).withMinute(0).withSecond(0).withNano(0);
    mockMvc
        .perform(
            post("/api/client/bookings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(Map.of("resourceId", resource.getId(), "startAt", startAt))))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            get("/api/client/bookings")
                .param("tab", "upcoming")
                .param("page", "0")
                .param("size", "9999")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.size").value(100));
  }

  @Test
  void listBookings_unknownTab_fallsBackToUpcomingSemantics() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Tab Merchant " + System.nanoTime());
    merchant.setSlug("tab-merchant-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setVisibility(MerchantVisibility.PUBLIC);
    entityManager.persist(merchant);

    ServiceItem serviceItem = new ServiceItem();
    serviceItem.setMerchant(merchant);
    serviceItem.setName("Svc3");
    serviceItem.setCategory("GENERAL");
    serviceItem.setDurationMinutes(30);
    serviceItem.setPrice(BigDecimal.TEN);
    entityManager.persist(serviceItem);
    entityManager.flush();

    ResourceItem resource = new ResourceItem();
    resource.setMerchant(merchant);
    resource.setName("Res3");
    resource.setType("ROOM");
    resource.setCategory("GENERAL");
    resource.setCapacity(1);
    resource.setServiceItemsJson("[\"" + serviceItem.getId() + "\"]");
    resource.setPrice(BigDecimal.TEN);
    resource.setActive(true);
    entityManager.persist(resource);
    entityManager.flush();

    PlatformUser client = new PlatformUser();
    client.setUsername("client-tab-" + System.nanoTime() + "@example.com");
    client.setPasswordHash(passwordEncoder.encode("secret-pass"));
    client.setRole(PlatformUserRole.CLIENT);
    client.setEnabled(true);
    entityManager.persist(client);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, client.getUsername(), "secret-pass");
    LocalDateTime startAt = LocalDateTime.now().plusDays(5).withHour(9).withMinute(0).withSecond(0).withNano(0);
    mockMvc
        .perform(
            post("/api/client/bookings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(Map.of("resourceId", resource.getId(), "startAt", startAt))))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            get("/api/client/bookings")
                .param("tab", "not-a-real-tab")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(1))
        .andExpect(jsonPath("$.data.total").value(1));

    mockMvc
        .perform(
            get("/api/client/bookings")
                .param("tab", "upcoming")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(1))
        .andExpect(jsonPath("$.data.total").value(1));
  }

  @Test
  void cancelTwice_secondRequest_returnsConflict() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Twice Merchant " + System.nanoTime());
    merchant.setSlug("twice-merchant-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setVisibility(MerchantVisibility.PUBLIC);
    entityManager.persist(merchant);

    ServiceItem serviceItem = new ServiceItem();
    serviceItem.setMerchant(merchant);
    serviceItem.setName("Twice Service");
    serviceItem.setCategory("GENERAL");
    serviceItem.setDurationMinutes(30);
    serviceItem.setPrice(BigDecimal.TEN);
    entityManager.persist(serviceItem);
    entityManager.flush();

    ResourceItem resource = new ResourceItem();
    resource.setMerchant(merchant);
    resource.setName("Twice Resource");
    resource.setType("ROOM");
    resource.setCategory("GENERAL");
    resource.setCapacity(1);
    resource.setServiceItemsJson("[\"" + serviceItem.getId() + "\"]");
    resource.setPrice(BigDecimal.TEN);
    resource.setActive(true);
    entityManager.persist(resource);
    entityManager.flush();

    PlatformUser client = new PlatformUser();
    client.setUsername("client-twice-" + System.nanoTime() + "@example.com");
    client.setPasswordHash(passwordEncoder.encode("secret-pass"));
    client.setRole(PlatformUserRole.CLIENT);
    client.setEnabled(true);
    entityManager.persist(client);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, client.getUsername(), "secret-pass");
    LocalDateTime startAt = LocalDateTime.now().plusDays(6).withHour(12).withMinute(0).withSecond(0).withNano(0);

    String createdJson =
        mockMvc
            .perform(
                post("/api/client/bookings")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of("resourceId", resource.getId(), "startAt", startAt))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID bookingId =
        UUID.fromString(objectMapper.readTree(createdJson).path("data").path("id").asText());

    mockMvc
        .perform(
            patch("/api/client/bookings/" + bookingId + "/cancel")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"first\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            patch("/api/client/bookings/" + bookingId + "/cancel")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"second\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.data.errorCode").value("STATE_CONFLICT"));
  }

  @Test
  void cancelBooking_otherClient_returns404() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Owner Merchant " + System.nanoTime());
    merchant.setSlug("owner-merchant-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setVisibility(MerchantVisibility.PUBLIC);
    entityManager.persist(merchant);

    ServiceItem serviceItem = new ServiceItem();
    serviceItem.setMerchant(merchant);
    serviceItem.setName("Owner Service");
    serviceItem.setCategory("GENERAL");
    serviceItem.setDurationMinutes(30);
    serviceItem.setPrice(BigDecimal.TEN);
    entityManager.persist(serviceItem);
    entityManager.flush();

    ResourceItem resource = new ResourceItem();
    resource.setMerchant(merchant);
    resource.setName("Owner Resource");
    resource.setType("ROOM");
    resource.setCategory("GENERAL");
    resource.setCapacity(1);
    resource.setServiceItemsJson("[\"" + serviceItem.getId() + "\"]");
    resource.setPrice(BigDecimal.TEN);
    resource.setActive(true);
    entityManager.persist(resource);
    entityManager.flush();

    PlatformUser owner = new PlatformUser();
    owner.setUsername("client-owner-" + System.nanoTime() + "@example.com");
    owner.setPasswordHash(passwordEncoder.encode("secret-pass"));
    owner.setRole(PlatformUserRole.CLIENT);
    owner.setEnabled(true);
    entityManager.persist(owner);

    PlatformUser other = new PlatformUser();
    other.setUsername("client-other-" + System.nanoTime() + "@example.com");
    other.setPasswordHash(passwordEncoder.encode("secret-pass"));
    other.setRole(PlatformUserRole.CLIENT);
    other.setEnabled(true);
    entityManager.persist(other);
    entityManager.flush();

    String ownerToken = TestJwtHelper.login(mockMvc, objectMapper, owner.getUsername(), "secret-pass");
    String otherToken = TestJwtHelper.login(mockMvc, objectMapper, other.getUsername(), "secret-pass");
    LocalDateTime startAt = LocalDateTime.now().plusDays(7).withHour(14).withMinute(0).withSecond(0).withNano(0);

    String createdJson =
        mockMvc
            .perform(
                post("/api/client/bookings")
                    .header("Authorization", "Bearer " + ownerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of("resourceId", resource.getId(), "startAt", startAt))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID bookingId =
        UUID.fromString(objectMapper.readTree(createdJson).path("data").path("id").asText());

    mockMvc
        .perform(
            patch("/api/client/bookings/" + bookingId + "/cancel")
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"should fail\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void listBookings_pageBeyondLast_returnsEmptyItemsPreservesTotal() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Empty Page Merchant " + System.nanoTime());
    merchant.setSlug("empty-page-merchant-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setVisibility(MerchantVisibility.PUBLIC);
    entityManager.persist(merchant);

    ServiceItem serviceItem = new ServiceItem();
    serviceItem.setMerchant(merchant);
    serviceItem.setName("Empty Page Svc");
    serviceItem.setCategory("GENERAL");
    serviceItem.setDurationMinutes(30);
    serviceItem.setPrice(BigDecimal.TEN);
    entityManager.persist(serviceItem);
    entityManager.flush();

    ResourceItem resource = new ResourceItem();
    resource.setMerchant(merchant);
    resource.setName("Empty Page Res");
    resource.setType("ROOM");
    resource.setCategory("GENERAL");
    resource.setCapacity(1);
    resource.setServiceItemsJson("[\"" + serviceItem.getId() + "\"]");
    resource.setPrice(BigDecimal.TEN);
    resource.setActive(true);
    entityManager.persist(resource);
    entityManager.flush();

    PlatformUser client = new PlatformUser();
    client.setUsername("client-emptypage-" + System.nanoTime() + "@example.com");
    client.setPasswordHash(passwordEncoder.encode("secret-pass"));
    client.setRole(PlatformUserRole.CLIENT);
    client.setEnabled(true);
    entityManager.persist(client);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, client.getUsername(), "secret-pass");
    LocalDateTime startAt = LocalDateTime.now().plusDays(8).withHour(8).withMinute(0).withSecond(0).withNano(0);
    mockMvc
        .perform(
            post("/api/client/bookings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(Map.of("resourceId", resource.getId(), "startAt", startAt))))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            get("/api/client/bookings")
                .param("tab", "upcoming")
                .param("page", "5")
                .param("size", "1")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(0))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.page").value(5));
  }

  @Test
  void listBookings_pastTab_includesPastNonCancelledBooking() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Past Merchant " + System.nanoTime());
    merchant.setSlug("past-merchant-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setVisibility(MerchantVisibility.PUBLIC);
    entityManager.persist(merchant);

    ServiceItem serviceItem = new ServiceItem();
    serviceItem.setMerchant(merchant);
    serviceItem.setName("Past Service");
    serviceItem.setCategory("GENERAL");
    serviceItem.setDurationMinutes(30);
    serviceItem.setPrice(BigDecimal.TEN);
    entityManager.persist(serviceItem);
    entityManager.flush();

    PlatformUser client = new PlatformUser();
    client.setUsername("client-past-" + System.nanoTime() + "@example.com");
    client.setPasswordHash(passwordEncoder.encode("secret-pass"));
    client.setRole(PlatformUserRole.CLIENT);
    client.setEnabled(true);
    entityManager.persist(client);
    entityManager.flush();

    LocalDateTime pastStart = LocalDateTime.now().minusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
    Booking past = new Booking();
    past.setMerchant(merchant);
    past.setServiceItem(serviceItem);
    past.setPlatformUser(client);
    past.setStartAt(pastStart);
    past.setEndAt(pastStart.plusMinutes(30));
    past.setCustomerName("CLIENT");
    past.setCustomerContact("N/A");
    past.setStatus(BookingStatus.COMPLETED);
    entityManager.persist(past);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, client.getUsername(), "secret-pass");
    mockMvc
        .perform(
            get("/api/client/bookings")
                .param("tab", "past")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(1))
        .andExpect(jsonPath("$.data.items[0].status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.total").value(1));
  }

  @Test
  void cancelCompletedBooking_returnsConflict() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Done Merchant " + System.nanoTime());
    merchant.setSlug("done-merchant-" + System.nanoTime());
    merchant.setActive(true);
    merchant.setVisibility(MerchantVisibility.PUBLIC);
    entityManager.persist(merchant);

    ServiceItem serviceItem = new ServiceItem();
    serviceItem.setMerchant(merchant);
    serviceItem.setName("Done Service");
    serviceItem.setCategory("GENERAL");
    serviceItem.setDurationMinutes(30);
    serviceItem.setPrice(BigDecimal.TEN);
    entityManager.persist(serviceItem);
    entityManager.flush();

    PlatformUser client = new PlatformUser();
    client.setUsername("client-done-" + System.nanoTime() + "@example.com");
    client.setPasswordHash(passwordEncoder.encode("secret-pass"));
    client.setRole(PlatformUserRole.CLIENT);
    client.setEnabled(true);
    entityManager.persist(client);
    entityManager.flush();

    LocalDateTime start = LocalDateTime.now().minusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
    Booking booking = new Booking();
    booking.setMerchant(merchant);
    booking.setServiceItem(serviceItem);
    booking.setPlatformUser(client);
    booking.setStartAt(start);
    booking.setEndAt(start.plusMinutes(30));
    booking.setCustomerName("CLIENT");
    booking.setCustomerContact("N/A");
    booking.setStatus(BookingStatus.COMPLETED);
    entityManager.persist(booking);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, client.getUsername(), "secret-pass");
    mockMvc
        .perform(
            patch("/api/client/bookings/" + booking.getId() + "/cancel")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"too late\"}"))
        .andExpect(status().isConflict());
  }
}
