package com.bookingcore.modules.platform.rbac;

import java.util.List;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

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

  @Query(
      """
      select new com.bookingcore.modules.platform.rbac.ActiveAuthContext(r.code,
        case when (b.merchant is null) then null else b.merchant.id end)
      from PlatformUserRbacBinding b
      join b.rbacRole r
      where b.platformUser.id = :userId and b.status = 'ACTIVE'
      order by b.id asc
      """)
  List<ActiveAuthContext> findActiveAuthContexts(@Param("userId") Long userId);

  @Query(
      """
      select distinct b from PlatformUserRbacBinding b
      join fetch b.rbacRole r
      left join fetch r.permissions
      left join fetch b.merchant
      where b.platformUser.id = :userId
      order by b.id asc
      """)
  List<PlatformUserRbacBinding> findBindingsForUserWithRoleAndPermissions(@Param("userId") Long userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select b from PlatformUserRbacBinding b
      where b.platformUser.id = :userId
      order by b.id asc
      """)
  List<PlatformUserRbacBinding> lockBindingsForUser(@Param("userId") Long userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select b from PlatformUserRbacBinding b
      join b.platformUser u
      join b.rbacRole r
      where b.status = 'ACTIVE'
        and u.enabled = true
        and r.code = 'SYSTEM_ADMIN'
        and b.merchant is null
      order by b.id asc
      """)
  List<PlatformUserRbacBinding> lockEnabledActiveSystemAdminBindings();

  @Query(
      """
      select count(b) from PlatformUserRbacBinding b
      join b.platformUser u
      join b.rbacRole r
      where b.status = 'ACTIVE'
        and u.enabled = true
        and r.code = 'SYSTEM_ADMIN'
        and b.merchant is null
        and u.id <> :excludedUserId
      """)
  long countOtherEnabledActiveSystemAdmins(@Param("excludedUserId") Long excludedUserId);
}
