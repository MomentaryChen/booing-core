package com.bookingcore;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookingcore.security.PermissionAuthorizer;
import com.bookingcore.security.RolePermissionCatalog;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class PermissionAuthorizerTest {

  private final PermissionAuthorizer authorizer = new PermissionAuthorizer(new RolePermissionCatalog());

  @Test
  void unknownRoleAuthorityShouldBeDeniedInsteadOfThrowing() {
    var auth =
        new UsernamePasswordAuthenticationToken(
            "tester", null, List.of(new SimpleGrantedAuthority("ROLE_UNKNOWN")));

    boolean allowed = authorizer.hasPermission(auth, "system.dashboard.read");

    assertThat(allowed).isFalse();
  }

  @Test
  void anyMappedRoleShouldGrantPermissionForMultiRolePrincipal() {
    var auth =
        new UsernamePasswordAuthenticationToken(
            "tester",
            null,
            List.of(
                new SimpleGrantedAuthority("ROLE_UNKNOWN"),
                new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN")));

    boolean allowed = authorizer.hasPermission(auth, "system.dashboard.read");

    assertThat(allowed).isTrue();
  }

  @Test
  void systemNamespacePermissionShouldMatchBaselineCatalog() {
    var auth =
        new UsernamePasswordAuthenticationToken(
            "tester", null, List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN")));

    assertThat(authorizer.hasSystemNamespacePermission(auth)).isTrue();
  }

  @Test
  void merchantShouldNotHaveSystemNamespacePermission() {
    var auth =
        new UsernamePasswordAuthenticationToken(
            "merchant", null, List.of(new SimpleGrantedAuthority("ROLE_MERCHANT")));

    assertThat(authorizer.hasSystemNamespacePermission(auth)).isFalse();
  }

  @Test
  void hasAnyPermissionShouldDenyForNullOrEmptyPermissions() {
    var auth =
        new UsernamePasswordAuthenticationToken(
            "tester", null, List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN")));

    assertThat(authorizer.hasAnyPermission(auth, null)).isFalse();
    assertThat(authorizer.hasAnyPermission(auth, List.of())).isFalse();
  }
}
