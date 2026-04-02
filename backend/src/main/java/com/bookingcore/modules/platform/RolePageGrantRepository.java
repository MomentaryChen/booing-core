package com.bookingcore.modules.platform;

import com.bookingcore.security.PlatformUserRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePageGrantRepository extends JpaRepository<RolePageGrant, Long> {

  @Query(
      "select p from PlatformPage p join RolePageGrant g on g.page.id = p.id "
          + "where g.role = :role and p.active = true order by p.sortOrder asc")
  List<PlatformPage> findPagesForRole(@Param("role") PlatformUserRole role);
}
