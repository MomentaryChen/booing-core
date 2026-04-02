package com.bookingcore.service;

import com.bookingcore.api.ApiDtos.LoginRequest;
import com.bookingcore.api.ApiDtos.TokenResponse;
import com.bookingcore.common.ApiException;
import com.bookingcore.config.BookingPlatformProperties;
import com.bookingcore.config.BookingPlatformProperties.DevUser;
import com.bookingcore.modules.merchant.MerchantRepository;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.platform.PlatformUserRepository;
import com.bookingcore.security.JwtService;
import com.bookingcore.security.PlatformUserRole;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class AuthService {

  private final BookingPlatformProperties properties;
  private final JwtService jwtService;
  private final MerchantRepository merchantRepository;
  private final PlatformUserRepository platformUserRepository;
  private final PasswordEncoder passwordEncoder;

  public AuthService(
      BookingPlatformProperties properties,
      JwtService jwtService,
      MerchantRepository merchantRepository,
      PlatformUserRepository platformUserRepository,
      PasswordEncoder passwordEncoder) {
    this.properties = properties;
    this.jwtService = jwtService;
    this.merchantRepository = merchantRepository;
    this.platformUserRepository = platformUserRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public TokenResponse login(LoginRequest request) {
    if (!StringUtils.hasText(properties.getJwt().getSecret())) {
      throw new ApiException("JWT auth is not enabled (set booking.platform.jwt.secret)", HttpStatus.BAD_REQUEST);
    }

    PlatformUser dbUser = platformUserRepository.findByUsername(request.username()).orElse(null);
    if (dbUser != null) {
      if (!Boolean.TRUE.equals(dbUser.getEnabled())) {
        throw new ApiException("User is disabled", HttpStatus.UNAUTHORIZED);
      }
      if (passwordEncoder.matches(request.password(), dbUser.getPasswordHash())) {
        Long merchantId = dbUser.getMerchant() == null ? null : dbUser.getMerchant().getId();
        String token = jwtService.createAccessToken(dbUser.getUsername(), dbUser.getRole(), merchantId);
        dbUser.setLastLoginAt(LocalDateTime.now());
        platformUserRepository.save(dbUser);
        return new TokenResponse(
            token,
            "Bearer",
            properties.getJwt().getExpirationSeconds(),
            dbUser.getRole().name());
      }
      throw new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED);
    }

    // Backward-compatible fallback for config-driven dev accounts.
    for (DevUser u : properties.getDevUsers()) {
      if (request.username().equals(u.getUsername()) && request.password().equals(u.getPassword())) {
        Long merchantId = resolveMerchantId(u);
        String token = jwtService.createAccessToken(u.getUsername(), u.getRole(), merchantId);
        return new TokenResponse(
            token,
            "Bearer",
            properties.getJwt().getExpirationSeconds(),
            u.getRole().name());
      }
    }
    throw new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED);
  }

  private Long resolveMerchantId(DevUser user) {
    if (user.getRole() != PlatformUserRole.MERCHANT && user.getRole() != PlatformUserRole.SUB_MERCHANT) {
      return null;
    }
    if (user.getMerchantId() != null) {
      return user.getMerchantId();
    }
    return merchantRepository.findFirstByOrderByIdAsc()
        .map(m -> m.getId())
        .orElseThrow(() -> new ApiException(
            "No merchant is configured yet. Please register a merchant first (/merchant/register).",
            HttpStatus.BAD_REQUEST));
  }
}
