package com.bookingcore;

import com.bookingcore.config.BookingPlatformProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableConfigurationProperties(BookingPlatformProperties.class)
@EnableCaching
public class BookingCoreApplication {
  public static void main(String[] args) {
    SpringApplication.run(BookingCoreApplication.class, args);
  }
}
