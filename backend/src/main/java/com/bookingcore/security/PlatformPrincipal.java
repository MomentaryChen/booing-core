package com.bookingcore.security;

import java.util.UUID;

public record PlatformPrincipal(
    String username,
    PlatformUserRole role,
    UUID merchantId) {}

