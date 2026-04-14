package com.bookingcore.modules.platform;

import com.bookingcore.security.PlatformUserRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformUserRepository extends JpaRepository<PlatformUser, Long> {
  Optional<PlatformUser> findByUsername(String username);
  Optional<PlatformUser> findByUsernameIgnoreCase(String username);

  boolean existsByRole(PlatformUserRole role);
}

