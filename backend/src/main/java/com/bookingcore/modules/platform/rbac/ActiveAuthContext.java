package com.bookingcore.modules.platform.rbac;

/**
 * One ACTIVE {@link PlatformUserRbacBinding} resolved to role code and optional merchant scope.
 */
public record ActiveAuthContext(String roleCode, Long merchantId) {}
