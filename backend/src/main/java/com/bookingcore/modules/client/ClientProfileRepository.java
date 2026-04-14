package com.bookingcore.modules.client;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientProfileRepository extends JpaRepository<ClientProfile, Long> {
  Optional<ClientProfile> findByPlatformUserId(Long platformUserId);
}
