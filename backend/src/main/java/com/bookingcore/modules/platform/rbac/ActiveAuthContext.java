package com.bookingcore.modules.platform.rbac;

import java.util.UUID;

/**
 * One ACTIVE {@link PlatformUserRbacBinding} resolved to role code and optional merchant scope.
 */
public record ActiveAuthContext(String roleCode, UUID merchantId) {}
