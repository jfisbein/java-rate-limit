package org.sputnik.ratelimit.domain;

public record CanDoResponse(boolean canDo, long waitMillis, Reason reason, long eventsIntents) {

  public enum Reason {INVALID_REQUEST, TOO_MANY_EVENTS}

  public static CanDoResponse success(long currentIntents) {
    return new CanDoResponse(true, 0, null, currentIntents);
  }

  public static CanDoResponse invalidRequest() {
    return new CanDoResponse(false, 0, Reason.INVALID_REQUEST, 0);
  }

  public static CanDoResponse tooMany(long waitMillis, long intents) {
    return new CanDoResponse(false, waitMillis, Reason.TOO_MANY_EVENTS, intents);
  }
}
