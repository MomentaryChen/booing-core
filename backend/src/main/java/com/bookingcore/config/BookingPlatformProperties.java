package com.bookingcore.config;

import com.bookingcore.security.PlatformUserRole;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "booking.platform")
public class BookingPlatformProperties {

  /** IANA zone id for command-center date boundaries (e.g. Asia/Taipei). */
  private String timeZone = "Asia/Taipei";

  /**
   * When non-blank, all {@code /api/system/**} requests must present this value as
   * {@code Authorization: Bearer <token>} or header {@code X-System-Admin-Token}.
   */
  private String systemAdminToken = "";

  private Jwt jwt = new Jwt();

  private Auth auth = new Auth();

  /** Dev-only credentials for {@code POST /api/auth/login} when {@link Jwt#getSecret()} is set. */
  private List<DevUser> devUsers = new ArrayList<>();

  public String getTimeZone() {
    return timeZone;
  }

  public void setTimeZone(String timeZone) {
    this.timeZone = timeZone;
  }

  public String getSystemAdminToken() {
    return systemAdminToken;
  }

  public void setSystemAdminToken(String systemAdminToken) {
    this.systemAdminToken = systemAdminToken;
  }

  public Jwt getJwt() {
    return jwt;
  }

  public void setJwt(Jwt jwt) {
    this.jwt = jwt;
  }

  public List<DevUser> getDevUsers() {
    return devUsers;
  }

  public void setDevUsers(List<DevUser> devUsers) {
    this.devUsers = devUsers;
  }

  public Auth getAuth() {
    return auth;
  }

  public void setAuth(Auth auth) {
    this.auth = auth;
  }

  public static class Auth {
    private Login login = new Login();
    /**
     * Dev safety valve: when true (and only with spring profile {@code dev}), startup logs bootstrap
     * usernames/passwords from {@code booking.platform.auth.bootstrap-*}. Keep false in normal use.
     */
    private boolean logDevBootstrapCredentials = false;

    private BootstrapDefaultMerchant bootstrapDefaultMerchant = new BootstrapDefaultMerchant();
    private BootstrapDefaultMerchantUser bootstrapDefaultMerchantUser = new BootstrapDefaultMerchantUser();
    private BootstrapSystemAdmin bootstrapSystemAdmin = new BootstrapSystemAdmin();
    private BootstrapDefaultClient bootstrapDefaultClient = new BootstrapDefaultClient();

    public Login getLogin() {
      return login;
    }

    public void setLogin(Login login) {
      this.login = login;
    }

    public boolean isLogDevBootstrapCredentials() {
      return logDevBootstrapCredentials;
    }

    public void setLogDevBootstrapCredentials(boolean logDevBootstrapCredentials) {
      this.logDevBootstrapCredentials = logDevBootstrapCredentials;
    }

    public BootstrapDefaultMerchant getBootstrapDefaultMerchant() {
      return bootstrapDefaultMerchant;
    }

    public void setBootstrapDefaultMerchant(BootstrapDefaultMerchant bootstrapDefaultMerchant) {
      this.bootstrapDefaultMerchant = bootstrapDefaultMerchant;
    }

    public BootstrapDefaultMerchantUser getBootstrapDefaultMerchantUser() {
      return bootstrapDefaultMerchantUser;
    }

    public void setBootstrapDefaultMerchantUser(BootstrapDefaultMerchantUser bootstrapDefaultMerchantUser) {
      this.bootstrapDefaultMerchantUser = bootstrapDefaultMerchantUser;
    }

    public BootstrapSystemAdmin getBootstrapSystemAdmin() {
      return bootstrapSystemAdmin;
    }

    public void setBootstrapSystemAdmin(BootstrapSystemAdmin bootstrapSystemAdmin) {
      this.bootstrapSystemAdmin = bootstrapSystemAdmin;
    }

    public BootstrapDefaultClient getBootstrapDefaultClient() {
      return bootstrapDefaultClient;
    }

    public void setBootstrapDefaultClient(BootstrapDefaultClient bootstrapDefaultClient) {
      this.bootstrapDefaultClient = bootstrapDefaultClient;
    }

    public static class BootstrapDefaultMerchant {
      private boolean enabled = false;
      private String name = "";
      private String slug = "";

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public String getName() {
        return name;
      }

      public void setName(String name) {
        this.name = name;
      }

      public String getSlug() {
        return slug;
      }

      public void setSlug(String slug) {
        this.slug = slug;
      }
    }

    public static class BootstrapDefaultMerchantUser {
      private boolean enabled = false;
      private String username = "";
      private String password = "";
      /** When blank and default-merchant bootstrap is on, uses that merchant's slug. */
      private String merchantSlug = "";

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public String getUsername() {
        return username;
      }

      public void setUsername(String username) {
        this.username = username;
      }

      public String getPassword() {
        return password;
      }

      public void setPassword(String password) {
        this.password = password;
      }

      public String getMerchantSlug() {
        return merchantSlug;
      }

      public void setMerchantSlug(String merchantSlug) {
        this.merchantSlug = merchantSlug;
      }
    }

    public static class BootstrapSystemAdmin {
      private boolean enabled = false;
      private String username = "";
      private String password = "";

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public String getUsername() {
        return username;
      }

      public void setUsername(String username) {
        this.username = username;
      }

      public String getPassword() {
        return password;
      }

      public void setPassword(String password) {
        this.password = password;
      }
    }

    public static class BootstrapDefaultClient {
      private boolean enabled = false;
      private String username = "";
      private String password = "";

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public String getUsername() {
        return username;
      }

      public void setUsername(String username) {
        this.username = username;
      }

      public String getPassword() {
        return password;
      }

      public void setPassword(String password) {
        this.password = password;
      }
    }

    /** Rate limits and related knobs for {@code POST /api/auth/login}. */
    public static class Login {
      private boolean rateLimitEnabled = true;
      private int maxAttemptsPerIpPerWindow = 60;
      private int windowMinutes = 10;
      /**
       * When true, use the first {@code X-Forwarded-For} hop as client IP (only safe behind a trusted
       * proxy that strips/spoof-proofs this header).
       */
      private boolean trustXForwardedFor = false;

      /** When true, lock {@code platform_users} after repeated wrong passwords (same generic error as invalid login). */
      private boolean lockoutEnabled = true;

      private int maxFailedAttemptsPerUser = 5;
      private int failureWindowMinutes = 15;
      private int lockoutDurationMinutes = 15;

      public boolean isRateLimitEnabled() {
        return rateLimitEnabled;
      }

      public void setRateLimitEnabled(boolean rateLimitEnabled) {
        this.rateLimitEnabled = rateLimitEnabled;
      }

      public int getMaxAttemptsPerIpPerWindow() {
        return maxAttemptsPerIpPerWindow;
      }

      public void setMaxAttemptsPerIpPerWindow(int maxAttemptsPerIpPerWindow) {
        this.maxAttemptsPerIpPerWindow = maxAttemptsPerIpPerWindow;
      }

      public int getWindowMinutes() {
        return windowMinutes;
      }

      public void setWindowMinutes(int windowMinutes) {
        this.windowMinutes = windowMinutes;
      }

      public boolean isTrustXForwardedFor() {
        return trustXForwardedFor;
      }

      public void setTrustXForwardedFor(boolean trustXForwardedFor) {
        this.trustXForwardedFor = trustXForwardedFor;
      }

      public boolean isLockoutEnabled() {
        return lockoutEnabled;
      }

      public void setLockoutEnabled(boolean lockoutEnabled) {
        this.lockoutEnabled = lockoutEnabled;
      }

      public int getMaxFailedAttemptsPerUser() {
        return maxFailedAttemptsPerUser;
      }

      public void setMaxFailedAttemptsPerUser(int maxFailedAttemptsPerUser) {
        this.maxFailedAttemptsPerUser = maxFailedAttemptsPerUser;
      }

      public int getFailureWindowMinutes() {
        return failureWindowMinutes;
      }

      public void setFailureWindowMinutes(int failureWindowMinutes) {
        this.failureWindowMinutes = failureWindowMinutes;
      }

      public int getLockoutDurationMinutes() {
        return lockoutDurationMinutes;
      }

      public void setLockoutDurationMinutes(int lockoutDurationMinutes) {
        this.lockoutDurationMinutes = lockoutDurationMinutes;
      }
    }
  }

  public static class Jwt {
    /**
     * HS256 signing key (at least 256 bits). When blank, JWT enforcement is off: APIs stay open except
     * optional static {@link BookingPlatformProperties#systemAdminToken} on {@code /api/system/**}.
     */
    private String secret = "";

    private long expirationSeconds = 86400;

    public String getSecret() {
      return secret;
    }

    public void setSecret(String secret) {
      this.secret = secret;
    }

    public long getExpirationSeconds() {
      return expirationSeconds;
    }

    public void setExpirationSeconds(long expirationSeconds) {
      this.expirationSeconds = expirationSeconds;
    }
  }

  public static class DevUser {
    private String username = "";
    private String password = "";
    private PlatformUserRole role = PlatformUserRole.MERCHANT;
    /**
     * Optional merchant scope for MERCHANT/SUB_MERCHANT tokens.
     * When null, backend may auto-resolve a default merchant for local dev.
     */
    private Long merchantId;

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public PlatformUserRole getRole() {
      return role;
    }

    public void setRole(PlatformUserRole role) {
      this.role = role;
    }

    public Long getMerchantId() {
      return merchantId;
    }

    public void setMerchantId(Long merchantId) {
      this.merchantId = merchantId;
    }
  }
}
