package org.sputnik.ratelimit.domain;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CanDoResponse {

  private final boolean canDo;
  private final long waitMillis;
  private final Reason reason;
  private final long eventsIntents;

  public enum Reason {INVALID_REQUEST, TOO_MANY_EVENTS}
}
