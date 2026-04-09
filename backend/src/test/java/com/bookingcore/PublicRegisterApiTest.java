package com.bookingcore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookingcore.service.PublicRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PublicRegisterApiTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void authRegisterRejectsSystemAdminType() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"registerType\":\"SYSTEM_ADMIN\",\"name\":\"Evil Co\",\"slug\":\"evil-co-sa\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void authRegisterRejectsSubMerchantType() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"registerType\":\"SUB_MERCHANT\",\"name\":\"Evil Sub\",\"slug\":\"evil-sub\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void authRegisterAllowsMerchantAndReturnsSafeNextDestination() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"registerType\":\"MERCHANT\",\"name\":\"Good Biz\",\"slug\":\"good-biz-pr\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.slug").value("good-biz-pr"))
        .andExpect(jsonPath("$.nextDestination").value(PublicRegistrationService.NEXT_DESTINATION_MERCHANT_LOGIN));

    String dest = PublicRegistrationService.NEXT_DESTINATION_MERCHANT_LOGIN;
    assertThat(dest).startsWith("/");
    assertThat(dest).doesNotContain("//");
    assertThat(dest.toLowerCase()).doesNotContain("http");
    assertThat(dest).doesNotContain("evil");
  }

  @Test
  void legacyMerchantRegisterIncludesSameNextDestination() throws Exception {
    mockMvc
        .perform(
            post("/api/merchant/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Legacy Path\",\"slug\":\"legacy-path-pr\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nextDestination").value(PublicRegistrationService.NEXT_DESTINATION_MERCHANT_LOGIN));
  }

  @Test
  void authRegisterAllowsClientAndReturnsSafeNextDestination() throws Exception {
    String u = "pr-client-" + System.nanoTime();
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"registerType\":\"CLIENT\",\"username\":\""
                        + u
                        + "\",\"password\":\"secret12\",\"name\":\"\",\"slug\":\"\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(u))
        .andExpect(jsonPath("$.nextDestination").value(PublicRegistrationService.NEXT_DESTINATION_CLIENT_LOGIN));
  }

  @Test
  void authRegisterClientRejectsDuplicateUsername() throws Exception {
    String u = "pr-client-dup-" + System.nanoTime();
    String body =
        "{\"registerType\":\"CLIENT\",\"username\":\""
            + u
            + "\",\"password\":\"secret12\",\"name\":\"\",\"slug\":\"\"}";
    mockMvc
        .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk());
    mockMvc
        .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isConflict());
  }

  @Test
  void authRegisterMerchantRejectsMissingSlug() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"registerType\":\"MERCHANT\",\"name\":\"No Slug Co\",\"slug\":\"\"}"))
        .andExpect(status().isBadRequest());
  }
}
