package com.bookingcore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookingcore.modules.admin.AuditLog;
import com.bookingcore.modules.admin.AuditLogRepository;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.platform.PlatformUserRepository;
import com.bookingcore.modules.platform.rbac.PlatformRbacBindingStatus;
import com.bookingcore.modules.platform.rbac.PlatformUserRbacBinding;
import com.bookingcore.modules.platform.rbac.RbacPermission;
import com.bookingcore.modules.platform.rbac.RbacRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SystemUserManagementApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PlatformUserRepository platformUserRepository;
  @Autowired private AuditLogRepository auditLogRepository;
  @PersistenceContext private EntityManager entityManager;

  @Test
  void systemAdminCanListAndUpdateUserBindings() throws Exception {
    RbacPermission read = new RbacPermission();
    read.setCode("system.users.read");
    entityManager.persist(read);
    RbacPermission write = new RbacPermission();
    write.setCode("system.users.write");
    entityManager.persist(write);

    RbacRole adminRole = new RbacRole();
    adminRole.setCode("SYSTEM_ADMIN");
    adminRole.getPermissions().add(read);
    adminRole.getPermissions().add(write);
    entityManager.persist(adminRole);

    RbacRole clientRole = new RbacRole();
    clientRole.setCode("CLIENT");
    clientRole.getPermissions().add(read);
    entityManager.persist(clientRole);

    PlatformUser admin = platformUserRepository.findByUsername("admin").orElseThrow();
    PlatformUserRbacBinding adminBinding = new PlatformUserRbacBinding();
    adminBinding.setPlatformUser(admin);
    adminBinding.setRbacRole(adminRole);
    adminBinding.setStatus(PlatformRbacBindingStatus.ACTIVE);
    entityManager.persist(adminBinding);

    PlatformUser target = new PlatformUser();
    target.setUsername("system-user-target-" + System.nanoTime());
    target.setPasswordHash(admin.getPasswordHash());
    target.setRole(com.bookingcore.security.PlatformUserRole.CLIENT);
    target.setEnabled(true);
    entityManager.persist(target);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");

    mockMvc
        .perform(get("/api/system/users").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0]").exists());

    mockMvc
        .perform(
            put("/api/system/users/" + target.getId() + "/rbac-bindings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bindings\":[{\"roleCode\":\"CLIENT\",\"active\":true}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bindings[0].roleCode").value("CLIENT"))
        .andExpect(jsonPath("$.bindings[0].status").value("ACTIVE"));
  }

  @Test
  void cannotRemoveLastEnabledSystemAdminBinding() throws Exception {
    RbacPermission usersRead = new RbacPermission();
    usersRead.setCode("system.users.read");
    entityManager.persist(usersRead);
    RbacPermission usersWrite = new RbacPermission();
    usersWrite.setCode("system.users.write");
    entityManager.persist(usersWrite);

    RbacRole adminRole = new RbacRole();
    adminRole.setCode("SYSTEM_ADMIN");
    adminRole.getPermissions().add(usersRead);
    adminRole.getPermissions().add(usersWrite);
    entityManager.persist(adminRole);

    RbacRole clientRole = new RbacRole();
    clientRole.setCode("CLIENT");
    entityManager.persist(clientRole);

    PlatformUser admin = platformUserRepository.findByUsername("admin").orElseThrow();
    PlatformUserRbacBinding adminBinding = new PlatformUserRbacBinding();
    adminBinding.setPlatformUser(admin);
    adminBinding.setRbacRole(adminRole);
    adminBinding.setStatus(PlatformRbacBindingStatus.ACTIVE);
    entityManager.persist(adminBinding);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");

    mockMvc
        .perform(
            put("/api/system/users/" + admin.getId() + "/rbac-bindings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bindings\":[{\"roleCode\":\"CLIENT\",\"active\":true}]}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("LAST_SYSTEM_ADMIN_REQUIRED"));
  }

  @Test
  void nonSystemAdminCannotAccessSystemUserManagementApis() throws Exception {
    PlatformUser admin = platformUserRepository.findByUsername("admin").orElseThrow();
    PlatformUser target = new PlatformUser();
    target.setUsername("forbidden-target-" + System.nanoTime());
    target.setPasswordHash(admin.getPasswordHash());
    target.setRole(com.bookingcore.security.PlatformUserRole.CLIENT);
    target.setEnabled(true);
    entityManager.persist(target);
    entityManager.flush();

    String clientToken = TestJwtHelper.login(mockMvc, objectMapper, "client@example.com", "client");

    mockMvc
        .perform(get("/api/system/users").header("Authorization", "Bearer " + clientToken))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            put("/api/system/users/" + target.getId() + "/status")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}"))
        .andExpect(status().isForbidden());

    PlatformUser reloaded = platformUserRepository.findById(target.getId()).orElseThrow();
    Assertions.assertThat(reloaded.getEnabled()).isTrue();
  }

  @Test
  void statusNoopReturnsStableErrorCode() throws Exception {
    RbacPermission read = new RbacPermission();
    read.setCode("system.users.read");
    entityManager.persist(read);
    RbacPermission write = new RbacPermission();
    write.setCode("system.users.write");
    entityManager.persist(write);
    RbacRole adminRole = new RbacRole();
    adminRole.setCode("SYSTEM_ADMIN");
    adminRole.getPermissions().add(read);
    adminRole.getPermissions().add(write);
    entityManager.persist(adminRole);

    PlatformUser admin = platformUserRepository.findByUsername("admin").orElseThrow();
    PlatformUserRbacBinding adminBinding = new PlatformUserRbacBinding();
    adminBinding.setPlatformUser(admin);
    adminBinding.setRbacRole(adminRole);
    adminBinding.setStatus(PlatformRbacBindingStatus.ACTIVE);
    entityManager.persist(adminBinding);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");

    mockMvc
        .perform(
            put("/api/system/users/" + admin.getId() + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":true}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("USER_STATUS_NOOP"));
  }

  @Test
  void roleScopeMismatchIsRejectedWithStableCode() throws Exception {
    RbacPermission read = new RbacPermission();
    read.setCode("system.users.read");
    entityManager.persist(read);
    RbacPermission write = new RbacPermission();
    write.setCode("system.users.write");
    entityManager.persist(write);
    RbacRole adminRole = new RbacRole();
    adminRole.setCode("SYSTEM_ADMIN");
    adminRole.getPermissions().add(read);
    adminRole.getPermissions().add(write);
    entityManager.persist(adminRole);
    RbacRole clientRole = new RbacRole();
    clientRole.setCode("CLIENT");
    entityManager.persist(clientRole);

    PlatformUser admin = platformUserRepository.findByUsername("admin").orElseThrow();
    PlatformUserRbacBinding adminBinding = new PlatformUserRbacBinding();
    adminBinding.setPlatformUser(admin);
    adminBinding.setRbacRole(adminRole);
    adminBinding.setStatus(PlatformRbacBindingStatus.ACTIVE);
    entityManager.persist(adminBinding);

    PlatformUser target = new PlatformUser();
    target.setUsername("scope-target-" + System.nanoTime());
    target.setPasswordHash(admin.getPasswordHash());
    target.setRole(com.bookingcore.security.PlatformUserRole.CLIENT);
    target.setEnabled(true);
    entityManager.persist(target);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");

    mockMvc
        .perform(
            put("/api/system/users/" + target.getId() + "/rbac-bindings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bindings\":[{\"roleCode\":\"CLIENT\",\"merchantId\":1,\"active\":true}]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MERCHANT_ID_NOT_ALLOWED"));
  }

  @Test
  void statusMutationWritesAuditWithCorrelationAndBeforeAfter() throws Exception {
    RbacPermission read = new RbacPermission();
    read.setCode("system.users.read");
    entityManager.persist(read);
    RbacPermission write = new RbacPermission();
    write.setCode("system.users.write");
    entityManager.persist(write);
    RbacRole adminRole = new RbacRole();
    adminRole.setCode("SYSTEM_ADMIN");
    adminRole.getPermissions().add(read);
    adminRole.getPermissions().add(write);
    entityManager.persist(adminRole);

    PlatformUser admin = platformUserRepository.findByUsername("admin").orElseThrow();
    PlatformUserRbacBinding adminBinding = new PlatformUserRbacBinding();
    adminBinding.setPlatformUser(admin);
    adminBinding.setRbacRole(adminRole);
    adminBinding.setStatus(PlatformRbacBindingStatus.ACTIVE);
    entityManager.persist(adminBinding);

    PlatformUser target = new PlatformUser();
    target.setUsername("status-audit-target-" + System.nanoTime());
    target.setPasswordHash(admin.getPasswordHash());
    target.setRole(com.bookingcore.security.PlatformUserRole.CLIENT);
    target.setEnabled(true);
    entityManager.persist(target);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    String requestId = "qa-audit-" + System.nanoTime();

    mockMvc
        .perform(
            put("/api/system/users/" + target.getId() + "/status")
                .header("Authorization", "Bearer " + token)
                .header("X-Request-Id", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(false));

    List<AuditLog> logs = auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    AuditLog statusLog =
        logs.stream()
            .filter(
                l ->
                    "system.user.status.updated".equals(l.getAction())
                        && target.getId().equals(l.getTargetId()))
            .findFirst()
            .orElseThrow();
    Assertions.assertThat(statusLog.getCorrelationId()).isEqualTo(requestId);
    Assertions.assertThat(statusLog.getBeforeState()).contains("\"enabled\":true");
    Assertions.assertThat(statusLog.getAfterState()).contains("\"enabled\":false");
  }

  @Test
  void replaceBindingsIsIdempotentForDuplicatePayload() throws Exception {
    RbacPermission read = new RbacPermission();
    read.setCode("system.users.read");
    entityManager.persist(read);
    RbacPermission write = new RbacPermission();
    write.setCode("system.users.write");
    entityManager.persist(write);

    RbacRole adminRole = new RbacRole();
    adminRole.setCode("SYSTEM_ADMIN");
    adminRole.getPermissions().add(read);
    adminRole.getPermissions().add(write);
    entityManager.persist(adminRole);

    RbacRole clientRole = new RbacRole();
    clientRole.setCode("CLIENT");
    clientRole.getPermissions().add(read);
    entityManager.persist(clientRole);

    PlatformUser admin = platformUserRepository.findByUsername("admin").orElseThrow();
    PlatformUserRbacBinding adminBinding = new PlatformUserRbacBinding();
    adminBinding.setPlatformUser(admin);
    adminBinding.setRbacRole(adminRole);
    adminBinding.setStatus(PlatformRbacBindingStatus.ACTIVE);
    entityManager.persist(adminBinding);

    PlatformUser target = new PlatformUser();
    target.setUsername("idempotent-target-" + System.nanoTime());
    target.setPasswordHash(admin.getPasswordHash());
    target.setRole(com.bookingcore.security.PlatformUserRole.CLIENT);
    target.setEnabled(true);
    entityManager.persist(target);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    String body = "{\"bindings\":[{\"roleCode\":\"CLIENT\",\"active\":true}]}";

    String first =
        mockMvc
            .perform(
                put("/api/system/users/" + target.getId() + "/rbac-bindings")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String second =
        mockMvc
            .perform(
                put("/api/system/users/" + target.getId() + "/rbac-bindings")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode a = objectMapper.readTree(first).get("effectivePermissions");
    JsonNode b = objectMapper.readTree(second).get("effectivePermissions");
    Assertions.assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test
  void userDetailEffectivePermissionsMatchRepeatedReads() throws Exception {
    RbacPermission read = new RbacPermission();
    read.setCode("system.users.read");
    entityManager.persist(read);
    RbacPermission write = new RbacPermission();
    write.setCode("system.users.write");
    entityManager.persist(write);
    RbacPermission nav = new RbacPermission();
    nav.setCode("me.navigation.read");
    entityManager.persist(nav);

    RbacRole adminRole = new RbacRole();
    adminRole.setCode("SYSTEM_ADMIN");
    adminRole.getPermissions().add(read);
    adminRole.getPermissions().add(write);
    entityManager.persist(adminRole);

    RbacRole clientRole = new RbacRole();
    clientRole.setCode("CLIENT");
    clientRole.getPermissions().add(read);
    clientRole.getPermissions().add(nav);
    entityManager.persist(clientRole);

    PlatformUser admin = platformUserRepository.findByUsername("admin").orElseThrow();
    PlatformUserRbacBinding adminBinding = new PlatformUserRbacBinding();
    adminBinding.setPlatformUser(admin);
    adminBinding.setRbacRole(adminRole);
    adminBinding.setStatus(PlatformRbacBindingStatus.ACTIVE);
    entityManager.persist(adminBinding);

    PlatformUser target = new PlatformUser();
    target.setUsername("effperm-target-" + System.nanoTime());
    target.setPasswordHash(admin.getPasswordHash());
    target.setRole(com.bookingcore.security.PlatformUserRole.CLIENT);
    target.setEnabled(true);
    entityManager.persist(target);

    PlatformUserRbacBinding clientBinding = new PlatformUserRbacBinding();
    clientBinding.setPlatformUser(target);
    clientBinding.setRbacRole(clientRole);
    clientBinding.setStatus(PlatformRbacBindingStatus.ACTIVE);
    entityManager.persist(clientBinding);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");

    String d1 =
        mockMvc
            .perform(
                get("/api/system/users/" + target.getId()).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String d2 =
        mockMvc
            .perform(
                get("/api/system/users/" + target.getId()).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode p1 = objectMapper.readTree(d1).get("effectivePermissions");
    JsonNode p2 = objectMapper.readTree(d2).get("effectivePermissions");
    Assertions.assertThat(p1.toString()).isEqualTo(p2.toString());
    Assertions.assertThat(p1.toString()).contains("me.navigation.read");
    Assertions.assertThat(p1.toString()).contains("system.users.read");
  }

  @Test
  void merchantScopedBindingRejectsTenantMismatch() throws Exception {
    RbacPermission read = new RbacPermission();
    read.setCode("system.users.read");
    entityManager.persist(read);
    RbacPermission write = new RbacPermission();
    write.setCode("system.users.write");
    entityManager.persist(write);

    RbacRole adminRole = new RbacRole();
    adminRole.setCode("SYSTEM_ADMIN");
    adminRole.getPermissions().add(read);
    adminRole.getPermissions().add(write);
    entityManager.persist(adminRole);
    RbacRole merchantRole = new RbacRole();
    merchantRole.setCode("MERCHANT");
    merchantRole.getPermissions().add(read);
    entityManager.persist(merchantRole);

    PlatformUser admin = platformUserRepository.findByUsername("admin").orElseThrow();
    PlatformUserRbacBinding adminBinding = new PlatformUserRbacBinding();
    adminBinding.setPlatformUser(admin);
    adminBinding.setRbacRole(adminRole);
    adminBinding.setStatus(PlatformRbacBindingStatus.ACTIVE);
    entityManager.persist(adminBinding);

    Merchant m1 = new Merchant();
    m1.setName("tenant-a-" + System.nanoTime());
    m1.setSlug("tenant-a-" + System.nanoTime());
    m1.setActive(true);
    m1.setServiceLimit(5);
    entityManager.persist(m1);
    Merchant m2 = new Merchant();
    m2.setName("tenant-b-" + System.nanoTime());
    m2.setSlug("tenant-b-" + System.nanoTime());
    m2.setActive(true);
    m2.setServiceLimit(5);
    entityManager.persist(m2);

    PlatformUser target = new PlatformUser();
    target.setUsername("tenant-mismatch-target-" + System.nanoTime());
    target.setPasswordHash(admin.getPasswordHash());
    target.setRole(com.bookingcore.security.PlatformUserRole.MERCHANT);
    target.setMerchant(m1);
    target.setEnabled(true);
    entityManager.persist(target);
    entityManager.flush();

    String token = TestJwtHelper.login(mockMvc, objectMapper, "admin", "admin");
    mockMvc
        .perform(
            put("/api/system/users/" + target.getId() + "/rbac-bindings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"bindings\":[{\"roleCode\":\"MERCHANT\",\"merchantId\":"
                        + m2.getId()
                        + ",\"active\":true}]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("RBAC_TENANT_MISMATCH"));
  }
}
