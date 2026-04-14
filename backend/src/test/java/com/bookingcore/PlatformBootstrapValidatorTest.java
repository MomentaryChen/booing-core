package com.bookingcore;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bookingcore.config.BookingPlatformProperties;
import com.bookingcore.config.PlatformBootstrapValidator;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

class PlatformBootstrapValidatorTest {

  @Test
  void internalSystemAdminAutoProvisionRequiresUsername() {
    BookingPlatformProperties props = new BookingPlatformProperties();
    props.getAuth().getInternalSystemAdmin().setAutoProvision(true);
    props.getAuth().getInternalSystemAdmin().setUsername(" ");
    props.getAuth().getInternalSystemAdmin().setPassword("x");
    Environment env = mock(Environment.class);
    when(env.acceptsProfiles(Profiles.of("prod"))).thenReturn(false);

    assertThatThrownBy(() -> PlatformBootstrapValidator.validate(props, env))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("internal-system-admin.username");
  }

  @Test
  void internalSystemAdminAutoProvisionRequiresPassword() {
    BookingPlatformProperties props = new BookingPlatformProperties();
    props.getAuth().getInternalSystemAdmin().setAutoProvision(true);
    props.getAuth().getInternalSystemAdmin().setUsername("admin");
    props.getAuth().getInternalSystemAdmin().setPassword(" ");
    Environment env = mock(Environment.class);
    when(env.acceptsProfiles(Profiles.of("prod"))).thenReturn(false);

    assertThatThrownBy(() -> PlatformBootstrapValidator.validate(props, env))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("internal-system-admin.password");
  }

  @Test
  void prodRejectsDevOnlyInternalSystemAdminPassword() {
    BookingPlatformProperties props = new BookingPlatformProperties();
    props.getAuth().getInternalSystemAdmin().setAutoProvision(true);
    props.getAuth().getInternalSystemAdmin().setUsername("admin");
    props.getAuth().getInternalSystemAdmin().setPassword("admin");
    Environment env = mock(Environment.class);
    when(env.acceptsProfiles(Profiles.of("prod"))).thenReturn(true);

    assertThatThrownBy(() -> PlatformBootstrapValidator.validate(props, env))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("dev-only literals");
  }
}
