---
name: springboot-security
description: Spring Security best practices for authn/authz, validation, CSRF, secrets, headers, rate limiting, and dependency security in Java Spring Boot services.
origin: ECC
---

# Spring Boot Security

Use for authentication, authorization, web hardening, secrets, and release security checks.

## When to Activate

- Authentication (JWT, OAuth2, sessions)
- Authorization (`@PreAuthorize`, custom `PermissionEvaluator`)
- Input validation and unsafe data (SQL, HTML)
- CORS, CSRF, security headers
- Secrets and credential rotation
- Rate limiting, brute-force mitigation
- Dependency / CVE posture

## How This Skill Relates to `springboot-patterns`

| Topic | **springboot-patterns** | This skill |
|--------|-------------------------|------------|
| REST layout, DTO records, `@Valid` | Primary reference | Reinforces “never trust input,” sanitization |
| Global `@ControllerAdvice` | API error shape | Map security exceptions consistently |
| Filters | Request logging | **Auth filters, rate limits** — canonical here |

---

## 1. Authentication

- Prefer stateless JWT or opaque tokens; support revocation when required
- Session cookies: `httpOnly`, `Secure`, `SameSite` appropriate to your threat model
- Validate tokens in `OncePerRequestFilter` or OAuth2 resource server

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
  private final JwtService jwtService;

  public JwtAuthFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      Authentication auth = jwtService.authenticate(token);
      SecurityContextHolder.getContext().setAuthentication(auth);
    }
    chain.doFilter(request, response);
  }
}
```

---

## 2. Authorization

- `@EnableMethodSecurity`
- Default **deny**; expose only required roles/scopes
- Prefer `@PreAuthorize` with expressions or dedicated `@Bean` authorization helpers

```java
@RestController
@RequestMapping("/api/admin")
public class AdminController {

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/users")
  public List<UserDto> listUsers() {
    return userService.findAll();
  }

  @PreAuthorize("@authz.isOwner(#id, authentication)")
  @DeleteMapping("/users/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
```

---

## 3. Input Validation and SQL Safety

- Always `@Valid` on request bodies; constrain DTOs (`@NotBlank`, `@Email`, `@Size`, custom validators)
- Sanitize HTML before rendering if user content is echoed
- **No** string-concatenated SQL; use Spring Data or `:name` parameters in `@Query`

```java
// BAD
@PostMapping("/users")
public User createUser(@RequestBody UserDto dto) {
  return userService.create(dto);
}

// GOOD
public record CreateUserDto(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Email String email,
    @NotNull @Min(0) @Max(150) Integer age
) {}

@PostMapping("/users")
public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserDto dto) {
  return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(dto));
}
```

```java
// BAD: concatenation in native query
@Query(value = "SELECT * FROM users WHERE name = '" + name + "'", nativeQuery = true)

// GOOD
@Query(value = "SELECT * FROM users WHERE name = :name", nativeQuery = true)
List<User> findByName(@Param("name") String name);
```

---

## 4. Password Encoding

- BCrypt or Argon2 via `PasswordEncoder` bean — never roll your own or store plaintext

```java
@Bean
public PasswordEncoder passwordEncoder() {
  return new BCryptPasswordEncoder(12);
}
```

---

## 5. CSRF and Session Policy

- **Browser** apps with cookies: keep CSRF enabled; send token in header or form
- **Bearer-token APIs**: typically `STATELESS` session + CSRF disabled

```java
http
  .csrf(csrf -> csrf.disable())
  .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
```

---

## 6. Secrets Management

- No secrets in git; env vars, secret managers, or Vault
- Rotate DB and API credentials

```yaml
spring:
  datasource:
    password: ${DB_PASSWORD}
```

---

## 7. Security Headers

```java
http
  .headers(headers -> headers
    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
    .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
    .xssProtection(Customizer.withDefaults())
    .referrerPolicy(rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)));
```

---

## 8. CORS

- Configure once in the security filter chain, not scattered on controllers
- **Never** `allowedOrigins("*")` with `allowCredentials(true)` in production

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
  CorsConfiguration config = new CorsConfiguration();
  config.setAllowedOrigins(List.of("https://app.example.com"));
  config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
  config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
  config.setAllowCredentials(true);
  config.setMaxAge(3600L);

  UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
  source.registerCorsConfiguration("/api/**", config);
  return source;
}

// SecurityFilterChain:
http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
```

---

## 9. Rate Limiting (Bucket4j) and Client IP

**Forwarded headers:** `X-Forwarded-For` is spoofable unless you sit behind a **trusted** reverse proxy and Spring is configured to trust it.

1. App behind nginx / ALB / similar
2. `server.forward-headers-strategy=NATIVE` or `FRAMEWORK`
3. With `FRAMEWORK`, register `ForwardedHeaderFilter` as a `@Bean`
4. Proxy should **overwrite** (not blindly append) `X-Forwarded-For`

When configured, `request.getRemoteAddr()` reflects the real client. **Do not** parse raw `X-Forwarded-For` in application code without that stack.

```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  private Bucket createBucket() {
    return Bucket.builder()
        .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
        .build();
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    String clientIp = request.getRemoteAddr();
    Bucket bucket = buckets.computeIfAbsent(clientIp, k -> createBucket());

    if (bucket.tryConsume(1)) {
      chain.doFilter(request, response);
    } else {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
    }
  }
}
```

Also consider gateway-level limits for coarse protection.

---

## 10. Logging and PII

- Never log passwords, tokens, full card numbers, or raw health identifiers
- Redact or hash where diagnostics require a stable key

---

## 11. File Uploads

- Enforce max size, content type, and extension policy
- Store outside web root; virus scan if policy requires

---

## 12. Dependency Security

- OWASP Dependency-Check, Snyk, or equivalent in CI
- Stay on supported Spring Boot / Spring Security lines; fail builds on critical CVEs when practical

---

## 13. Pre-Release Checklist

- [ ] Tokens validated, expiry and revocation behavior defined
- [ ] Sensitive endpoints guarded (method + URL rules)
- [ ] Inputs validated; no concatenated SQL
- [ ] CSRF/session policy matches app type (browser vs API)
- [ ] Secrets only from env / vault
- [ ] Security headers and CORS tightened for production
- [ ] Rate limits or gateway protection on abuse-prone routes
- [ ] Dependencies scanned; logs free of secrets and excessive PII

**Remember:** Deny by default, validate at the edge, least privilege, configure the platform (proxy + forward headers) before trusting client identity for limits.
