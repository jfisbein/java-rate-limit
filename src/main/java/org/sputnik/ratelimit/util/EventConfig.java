package org.sputnik.ratelimit.util;

import java.time.Duration;
import lombok.Value;

@Value
public class EventConfig {

  protected final String eventId;
  protected final long maxIntents;
  protected final Duration minTime;
}
