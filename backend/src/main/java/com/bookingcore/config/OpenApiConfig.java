package com.bookingcore.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI bookingCoreOpenApi() {
    final String schemeRef = "bearer-jwt";
    return new OpenAPI()
        .info(
            new Info()
                .title("booking-core API")
                .version("0.0.1-SNAPSHOT")
                .description(
                    "Runtime schema from Spring controllers. Normative P0 contract with success/error"
                        + " examples: repository file `doc/api/booking-core-p0.openapi.yaml`."))
        .components(
            new Components()
                .addSecuritySchemes(
                    schemeRef,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT from `accessToken` in POST /api/auth/login response.")));
  }
}
