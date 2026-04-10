---
name: springboot-patterns
description: Spring Boot architecture patterns, REST API design, layered services, data access, caching, async processing, and logging. Use for Java Spring Boot backend work.
origin: ECC
---

# Spring Boot Development Patterns

Architecture and API patterns for scalable, production-grade Spring Boot services.

## When to Activate

- REST APIs (Spring MVC / WebFlux)
- Controller → service → repository layering
- Spring Data JPA, caching, async, scheduling
- Validation, centralized exception handling, pagination
- Profiles (dev / staging / prod)
- Events, messaging (Kafka, etc.)

## How This Skill Relates to `springboot-security`

| Topic | Use this skill for… | Use **springboot-security** for… |
|--------|---------------------|----------------------------------|
| DTOs & `@Valid` | Shape of requests/responses, mapping | Threat-focused validation reminders, sanitization |
| Filters | Request logging, tracing IDs | JWT/session filters, security headers, **rate limiting** |
| Exceptions | `ApiError`, validation errors | `AccessDenied`, security filter failures |

**Rate limiting, forwarded headers / client IP, CORS, CSRF, authn/authz** — keep one source of truth in **springboot-security**; do not duplicate Bucket4j or proxy guidance here.

---

## 1. Layered REST API

Thin controller, fat stays in services; constructor injection.

```java
@RestController
@RequestMapping("/api/markets")
@Validated
class MarketController {
  private final MarketService marketService;

  MarketController(MarketService marketService) {
    this.marketService = marketService;
  }

  @GetMapping
  ResponseEntity<Page<MarketResponse>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Page<Market> markets = marketService.list(PageRequest.of(page, size));
    return ResponseEntity.ok(markets.map(MarketResponse::from));
  }

  @PostMapping
  ResponseEntity<MarketResponse> create(@Valid @RequestBody CreateMarketRequest request) {
    Market market = marketService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(MarketResponse.from(market));
  }
}
```

---

## 2. DTOs and Validation

Use records + Bean Validation on ingress DTOs; map to domain or entities in the service.

```java
public record CreateMarketRequest(
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Size(max = 2000) String description,
    @NotNull @FutureOrPresent Instant endDate,
    @NotEmpty List<@NotBlank String> categories) {}

public record MarketResponse(Long id, String name, MarketStatus status) {
  static MarketResponse from(Market market) {
    return new MarketResponse(market.id(), market.name(), market.status());
  }
}
```

---

## 3. Repository (Spring Data JPA)

Prefer derived or `@Query` with named parameters; native SQL must use bindings (see **springboot-security** for injection pitfalls).

```java
public interface MarketRepository extends JpaRepository<MarketEntity, Long> {
  @Query("select m from MarketEntity m where m.status = :status order by m.volume desc")
  List<MarketEntity> findActive(@Param("status") MarketStatus status, Pageable pageable);
}
```

---

## 4. Service Layer and Transactions

```java
@Service
public class MarketService {
  private final MarketRepository repo;

  public MarketService(MarketRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public Market create(CreateMarketRequest request) {
    MarketEntity entity = MarketEntity.from(request);
    MarketEntity saved = repo.save(entity);
    return Market.from(saved);
  }
}
```

Use `@Transactional(readOnly = true)` on query-only service methods.

---

## 5. Pagination and Sorting

```java
PageRequest page = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending());
Page<Market> results = marketService.list(page);
```

---

## 6. Centralized Exception Handling

Map validation and unexpected errors to stable API shapes; log server errors with context.

```java
@ControllerAdvice
class GlobalExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(e -> e.getField() + ": " + e.getDefaultMessage())
        .collect(Collectors.joining(", "));
    return ResponseEntity.badRequest().body(ApiError.validation(message));
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ApiError> handleAccessDenied() {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.of("Forbidden"));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> handleGeneric(Exception ex) {
    // Log with stack trace; avoid leaking internals in body
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiError.of("Internal server error"));
  }
}
```

---

## 7. Caching

Enable with `@EnableCaching` on a `@Configuration` class.

```java
@Service
public class MarketCacheService {
  private final MarketRepository repo;

  public MarketCacheService(MarketRepository repo) {
    this.repo = repo;
  }

  @Cacheable(value = "market", key = "#id")
  public Market getById(Long id) {
    return repo.findById(id)
        .map(Market::from)
        .orElseThrow(() -> new EntityNotFoundException("Market not found"));
  }

  @CacheEvict(value = "market", key = "#id")
  public void evict(Long id) {}
}
```

---

## 8. Async Processing

Enable with `@EnableAsync`.

```java
@Service
public class NotificationService {
  @Async
  public CompletableFuture<Void> sendAsync(Notification notification) {
    return CompletableFuture.completedFuture(null);
  }
}
```

---

## 9. Observability-Oriented Filters

Use for request timing and correlation — not for auth (that belongs in **springboot-security**).

```java
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    long start = System.currentTimeMillis();
    try {
      filterChain.doFilter(request, response);
    } finally {
      long duration = System.currentTimeMillis() - start;
      log.info("req method={} uri={} status={} durationMs={}",
          request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
    }
  }
}
```

---

## 10. Structured Logging (SLF4J)

```java
@Service
public class ReportService {
  private static final Logger log = LoggerFactory.getLogger(ReportService.class);

  public Report generate(Long marketId) {
    log.info("generate_report marketId={}", marketId);
    try {
      return new Report();
    } catch (Exception ex) {
      log.error("generate_report_failed marketId={}", marketId, ex);
      throw ex;
    }
  }
}
```

Do not log secrets or PII — see **springboot-security**.

---

## 11. Resilient External Calls

```java
public <T> T withRetry(Supplier<T> supplier, int maxRetries) {
  int attempts = 0;
  while (true) {
    try {
      return supplier.get();
    } catch (Exception ex) {
      attempts++;
      if (attempts >= maxRetries) {
        throw ex;
      }
      try {
        Thread.sleep((long) Math.pow(2, attempts) * 100L);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        throw ex;
      }
    }
  }
}
```

Prefer resilience libraries (e.g. Resilience4j) for production circuit breaking and bulkheads.

---

## 12. Background Jobs

Use `@Scheduled` or queues (Kafka, SQS, RabbitMQ). Handlers should be **idempotent** and observable (metrics/logs).

---

## 13. Observability Stack

- Structured logs (e.g. JSON Logback encoder)
- Metrics: Micrometer + Prometheus / OpenTelemetry
- Tracing: Micrometer Tracing (Brave or OTel)

---

## 14. Production Defaults

- Constructor injection; avoid field `@Autowired`
- `spring.mvc.problemdetails.enabled=true` (RFC 7807, Boot 3+)
- Size HikariCP and connection timeouts for your load
- `@NonNull` / `Optional` where it clarifies contracts

**Remember:** Thin controllers, focused services, simple repositories, centralized errors. Optimize for testability and clear boundaries.
