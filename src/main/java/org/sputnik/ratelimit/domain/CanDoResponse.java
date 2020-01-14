package org.sputnik.ratelimit.domain;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CanDoResponse {

  private boolean canDo;
  private long waitMillis;
  private Reason reason;
  private long eventsIntents;

  public enum Reason {INVALID_REQUEST, TOO_MANY_EVENTS}
}
