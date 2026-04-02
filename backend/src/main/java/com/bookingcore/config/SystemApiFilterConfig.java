package com.bookingcore.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@Conditional(JwtDisabledCondition.class)
public class SystemApiFilterConfig {

  @Bean
  public FilterRegistrationBean<SystemAdminTokenFilter> systemAdminTokenFilter(
      BookingPlatformProperties properties) {
    FilterRegistrationBean<SystemAdminTokenFilter> reg = new FilterRegistrationBean<>();
    reg.setFilter(new SystemAdminTokenFilter(properties));
    reg.addUrlPatterns("/api/system/*");
    reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return reg;
  }
}
