package org.sputnik.ratelimit.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.sputnik.ratelimit.exeception.DuplicatedEventKeyException;
import org.sputnik.ratelimit.service.CanDoResponse.Reason;
import org.sputnik.ratelimit.util.EventConfig;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class RateLimiterTest {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RateLimiterTest.class);

  private static RateLimiter vcs;

  @Container
  private static final GenericContainer redis = new GenericContainer("redis:3.0.6").withExposedPorts(6379);

  @BeforeAll
  static void init() {
    List<EventConfig> eventsConfig = new ArrayList<>();
    eventsConfig.add(new EventConfig("testLogin", 3, Duration.ofSeconds(3600)));
    eventsConfig.add(new EventConfig("freeTrial", 1, Duration.ofSeconds(600)));
    eventsConfig.add(new EventConfig("maxLoginAttempts", 3, Duration.ofSeconds(6)));
    eventsConfig.add(new EventConfig("recurrenceTest", 3, Duration.ofSeconds(10)));
    eventsConfig.add(new EventConfig("logMessageTest", 3, Duration.ofSeconds(2)));

    vcs = new RateLimiter(JedisConfiguration.builder().port(redis.getMappedPort(6379)).host(redis.getContainerIpAddress()).build(),
        "hashSecret", eventsConfig.toArray(new EventConfig[0]));
  }

  @Test
  void testCanDoEventFullFlow() throws InterruptedException {
    String testEventId = "logMessageTest";
    String testKey = "my_test_key";

    // 0
    assertTrue(vcs.canDoEvent(testEventId, testKey).getCanDo());

    assertTrue(vcs.doEvent(testEventId, testKey)); // 1
    assertTrue(vcs.canDoEvent(testEventId, testKey).getCanDo());

    assertTrue(vcs.doEvent(testEventId, testKey)); // 2
    assertTrue(vcs.canDoEvent(testEventId, testKey).getCanDo());

    assertTrue(vcs.doEvent(testEventId, testKey)); // 3
    assertFalse(vcs.canDoEvent(testEventId, testKey).getCanDo());

    // Wait for the event attempts to expire
    TimeUnit.MILLISECONDS.sleep(2500);
    assertTrue(vcs.canDoEvent(testEventId, testKey).getCanDo());
  }

  @Test
  void testCanDoEventFullFlowEdgeCase() throws InterruptedException {
    String testEventId = "logMessageTest";
    String testKey = "my_test_key_2";

    // 0
    assertTrue(vcs.canDoEvent(testEventId, testKey).getCanDo());

    assertTrue(vcs.doEvent(testEventId, testKey)); // 1
    assertTrue(vcs.canDoEvent(testEventId, testKey).getCanDo());
    TimeUnit.MILLISECONDS.sleep(300);

    assertTrue(vcs.doEvent(testEventId, testKey)); // 2
    assertTrue(vcs.canDoEvent(testEventId, testKey).getCanDo());

    TimeUnit.MILLISECONDS.sleep(300);
    assertTrue(vcs.doEvent(testEventId, testKey)); // 3
    assertFalse(vcs.canDoEvent(testEventId, testKey).getCanDo());

    // Wait for the event attempts to expire
    TimeUnit.MILLISECONDS.sleep(1600);
    assertTrue(vcs.canDoEvent(testEventId, testKey).getCanDo());
  }

  @Test
  void testCanDoEventNoEventId() {
    logger.info("CanDoEventTest: no correct eventId and no empty or null key");
    assertFalse(vcs.canDoEvent("IncorrectLogin", "This is a test").getCanDo());
  }

  @Test
  void testCanDoEventCorrectEventId() {
    logger.info("CanDoEventTest: correct eventId and no empty or null key");
    assertTrue(vcs.canDoEvent("testLogin", "This is a test").getCanDo());
  }

  @Test
  void testCanDoEventNoCorrectEventIdEmptyKey() {
    logger.info("CanDoEventTest: no correct eventId and empty key");
    assertFalse(vcs.canDoEvent("IncorrectLogin", "").getCanDo());
  }

  @Test
  void testCanDoEventCorrectEventIdEmptyKey() {
    logger.info("CanDoEventTest: correct eventId and empty key");
    assertFalse(vcs.canDoEvent("testLogin", "").getCanDo());
  }

  @Test
  void testCanDoEventCorrectEventIdNullKey() {
    logger.info("CanDoEventTest: correct eventId and empty key");
    assertFalse(vcs.canDoEvent("testLogin", null).getCanDo());
  }

  @Test
  void testCanDoEventNoCorrectEventIdNullKey() {
    logger.info("CanDoEventTest: no correct eventId and empty key");
    assertFalse(vcs.canDoEvent("IncorrectLogin", null).getCanDo());
  }

  @Test
  void testDoEventNoEventId() {
    logger.info("DoEventTest: no correct eventId and no empty or null key");
    assertFalse(vcs.doEvent("IncorrectLogin", "This is a test"));
  }

  @Test
  void testDoEventCorrectEventId() {
    logger.info("DoEventTest: correct eventId and no empty or null key");
    assertTrue(vcs.doEvent("testLogin", "This is a test"));
  }

  @Test
  void testDoEventNoCorrectEventIdEmptyKey() {
    logger.info("DoEventTest: no correct eventId and empty key");
    assertFalse(vcs.doEvent("IncorrectLogin", ""));
  }

  @Test
  void testDoEventCorrectEventIdEmptyKey() {
    logger.info("DoEventTest: correct eventId and empty key");
    assertFalse(vcs.doEvent("testLogin", ""));
  }

  @Test
  void testDoEventCorrectEventIdNullKey() {
    logger.info("DoEventTest: correct eventId and empty key");
    assertFalse(vcs.doEvent("testLogin", null));
  }

  @Test
  void testDoEventNoCorrectEventIdNullKey() {
    logger.info("DoEventTest: no correct eventId and empty key");
    assertFalse(vcs.doEvent("IncorrectLogin", null));
  }

  @Test
  void testResetNoEventId() {
    logger.info("Reset: no correct eventId and no empty or null key");
    assertFalse(vcs.reset("IncorrectLogin", "This is a test"));
  }

  @Test
  void testResetCorrectEventId() {
    logger.info("Reset: correct eventId and no empty or null key");
    assertTrue(vcs.reset("testLogin", "This is a test"));
  }

  @Test
  void testResetNoCorrectEventIdEmptyKey() {
    logger.info("Reset: no correct eventId and empty key");
    assertFalse(vcs.reset("IncorrectLogin", ""));
  }

  @Test
  void testResetCorrectEventIdEmptyKey() {
    logger.info("Reset: correct eventId and empty key");
    assertFalse(vcs.reset("testLogin", ""));
  }

  @Test
  void testResetCorrectEventIdNullKey() {
    logger.info("Reset: correct eventId and empty key");
    assertFalse(vcs.reset("testLogin", null));
  }

  @Test
  void testResetNoCorrectEventIdNullKey() {
    logger.info("Reset: no correct eventId and empty key");
    assertFalse(vcs.reset("IncorrectLogin", null));
  }

  @Test
  void testDuplicateEventKey() {
    Assertions.assertThrows(DuplicatedEventKeyException.class, () ->
        new RateLimiter(redis.getContainerIpAddress(), redis.getMappedPort(6379), "secret",
            new EventConfig("aaa", 3, Duration.ZERO),
            new EventConfig("aaa", 2, Duration.ofSeconds(5))));
  }

  @Test
  void testWithResponse() {
    CanDoResponse canDoResponse = vcs.canDoEvent("testLogin", "This is a test");
    assertTrue(canDoResponse.getCanDo());
    assertNull(canDoResponse.getReason());
    assertEquals(0, canDoResponse.getWaitMillis());
  }

  @Test
  void testWithResponseReject() {
    CanDoResponse canDoResponse = vcs.canDoEvent("testLogin", "With Response Key");
    assertTrue(canDoResponse.getCanDo());
    assertNull(canDoResponse.getReason());
    assertEquals(0, canDoResponse.getWaitMillis());

    vcs.doEvent("testLogin", "With Response Key");
    vcs.doEvent("testLogin", "With Response Key");
    vcs.doEvent("testLogin", "With Response Key");
    vcs.doEvent("testLogin", "With Response Key");

    canDoResponse = vcs.canDoEvent("testLogin", "With Response Key");
    assertFalse(canDoResponse.getCanDo());
    assertEquals(Reason.TOO_MANY_EVENTS, canDoResponse.getReason());
    assertTrue(canDoResponse.getWaitMillis() > 0);
  }

  @Test
  void testGetEventConfig() {
    EventConfig freeTrial = vcs.getEventConfig("freeTrial");
    assertNotNull(freeTrial);
  }

  @Test
  void testGetEventConfigNonExists() {
    EventConfig freeTrial = vcs.getEventConfig("non.existing.event");
    assertNull(freeTrial);
  }
}
