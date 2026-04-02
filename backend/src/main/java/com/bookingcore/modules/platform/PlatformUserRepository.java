package com.bookingcore.modules.platform;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformUserRepository extends JpaRepository<PlatformUser, Long> {
  Optional<PlatformUser> findByUsername(String username);
}

