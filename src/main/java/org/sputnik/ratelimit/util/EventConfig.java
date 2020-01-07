package org.sputnik.ratelimit.util;

import java.time.Duration;

public class EventConfig {

  protected final String eventId;

  protected final long maxIntents;

  protected final Duration minTime;

  public EventConfig(String eventId, long maxIntents, Duration minTime) {
    this.eventId = eventId;
    this.maxIntents = maxIntents;
    this.minTime = minTime;
  }

  public String getEventId() {
    return eventId;
  }

  public long getMaxIntents() {
    return maxIntents;
  }

  public Duration getMinTime() {
    return minTime;
  }
}
