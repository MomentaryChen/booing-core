package com.bookingcore.modules.client;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientProfileRepository extends JpaRepository<ClientProfile, Long> {
  Optional<ClientProfile> findByPlatformUserId(UUID platformUserId);
}
