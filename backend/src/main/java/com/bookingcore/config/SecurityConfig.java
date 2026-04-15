package com.bookingcore.config;

import com.bookingcore.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private static final String[] AUTHENTICATED_ROLE_AUTHORITIES = {
    "ROLE_CLIENT",
    "ROLE_CLIENT_USER",
    "ROLE_MERCHANT",
    "ROLE_MERCHANT_OWNER",
    "ROLE_SUB_MERCHANT",
    "ROLE_MERCHANT_STAFF",
    "ROLE_SYSTEM_ADMIN"
  };

  private static final String[] MERCHANT_ROLE_AUTHORITIES = {
    "ROLE_MERCHANT", "ROLE_MERCHANT_OWNER", "ROLE_SUB_MERCHANT", "ROLE_MERCHANT_STAFF"
  };

  private final BookingPlatformProperties platformProperties;
  private final ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilter;
  private final ObjectMapper objectMapper;

  public SecurityConfig(
      BookingPlatformProperties platformProperties,
      ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilter,
      ObjectMapper objectMapper) {
    this.platformProperties = platformProperties;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.objectMapper = objectMapper;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            eh ->
                eh.authenticationEntryPoint(new BookingAuthenticationEntryPoint(objectMapper))
                    .accessDeniedHandler(new BookingAccessDeniedHandler(objectMapper)));

    jwtAuthenticationFilter.ifAvailable(
        filter -> http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class));

    if (StringUtils.hasText(platformProperties.getJwt().getSecret())) {
      http.authorizeHttpRequests(
          a ->
              a.requestMatchers("/h2-console", "/h2-console/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.POST, "/api/client/bookings")
                  .hasAnyAuthority("ROLE_CLIENT", "ROLE_CLIENT_USER")
                  .requestMatchers(HttpMethod.GET, "/api/client/bookings")
                  .hasAnyAuthority("ROLE_CLIENT", "ROLE_CLIENT_USER")
                  .requestMatchers("/api/client/**")
                  .permitAll()
                  .requestMatchers("/api/public/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register")
                  .permitAll()
                  .requestMatchers(HttpMethod.GET, "/api/auth/me")
                  .hasAnyAuthority(AUTHENTICATED_ROLE_AUTHORITIES)
                  .requestMatchers(HttpMethod.POST, "/api/auth/logout", "/api/auth/refresh")
                  .hasAnyAuthority(AUTHENTICATED_ROLE_AUTHORITIES)
                  .requestMatchers(HttpMethod.POST, "/api/auth/context/select")
                  .hasAnyAuthority(AUTHENTICATED_ROLE_AUTHORITIES)
                  .requestMatchers(HttpMethod.POST, "/api/auth/context/switch")
                  .hasAnyAuthority(AUTHENTICATED_ROLE_AUTHORITIES)
                  .requestMatchers(HttpMethod.POST, "/api/auth/merchant/enable")
                  .hasAnyAuthority(AUTHENTICATED_ROLE_AUTHORITIES)
                  .requestMatchers(HttpMethod.POST, "/api/merchant/register")
                  .permitAll()
                  .requestMatchers("/api/me/**")
                  .hasAnyAuthority(AUTHENTICATED_ROLE_AUTHORITIES)
                  // Explicit system operation endpoint under merchant namespace (legacy route).
                  .requestMatchers(HttpMethod.POST, "/api/merchant/merchants")
                  .hasRole("SYSTEM_ADMIN")
                  .requestMatchers("/api/merchant/**")
                  .hasAnyAuthority(MERCHANT_ROLE_AUTHORITIES)
                  .requestMatchers("/api/system/**")
                  .hasRole("SYSTEM_ADMIN")
                  .anyRequest()
                  .permitAll());
    } else {
      // Secure-by-default fallback: client/public endpoints remain accessible,
      // but merchant/system APIs must not be accidentally exposed.
      http.authorizeHttpRequests(
          a ->
              a.requestMatchers("/h2-console", "/h2-console/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.POST, "/api/client/bookings")
                  .denyAll()
                  .requestMatchers(HttpMethod.GET, "/api/client/bookings")
                  .denyAll()
                  .requestMatchers("/api/client/**")
                  .permitAll()
                  .requestMatchers("/api/public/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register")
                  .permitAll()
                  .requestMatchers("/api/auth/**")
                  .denyAll()
                  .requestMatchers("/api/me/**")
                  .denyAll()
                  .requestMatchers("/api/merchant/**")
                  .denyAll()
                  .requestMatchers("/api/system/**")
                  .denyAll()
                  .anyRequest()
                  .permitAll());
    }

    return http.build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(
        List.of(
            "http://localhost:25173",
            "http://localhost:25174",
            "http://localhost:25175",
            "http://127.0.0.1:25173",
            "http://127.0.0.1:25174",
            "http://127.0.0.1:25175"));
    configuration.setAllowedMethods(List.of("*"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }
}
