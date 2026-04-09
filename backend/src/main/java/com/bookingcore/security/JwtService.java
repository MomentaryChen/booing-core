package com.bookingcore.security;

import com.bookingcore.config.BookingPlatformProperties;
import com.bookingcore.modules.platform.PlatformUserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private static final String ROLE_CLAIM = "role";
  private static final String MERCHANT_ID_CLAIM = "merchantId";
  private static final String CREDENTIAL_VERSION_CLAIM = "cv";

  private final BookingPlatformProperties properties;
  private final PlatformUserRepository platformUserRepository;

  public JwtService(BookingPlatformProperties properties, PlatformUserRepository platformUserRepository) {
    this.properties = properties;
    this.platformUserRepository = platformUserRepository;
  }

  public String createAccessToken(String subject, PlatformUserRole role, Long merchantId) {
    return createAccessToken(subject, role, merchantId, null);
  }

  /**
   * @param credentialVersion when non-null, embedded in JWT; required to match {@code platform_users} row
   *     on each request (logout bumps version to revoke outstanding access tokens). Omit for dev-only
   *     accounts that are not stored in {@code platform_users}.
   */
  public String createAccessToken(
      String subject, PlatformUserRole role, Long merchantId, Integer credentialVersion) {
    long expSec = properties.getJwt().getExpirationSeconds();
    Date now = new Date();
    Date exp = new Date(now.getTime() + expSec * 1000);
    var builder = Jwts.builder()
        .subject(subject)
        .claim(ROLE_CLAIM, role.name())
        .issuedAt(now)
        .expiration(exp)
        .signWith(signingKey());
    if (merchantId != null) {
      builder.claim(MERCHANT_ID_CLAIM, merchantId);
    }
    if (credentialVersion != null) {
      builder.claim(CREDENTIAL_VERSION_CLAIM, credentialVersion);
    }
    return builder.compact();
  }

  public Authentication parseToken(String token) {
    Claims claims = Jwts.parser()
        .verifyWith(signingKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
    String subject = claims.getSubject();
    String roleName = claims.get(ROLE_CLAIM, String.class);
    if (subject == null || roleName == null) {
      throw new IllegalArgumentException("Invalid token payload");
    }
    platformUserRepository
        .findByUsername(subject)
        .ifPresent(
            user -> {
              Object rawCv = claims.get(CREDENTIAL_VERSION_CLAIM);
              if (rawCv == null) {
                throw new JwtException("Token missing credential version");
              }
              int tokenCv;
              if (rawCv instanceof Number n) {
                tokenCv = n.intValue();
              } else {
                try {
                  tokenCv = Integer.parseInt(String.valueOf(rawCv));
                } catch (NumberFormatException ex) {
                  throw new JwtException("Invalid credential version");
                }
              }
              if (tokenCv != user.getCredentialVersion()) {
                throw new JwtException("Token revoked");
              }
            });
    PlatformUserRole role = PlatformUserRole.valueOf(roleName);
    Long merchantId = null;
    Object rawMerchantId = claims.get(MERCHANT_ID_CLAIM);
    if (rawMerchantId instanceof Number n) {
      merchantId = n.longValue();
    } else if (rawMerchantId instanceof String s) {
      try {
        merchantId = Long.parseLong(s);
      } catch (NumberFormatException ignored) {
        merchantId = null;
      }
    }
    return new UsernamePasswordAuthenticationToken(
        new PlatformPrincipal(subject, role, merchantId),
        null,
        List.of(new SimpleGrantedAuthority(role.authority())));
  }

  private SecretKey signingKey() {
    byte[] bytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(bytes);
  }
}
