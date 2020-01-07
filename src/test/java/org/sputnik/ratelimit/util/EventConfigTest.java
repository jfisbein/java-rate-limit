package org.sputnik.ratelimit.util;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

public class EventConfigTest {

  @Test
  public void testEventConfig() {
    String eventId = RandomStringUtils.randomAlphabetic(10);
    long maxIntents = Long.parseLong(RandomStringUtils.randomNumeric(5));
    Duration minTime = Duration.ofSeconds(Long.parseLong(RandomStringUtils.randomNumeric(6)));

    EventConfig ec = new EventConfig(eventId, maxIntents, minTime);
    assertEquals(eventId, ec.getEventId());
    assertEquals(maxIntents, ec.getMaxIntents());
    assertEquals(minTime, ec.getMinTime());
  }
}