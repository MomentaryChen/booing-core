package com.bookingcore;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(
    properties = {
      "spring.profiles.active=test",
      "booking.platform.auth.bootstrap-default-merchant.enabled=false",
      "booking.platform.auth.bootstrap-default-merchant-user.enabled=false",
      "booking.platform.auth.bootstrap-system-admin.enabled=false",
      "booking.platform.auth.bootstrap-default-client.enabled=false",
      "spring.flyway.enabled=true",
      "spring.flyway.locations=classpath:db/migration",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.jpa.show-sql=false",
      "spring.cache.type=none",
      "spring.data.redis.repositories.enabled=false",
      "booking.platform.jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    },
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
class FlywayMySqlMigrationTest {

  @Container
  @SuppressWarnings("resource")
  static final MySQLContainer<?> mysql =
      new MySQLContainer<>("mysql:8.4")
          .withDatabaseName("bookingcore")
          .withUsername("bookingcore")
          .withPassword("bookingcore");

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
  }

  @Test
  void contextLoads_withFlywayMigrationsApplied_andJpaValidatesSchema() {
    // If Flyway migrations fail or JPA validation mismatches schema, the context will fail to start.
  }
}

