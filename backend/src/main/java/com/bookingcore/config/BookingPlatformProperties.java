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
