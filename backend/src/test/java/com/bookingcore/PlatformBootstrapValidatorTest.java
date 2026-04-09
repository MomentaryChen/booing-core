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
  void rejectsCredentialLoggingOutsideDev() {
    BookingPlatformProperties props = new BookingPlatformProperties();
    props.getAuth().setLogDevBootstrapCredentials(true);
    Environment env = mock(Environment.class);
    when(env.acceptsProfiles(Profiles.of("dev"))).thenReturn(false);

    assertThatThrownBy(() -> PlatformBootstrapValidator.validate(props, env))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("log-dev-bootstrap-credentials");
  }

  @Test
  void prodRejectsDevOnlyBootstrapPasswords() {
    BookingPlatformProperties props = new BookingPlatformProperties();
    props.getAuth().getBootstrapSystemAdmin().setEnabled(true);
    props.getAuth().getBootstrapSystemAdmin().setUsername("admin");
    props.getAuth().getBootstrapSystemAdmin().setPassword("admin");
    Environment env = mock(Environment.class);
    when(env.acceptsProfiles(Profiles.of("dev"))).thenReturn(false);
    when(env.acceptsProfiles(Profiles.of("prod"))).thenReturn(true);

    assertThatThrownBy(() -> PlatformBootstrapValidator.validate(props, env))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("dev-only literals");
  }

  @Test
  void merchantUserRequiresMerchantSlugWhenMerchantBootstrapOff() {
    BookingPlatformProperties props = new BookingPlatformProperties();
    props.getAuth().getBootstrapDefaultMerchantUser().setEnabled(true);
    props.getAuth().getBootstrapDefaultMerchantUser().setUsername("m");
    props.getAuth().getBootstrapDefaultMerchantUser().setPassword("p");
    props.getAuth().getBootstrapDefaultMerchantUser().setMerchantSlug("");
    props.getAuth().getBootstrapDefaultMerchant().setEnabled(false);
    Environment env = mock(Environment.class);
    when(env.acceptsProfiles(Profiles.of("prod"))).thenReturn(false);

    assertThatThrownBy(() -> PlatformBootstrapValidator.validate(props, env))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("merchant-slug is required");
  }

  @Test
  void prodBootstrapEnabledButPasswordMissingFailsFast() {
    BookingPlatformProperties props = new BookingPlatformProperties();
    props.getAuth().getBootstrapSystemAdmin().setEnabled(true);
    props.getAuth().getBootstrapSystemAdmin().setUsername("admin-prod");
    props.getAuth().getBootstrapSystemAdmin().setPassword("   ");
    Environment env = mock(Environment.class);
    when(env.acceptsProfiles(Profiles.of("dev"))).thenReturn(false);
    when(env.acceptsProfiles(Profiles.of("prod"))).thenReturn(true);

    assertThatThrownBy(() -> PlatformBootstrapValidator.validate(props, env))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Missing required configuration")
        .hasMessageContaining("bootstrap-system-admin.password");
  }
}
