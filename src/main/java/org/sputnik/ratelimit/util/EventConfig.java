package org.sputnik.ratelimit.util;

import java.time.Duration;
import java.util.Objects;

/**
 * Immutable event configuration. maxIntents: max events allowed inside minTime sliding window.
 */
public record EventConfig(String eventId, long maxIntents, Duration minTime) {

  public EventConfig {
    if (eventId == null || eventId.isBlank()) {
      throw new IllegalArgumentException("eventId must not be blank");
    }
    if (maxIntents <= 0) {
      throw new IllegalArgumentException("maxIntents must be > 0");
    }
    Objects.requireNonNull(minTime, "minTime");
    if (minTime.isZero() || minTime.isNegative()) {
      throw new IllegalArgumentException("minTime must be positive");
    }
  }
}