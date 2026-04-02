package com.bookingcore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void shouldCreateMerchantAndReadDefaultCustomization() throws Exception {
    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    mockMvc
        .perform(
            post("/api/merchant/merchants")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Merchant\",\"slug\":\"test-merchant\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.slug").value("test-merchant"));

    mockMvc
        .perform(
            get("/api/merchant/1/customization").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.themeColor").exists())
        .andExpect(jsonPath("$.homepageSectionsJson").exists());
  }

  @Test
  void shouldRegisterMerchantViaMerchantNamespace() throws Exception {
    mockMvc.perform(post("/api/merchant/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Portal Merchant\",\"slug\":\"portal-merchant-reg\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.slug").value("portal-merchant-reg"))
        .andExpect(jsonPath("$.id").exists());
  }
}
