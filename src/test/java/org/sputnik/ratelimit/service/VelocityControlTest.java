package org.sputnik.ratelimit.service;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.sputnik.ratelimit.util.EventConfig;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Testcontainers
public class VelocityControlTest {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(VelocityControlTest.class);

  protected static VelocityControl vcs;
  private static Jedis redisClient;
  @Container
  public static final GenericContainer redis = new GenericContainer("redis:3.0.6").withExposedPorts(6379);

  @BeforeAll
  public static void init() {
    List<EventConfig> eventsConfig = new ArrayList<>();
    eventsConfig.add(new EventConfig("testLogin", 3, Duration.ofSeconds(3600)));
    eventsConfig.add(new EventConfig("freeTrial", 1, Duration.ofSeconds(600)));
    eventsConfig.add(new EventConfig("maxLoginAttempts", 3, Duration.ofSeconds(6)));
    eventsConfig.add(new EventConfig("recurrenceTest", 3, Duration.ofSeconds(10)));
    eventsConfig.add(new EventConfig("logMessageTest", 3, Duration.ofSeconds(2)));

    vcs = new VelocityControl(new JedisConfiguration().setPort(redis.getMappedPort(6379)).setHost(redis.getContainerIpAddress()),
        "hashsecret", eventsConfig.toArray(new EventConfig[0]));

    redisClient = new JedisPool(redis.getContainerIpAddress(), redis.getMappedPort(6379)).getResource();
  }


  @Test
  public void testCanDoEventFullFlow() throws InterruptedException {
    String testEventId = "logMessageTest";
    String testKey = "my_test_key";

    // 0
    assertTrue(vcs.canDoEvent(testEventId, testKey));

    assertTrue(vcs.doEvent(testEventId, testKey)); // 1
    assertTrue(vcs.canDoEvent(testEventId, testKey));

    assertTrue(vcs.doEvent(testEventId, testKey)); // 2
    assertTrue(vcs.canDoEvent(testEventId, testKey));

    assertTrue(vcs.doEvent(testEventId, testKey)); // 3
    assertFalse(vcs.canDoEvent(testEventId, testKey));

    // Wait for the event attempts to expire
    TimeUnit.MILLISECONDS.sleep(2500);
    assertTrue(vcs.canDoEvent(testEventId, testKey));
  }

  @Test
  public void testCanDoEventFullFlowEdgeCase() throws InterruptedException {
    String testEventId = "logMessageTest";
    String testKey = "my_test_key_2";

    // 0
    assertTrue(vcs.canDoEvent(testEventId, testKey));

    assertTrue(vcs.doEvent(testEventId, testKey)); // 1
    assertTrue(vcs.canDoEvent(testEventId, testKey));
    TimeUnit.MILLISECONDS.sleep(300);

    assertTrue(vcs.doEvent(testEventId, testKey)); // 2
    assertTrue(vcs.canDoEvent(testEventId, testKey));

    TimeUnit.MILLISECONDS.sleep(300);
    assertTrue(vcs.doEvent(testEventId, testKey)); // 3
    assertFalse(vcs.canDoEvent(testEventId, testKey));

    // Wait for the event attempts to expire
    TimeUnit.MILLISECONDS.sleep(1600);
    assertTrue(vcs.canDoEvent(testEventId, testKey));
  }

  @Test
  public void testCanDoEventNoEventId() {
    logger.info("CanDoEventTest: no correct eventId and no empty or null key");
    assertFalse(vcs.canDoEvent("IncorrectLogin", "This is a test"));
  }

  @Test
  public void testCanDoEventCorrectEventId() {
    logger.info("CanDoEventTest: correct eventId and no empty or null key");
    assertTrue(vcs.canDoEvent("testLogin", "This is a test"));
  }

  @Test
  public void testCanDoEventNoCorrectEventIdEmptyKey() {
    logger.info("CanDoEventTest: no correct eventId and empty key");
    assertFalse(vcs.canDoEvent("IncorrectLogin", ""));
  }

  @Test
  public void testCanDoEventCorrectEventIdEmptyKey() {
    logger.info("CanDoEventTest: correct eventId and empty key");
    assertFalse(vcs.canDoEvent("testLogin", ""));
  }

  @Test
  public void testCanDoEventCorrectEventIdNullKey() {
    logger.info("CanDoEventTest: correct eventId and empty key");
    assertFalse(vcs.canDoEvent("testLogin", null));
  }

  @Test
  public void testCanDoEventNoCorrectEventIdNullKey() {
    logger.info("CanDoEventTest: no correct eventId and empty key");
    assertFalse(vcs.canDoEvent("IncorrectLogin", null));
  }

  @Test
  public void testDoEventNoEventId() {
    logger.info("DoEventTest: no correct eventId and no empty or null key");
    assertFalse(vcs.doEvent("IncorrectLogin", "This is a test"));
  }

  @Test
  public void testDoEventCorrectEventId() {
    logger.info("DoEventTest: correct eventId and no empty or null key");
    assertTrue(vcs.doEvent("testLogin", "This is a test"));
  }

  @Test
  public void testDoEventNoCorrectEventIdEmptyKey() {
    logger.info("DoEventTest: no correct eventId and empty key");
    assertFalse(vcs.doEvent("IncorrectLogin", ""));
  }

  @Test
  public void testDoEventCorrectEventIdEmptyKey() {
    logger.info("DoEventTest: correct eventId and empty key");
    assertFalse(vcs.doEvent("testLogin", ""));
  }

  @Test
  public void testDoEventCorrectEventIdNullKey() {
    logger.info("DoEventTest: correct eventId and empty key");
    assertFalse(vcs.doEvent("testLogin", null));
  }

  @Test
  public void testDoEventNoCorrectEventIdNullKey() {
    logger.info("DoEventTest: no correct eventId and empty key");
    assertFalse(vcs.doEvent("IncorrectLogin", null));
  }

  @Test
  public void testResetNoEventId() {
    logger.info("Reset: no correct eventId and no empty or null key");
    assertFalse(vcs.reset("IncorrectLogin", "This is a test"));
  }

  @Test
  public void testResetCorrectEventId() {
    logger.info("Reset: correct eventId and no empty or null key");
    assertTrue(vcs.reset("testLogin", "This is a test"));
  }

  @Test
  public void testResetNoCorrectEventIdEmptyKey() {
    logger.info("Reset: no correct eventId and empty key");
    assertFalse(vcs.reset("IncorrectLogin", ""));
  }

  @Test
  public void testResetCorrectEventIdEmptyKey() {
    logger.info("Reset: correct eventId and empty key");
    assertFalse(vcs.reset("testLogin", ""));
  }

  @Test
  public void testResetCorrectEventIdNullKey() {
    logger.info("Reset: correct eventId and empty key");
    assertFalse(vcs.reset("testLogin", null));
  }

  @Test
  public void testResetNoCorrectEventIdNullKey() {
    logger.info("Reset: no correct eventId and empty key");
    assertFalse(vcs.reset("IncorrectLogin", null));
  }
}
