package com.bookingcore.service;

import com.bookingcore.config.BookingPlatformProperties;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.platform.PlatformUserRepository;
import com.bookingcore.security.PlatformUserRole;
import java.util.Locale;
import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PlatformBootstrapService {

  private final BookingPlatformProperties properties;
  private final Environment environment;
  private final PlatformUserRepository platformUserRepository;
  private final PasswordEncoder passwordEncoder;

  public PlatformBootstrapService(
      BookingPlatformProperties properties,
      Environment environment,
      PlatformUserRepository platformUserRepository,
      PasswordEncoder passwordEncoder) {
    this.properties = properties;
    this.environment = environment;
    this.platformUserRepository = platformUserRepository;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Ensures at least one {@link PlatformUserRole#SYSTEM_ADMIN} exists when configured. Runs before
   * optional demo bootstrap; skipped when any SYSTEM_ADMIN row already exists (including from manual
   * SQL).
   */
  @Transactional
  public void provisionInternalSystemAdminIfAbsent() {
    var cfg = properties.getAuth().getInternalSystemAdmin();
    if (!cfg.isAutoProvision()) {
      return;
    }
    if (platformUserRepository.existsByRole(PlatformUserRole.SYSTEM_ADMIN)) {
      return;
    }
    String username = cfg.getUsername() == null ? "" : cfg.getUsername().trim();
    String password = cfg.getPassword() == null ? "" : cfg.getPassword();
    if (!StringUtils.hasText(username)) {
      throw new IllegalStateException(
          "internal-system-admin.auto-provision is true but internal-system-admin.username is blank");
    }
    if (!StringUtils.hasText(password)) {
      throw new IllegalStateException(
          "internal-system-admin.auto-provision is true and no SYSTEM_ADMIN platform user exists yet, "
              + "but internal-system-admin.password is blank; set a password or insert an admin manually");
    }
    if (environment.acceptsProfiles(Profiles.of("prod")) && looksLikeDevOnlyPassword(password)) {
      throw new IllegalStateException(
          "internal-system-admin.password must not use dev-only literals when spring profile 'prod' is active");
    }
    ensurePlatformUser(PlatformUserRole.SYSTEM_ADMIN, username, password);
  }

  private void ensurePlatformUser(PlatformUserRole role, String username, String password) {
    Optional<PlatformUser> existing = platformUserRepository.findByUsername(username);
    if (existing.isPresent()) {
      PlatformUser user = existing.get();
      validateExistingUserRole(user, role);
      return;
    }
    PlatformUser user = new PlatformUser();
    user.setUsername(username);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setRole(role);
    user.setEnabled(true);
    user.setMerchant(null);
    platformUserRepository.save(user);
  }

  private void validateExistingUserRole(PlatformUser user, PlatformUserRole expectedRole) {
    if (user.getRole() != expectedRole) {
      throw new IllegalStateException(
          "Bootstrap user conflict: username '"
              + user.getUsername()
              + "' role mismatch (actual="
              + user.getRole()
              + ", expected="
              + expectedRole
              + ")");
    }
  }

  private static boolean looksLikeDevOnlyPassword(String password) {
    String p = password.trim().toLowerCase(Locale.ROOT);
    return p.equals("admin") || p.equals("merchant") || p.equals("client");
  }
}
