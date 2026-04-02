package com.bookingcore.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

public class JwtEnabledCondition implements Condition {

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    String secret = context.getEnvironment().getProperty("booking.platform.jwt.secret", "");
    return StringUtils.hasText(secret);
  }
}
