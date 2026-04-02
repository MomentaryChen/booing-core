package com.bookingcore.security;

public record PlatformPrincipal(
    String username,
    PlatformUserRole role,
    Long merchantId) {}

