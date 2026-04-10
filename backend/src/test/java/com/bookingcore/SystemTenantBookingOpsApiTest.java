package com.bookingcore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookingcore.modules.booking.Booking;
import com.bookingcore.modules.booking.BookingStatus;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.security.PlatformUserRole;
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
class SystemTenantBookingOpsApiTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordEncoder passwordEncoder;
  @PersistenceContext private EntityManager entityManager;

  @Test
  void systemAdminCanTransitionBookingWithTenantScope() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Tenant Merchant " + System.nanoTime());
    merchant.setSlug("tenant-merchant-" + System.nanoTime());
    merchant.setActive(true);
    entityManager.persist(merchant);

    Booking booking = new Booking();
    booking.setMerchant(merchant);
    booking.setStatus(BookingStatus.CONFIRMED);
    booking.setCustomerName("Alex");
    booking.setCustomerContact("09xx");
    booking.setStartAt(LocalDateTime.now().plusDays(1));
    booking.setEndAt(LocalDateTime.now().plusDays(1).plusMinutes(30));
    booking.setServiceItem(entityManager.createQuery("select s from ServiceItem s", com.bookingcore.modules.service.ServiceItem.class)
        .setMaxResults(1)
        .getResultList()
        .stream()
        .findFirst()
        .orElseGet(() -> {
          com.bookingcore.modules.service.ServiceItem s = new com.bookingcore.modules.service.ServiceItem();
          s.setMerchant(merchant);
          s.setName("General");
          s.setCategory("GENERAL");
          s.setDurationMinutes(30);
          s.setPrice(java.math.BigDecimal.TEN);
          entityManager.persist(s);
          return s;
        }));
    entityManager.persist(booking);
    entityManager.flush();

    String adminToken = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    mockMvc
        .perform(
            post("/api/system/tenants/" + merchant.getId() + "/bookings/" + booking.getId() + "/transitions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"merchantId\":" + merchant.getId() + ",\"event\":\"CHECK_IN\",\"reason\":\"support\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CHECKED_IN"));
  }

  @Test
  void nonAdminCannotTransitionBookingWithTenantScope() throws Exception {
    Merchant merchant = new Merchant();
    merchant.setName("Tenant Merchant 2 " + System.nanoTime());
    merchant.setSlug("tenant-merchant-2-" + System.nanoTime());
    merchant.setActive(true);
    entityManager.persist(merchant);

    Booking booking = new Booking();
    booking.setMerchant(merchant);
    booking.setStatus(BookingStatus.CONFIRMED);
    booking.setCustomerName("Kim");
    booking.setCustomerContact("08xx");
    booking.setStartAt(LocalDateTime.now().plusDays(1));
    booking.setEndAt(LocalDateTime.now().plusDays(1).plusMinutes(30));
    booking.setServiceItem(entityManager.createQuery("select s from ServiceItem s", com.bookingcore.modules.service.ServiceItem.class)
        .setMaxResults(1)
        .getResultList()
        .stream()
        .findFirst()
        .orElseGet(() -> {
          com.bookingcore.modules.service.ServiceItem s = new com.bookingcore.modules.service.ServiceItem();
          s.setMerchant(merchant);
          s.setName("General");
          s.setCategory("GENERAL");
          s.setDurationMinutes(30);
          s.setPrice(java.math.BigDecimal.TEN);
          entityManager.persist(s);
          return s;
        }));
    entityManager.persist(booking);

    PlatformUser client = new PlatformUser();
    client.setUsername("tenant-client-" + System.nanoTime());
    client.setPasswordHash(passwordEncoder.encode("secret-pass"));
    client.setRole(PlatformUserRole.CLIENT);
    client.setEnabled(true);
    entityManager.persist(client);
    entityManager.flush();

    String clientToken = TestJwtHelper.login(mockMvc, objectMapper, client.getUsername(), "secret-pass");
    mockMvc
        .perform(
            post("/api/system/tenants/" + merchant.getId() + "/bookings/" + booking.getId() + "/transitions")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"merchantId\":" + merchant.getId() + ",\"event\":\"CHECK_IN\"}"))
        .andExpect(status().isForbidden());
  }
}
