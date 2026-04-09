package com.bookingcore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bookingcore.config.BookingPlatformProperties;
import com.bookingcore.config.PlatformBootstrapRunner;
import com.bookingcore.service.PlatformBootstrapService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

class PlatformBootstrapRunnerLoggingTest {

  @Test
  void credentialLoggingOffDoesNotPrintPlaintextCredentials() throws Exception {
    BookingPlatformProperties properties = new BookingPlatformProperties();
    properties.getAuth().setLogDevBootstrapCredentials(false);
    properties.getAuth().getBootstrapSystemAdmin().setEnabled(true);
    properties.getAuth().getBootstrapSystemAdmin().setUsername("admin-off");
    properties.getAuth().getBootstrapSystemAdmin().setPassword("plain-secret-off");
    Environment env = mock(Environment.class);
    when(env.acceptsProfiles(Profiles.of("dev"))).thenReturn(true);
    when(env.acceptsProfiles(Profiles.of("prod"))).thenReturn(false);
    PlatformBootstrapService service = mock(PlatformBootstrapService.class);
    PlatformBootstrapRunner runner = new PlatformBootstrapRunner(properties, env, service);

    ListAppender<ILoggingEvent> appender = attachAppender();
    runner.run(new DefaultApplicationArguments(new String[0]));
    List<String> logs = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();

    assertThat(logs).noneMatch(line -> line.contains("plain-secret-off"));
    assertThat(logs)
        .noneMatch(line -> line.contains("DEV ONLY: printing bootstrap credentials from configuration"));
  }

  @Test
  void devAndCredentialLoggingOnPrintsCredentialsWithWarning() throws Exception {
    BookingPlatformProperties properties = new BookingPlatformProperties();
    properties.getAuth().setLogDevBootstrapCredentials(true);
    properties.getAuth().getBootstrapSystemAdmin().setEnabled(true);
    properties.getAuth().getBootstrapSystemAdmin().setUsername("admin-on");
    properties.getAuth().getBootstrapSystemAdmin().setPassword("plain-secret-on");
    Environment env = mock(Environment.class);
    when(env.acceptsProfiles(Profiles.of("dev"))).thenReturn(true);
    when(env.acceptsProfiles(Profiles.of("prod"))).thenReturn(false);
    PlatformBootstrapService service = mock(PlatformBootstrapService.class);
    PlatformBootstrapRunner runner = new PlatformBootstrapRunner(properties, env, service);

    ListAppender<ILoggingEvent> appender = attachAppender();
    runner.run(new DefaultApplicationArguments(new String[0]));
    List<String> logs = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();

    assertThat(logs)
        .anyMatch(line -> line.contains("DEV ONLY: printing bootstrap credentials from configuration"));
    assertThat(logs)
        .anyMatch(
            line ->
                line.contains("DEV DEFAULT SYSTEM_ADMIN")
                    && line.contains("admin-on")
                    && line.contains("plain-secret-on"));
  }

  private static ListAppender<ILoggingEvent> attachAppender() {
    Logger logger = (Logger) LoggerFactory.getLogger(PlatformBootstrapRunner.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.setName("bootstrap-test-appender");
    appender.start();
    logger.setLevel(Level.WARN);
    logger.addAppender(appender);
    return appender;
  }
}
