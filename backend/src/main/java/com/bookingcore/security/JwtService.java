package com.bookingcore.security;

import com.bookingcore.config.BookingPlatformProperties;
import io.jsonwebtoken.Claims;
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

  private final BookingPlatformProperties properties;

  public JwtService(BookingPlatformProperties properties) {
    this.properties = properties;
  }

  public String createAccessToken(String subject, PlatformUserRole role, Long merchantId) {
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
