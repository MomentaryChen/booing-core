package com.bookingcore.modules.platform.rbac;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlatformUserRbacBindingRepository extends JpaRepository<PlatformUserRbacBinding, Long> {

  @Query(
      """
      select count(b) > 0 from PlatformUserRbacBinding b
      join b.rbacRole r
      where b.platformUser.id = :userId
        and b.status = 'ACTIVE'
        and r.code = :roleCode
        and ((:merchantId is null and b.merchant is null)
          or (b.merchant is not null and b.merchant.id = :merchantId))
      """)
  boolean existsActiveBindingForContext(
      @Param("userId") Long userId,
      @Param("roleCode") String roleCode,
      @Param("merchantId") Long merchantId);

  @Query(
      """
      select distinct p.code from PlatformUserRbacBinding b
      join b.rbacRole r
      join r.permissions p
      where b.platformUser.id = :userId
        and b.status = 'ACTIVE'
        and r.code = :roleCode
        and ((:merchantId is null and b.merchant is null)
          or (b.merchant is not null and b.merchant.id = :merchantId))
      """)
  List<String> findPermissionCodesForUserContext(
      @Param("userId") Long userId,
      @Param("roleCode") String roleCode,
      @Param("merchantId") Long merchantId);

  @Query(
      """
      select b from PlatformUserRbacBinding b
      join b.rbacRole r
      where b.platformUser.id = :userId
        and r.code = :roleCode
        and ((:merchantId is null and b.merchant is null)
          or (b.merchant is not null and b.merchant.id = :merchantId))
      order by b.id asc
      """)
  List<PlatformUserRbacBinding> findBindingsForUserContext(
      @Param("userId") Long userId,
      @Param("roleCode") String roleCode,
      @Param("merchantId") Long merchantId);

  @Query(
      """
      select count(b) from PlatformUserRbacBinding b
      join b.rbacRole r
      where b.platformUser.id = :userId
        and b.status = 'ACTIVE'
        and r.code = :roleCode
        and ((:merchantId is null and b.merchant is null)
          or (b.merchant is not null and b.merchant.id = :merchantId))
      """)
  long countActiveBindingsForContext(
      @Param("userId") Long userId,
      @Param("roleCode") String roleCode,
      @Param("merchantId") Long merchantId);
}
