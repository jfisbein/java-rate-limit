package org.sputnik.ratelimit.service;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CanDoResponse {

  private boolean canDo;
  private long waitMillis;
  private Reason reason;

  public enum Reason {INVALID_REQUEST, TOO_MANY_EVENTS}
}
