package org.sputnik.ratelimit.util;

import java.time.Duration;
import java.util.Objects;

/**
 * Immutable event configuration. maxAttempts: max events allowed inside minTime sliding window.
 */
public record EventConfig(String eventId, long maxAttempts, Duration minTime) {

  public EventConfig {
    if (eventId == null || eventId.isBlank()) {
      throw new IllegalArgumentException("eventId must not be blank");
    }
    if (maxAttempts <= 0) {
      throw new IllegalArgumentException("maxAttempts must be > 0");
    }
    Objects.requireNonNull(minTime, "minTime");
    if (minTime.isZero() || minTime.isNegative()) {
      throw new IllegalArgumentException("minTime must be positive");
    }
  }
}
