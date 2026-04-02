package com.bookingcore.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/** Servlet-level {@link SystemAdminTokenFilter} is used only when JWT enforcement is off. */
public class JwtDisabledCondition implements Condition {

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    String secret = context.getEnvironment().getProperty("booking.platform.jwt.secret", "");
    return !StringUtils.hasText(secret);
  }
}
