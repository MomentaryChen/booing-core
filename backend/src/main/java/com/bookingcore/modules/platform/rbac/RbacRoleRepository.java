package com.bookingcore.modules.platform.rbac;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RbacRoleRepository extends JpaRepository<RbacRole, Long> {

  Optional<RbacRole> findByCode(String code);
}
