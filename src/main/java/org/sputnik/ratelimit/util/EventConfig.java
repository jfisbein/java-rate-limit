package org.sputnik.ratelimit.util;

import lombok.Value;

import java.time.Duration;

@Value
public class EventConfig {

  String eventId;
  long maxIntents;
  Duration minTime;
}
