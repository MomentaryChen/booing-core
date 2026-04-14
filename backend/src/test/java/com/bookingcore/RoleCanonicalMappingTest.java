package com.bookingcore;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookingcore.security.PlatformUserRole;
import com.bookingcore.security.RolePermissionCatalog;
import org.junit.jupiter.api.Test;

class RoleCanonicalMappingTest {

  private final RolePermissionCatalog catalog = new RolePermissionCatalog();

  @Test
  void platformRoleShouldExposeStableCanonicalCode() {
    assertThat(PlatformUserRole.SYSTEM_ADMIN.canonicalCode()).isEqualTo("SYSTEM_ADMIN");
    assertThat(PlatformUserRole.MERCHANT.canonicalCode()).isEqualTo("MERCHANT_OWNER");
    assertThat(PlatformUserRole.SUB_MERCHANT.canonicalCode()).isEqualTo("MERCHANT_STAFF");
    assertThat(PlatformUserRole.CLIENT.canonicalCode()).isEqualTo("CLIENT_USER");
  }

  @Test
  void canonicalPermissionLookupShouldMatchLegacyRoleLookup() {
    for (PlatformUserRole role : PlatformUserRole.values()) {
      assertThat(catalog.permissionsForCanonicalRole(role.canonicalCode()))
          .containsExactlyInAnyOrderElementsOf(catalog.permissionsFor(role));
    }
  }

  @Test
  void parserShouldAcceptLegacyAndCanonicalRoleCodes() {
    assertThat(PlatformUserRole.parse("CLIENT")).contains(PlatformUserRole.CLIENT);
    assertThat(PlatformUserRole.parse("CLIENT_USER")).contains(PlatformUserRole.CLIENT);
    assertThat(PlatformUserRole.parse("MERCHANT")).contains(PlatformUserRole.MERCHANT);
    assertThat(PlatformUserRole.parse("MERCHANT_OWNER")).contains(PlatformUserRole.MERCHANT);
    assertThat(PlatformUserRole.parse("SUB_MERCHANT")).contains(PlatformUserRole.SUB_MERCHANT);
    assertThat(PlatformUserRole.parse("MERCHANT_STAFF")).contains(PlatformUserRole.SUB_MERCHANT);
    assertThat(PlatformUserRole.parse("SYSTEM_ADMIN")).contains(PlatformUserRole.SYSTEM_ADMIN);
    assertThat(PlatformUserRole.parse("UNKNOWN_ROLE")).isEmpty();
  }

  @Test
  void merchantOwnerAndMerchantStaffBaselineCapabilitiesShouldBeExplicit() {
    assertThat(catalog.permissionsForCanonicalRole("MERCHANT_OWNER"))
        .containsExactlyInAnyOrder("merchant.portal.access", "me.navigation.read");
    assertThat(catalog.permissionsForCanonicalRole("MERCHANT_STAFF"))
        .containsExactlyInAnyOrder("merchant.portal.access", "me.navigation.read");
    assertThat(catalog.permissionsForCanonicalRole("CLIENT_USER"))
        .containsExactlyInAnyOrder("client.portal.access", "me.navigation.read");
  }
}
