package com.bookingcore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    user.setUsername("merch-list-" + System.nanoTime());
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
    resource.setServiceItemsJson("[" + serviceItem.getId() + "]");
    resource.setPrice(BigDecimal.TEN);
    resource.setActive(true);
    entityManager.persist(resource);
    entityManager.flush();

    PlatformUser client = new PlatformUser();
    client.setUsername("client-list-" + System.nanoTime());
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
        .andExpect(jsonPath("$.id").isNumber());

    mockMvc
        .perform(
            get("/api/client/bookings")
                .param("tab", "upcoming")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].serviceName").value("Cut"))
        .andExpect(jsonPath("$.items[0].providerName").value(merchant.getName()))
        .andExpect(jsonPath("$.items[0].status").value("PENDING"))
        .andExpect(jsonPath("$.total").value(1));
  }
}
