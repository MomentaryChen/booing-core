package com.bookingcore.common;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

public final class UuidUtils {
  private static final SecureRandom RANDOM = new SecureRandom();

  private UuidUtils() {}

  public static UUID uuidV7() {
    long unixTsMs = Instant.now().toEpochMilli();
    long randomA = RANDOM.nextInt(1 << 12) & 0xFFFL;
    long randomB = RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL;

    long mostSignificantBits = ((unixTsMs & 0xFFFFFFFFFFFFL) << 16) | 0x7000L | randomA;
    long leastSignificantBits = 0x8000000000000000L | randomB;
    return new UUID(mostSignificantBits, leastSignificantBits);
  }
}
