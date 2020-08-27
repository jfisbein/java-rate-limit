package org.sputnik.ratelimit.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sputnik.ratelimit.domain.CanDoResponse;
import org.sputnik.ratelimit.domain.CanDoResponse.Reason;
import org.sputnik.ratelimit.exeception.DuplicatedEventKeyException;
import org.sputnik.ratelimit.util.EventConfig;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class RateLimiterTest {

  private static RateLimiter vcs;

  @Container
  private static final GenericContainer redis = new GenericContainer("redis:3.0.6").withExposedPorts(6379);

  @BeforeAll
  static void init() {
    EventConfig[] eventsConfig = new EventConfig[]{
      new EventConfig("testLogin", 3, Duration.ofSeconds(3600)),
      new EventConfig("freeTrial", 1, Duration.ofSeconds(600)),
      new EventConfig("maxLoginAttempts", 3, Duration.ofSeconds(6)),
      new EventConfig("recurrenceTest", 3, Duration.ofSeconds(10)),
      new EventConfig("logMessageTest", 3, Duration.ofSeconds(2))
    };

    vcs = new RateLimiter(redis.getContainerIpAddress(), redis.getMappedPort(6379), "hashSecret", eventsConfig);
  }

  @AfterAll
  static void tearDown() {
    vcs.close();
  }

  @Test
  void testCanDoEventFullFlow() throws InterruptedException {
    String testEventId = "logMessageTest";
    String testKey = "my_test_key";

    // 0
    assertThat(vcs.canDoEvent(testEventId, testKey).getCanDo()).isTrue();

    assertThat(vcs.doEvent(testEventId, testKey)).isTrue(); // 1
    assertThat(vcs.canDoEvent(testEventId, testKey).getCanDo()).isTrue();

    assertThat(vcs.doEvent(testEventId, testKey)).isTrue(); // 2
    assertThat(vcs.canDoEvent(testEventId, testKey).getCanDo()).isTrue();

    assertThat(vcs.doEvent(testEventId, testKey)).isTrue(); // 3
    assertThat(vcs.canDoEvent(testEventId, testKey).getCanDo()).isFalse();

    // Wait for the event attempts to expire
    TimeUnit.MILLISECONDS.sleep(2200);
    assertThat(vcs.canDoEvent(testEventId, testKey).getCanDo()).isTrue();
  }

  @Test
  void testCanDoEventFullFlowEdgeCase() throws InterruptedException {
    String testEventId = "logMessageTest";
    String testKey = "my_test_key_2";

    // 0
    assertThat(vcs.canDoEvent(testEventId, testKey).getCanDo()).isTrue();

    assertThat(vcs.doEvent(testEventId, testKey)).isTrue(); // 1
    assertThat(vcs.canDoEvent(testEventId, testKey).getCanDo()).isTrue();
    TimeUnit.MILLISECONDS.sleep(300);

    assertThat(vcs.doEvent(testEventId, testKey)).isTrue(); // 2
    assertThat(vcs.canDoEvent(testEventId, testKey).getCanDo()).isTrue();

    TimeUnit.MILLISECONDS.sleep(300);
    assertThat(vcs.doEvent(testEventId, testKey)).isTrue(); // 3
    assertThat(vcs.canDoEvent(testEventId, testKey).getCanDo()).isFalse();

    // Wait for the event attempts to expire
    TimeUnit.MILLISECONDS.sleep(1600);
    assertThat(vcs.canDoEvent(testEventId, testKey).getCanDo()).isTrue();
  }

  @Test
  @DisplayName("CanDoEventTest: no correct eventId and no empty or null key")
  void testCanDoEventNoEventId() {
    assertThat(vcs.canDoEvent("IncorrectLogin", "This is a test").getCanDo()).isFalse();
  }

  @Test
  @DisplayName("CanDoEventTest: correct eventId and no empty or null key")
  void testCanDoEventCorrectEventId() {
    assertThat(vcs.canDoEvent("testLogin", "This is a test").getCanDo()).isTrue();
  }

  @Test
  @DisplayName("CanDoEventTest: no correct eventId and empty key")
  void testCanDoEventNoCorrectEventIdEmptyKey() {
    assertThat(vcs.canDoEvent("IncorrectLogin", "").getCanDo()).isFalse();
  }

  @Test
  @DisplayName("CanDoEventTest: correct eventId and empty key")
  void testCanDoEventCorrectEventIdEmptyKey() {
    assertThat(vcs.canDoEvent("testLogin", "").getCanDo()).isFalse();
  }

  @Test
  @DisplayName("CanDoEventTest: correct eventId and empty key")
  void testCanDoEventCorrectEventIdNullKey() {
    assertThat(vcs.canDoEvent("testLogin", null).getCanDo()).isFalse();
  }

  @Test
  @DisplayName("CanDoEventTest: no correct eventId and empty key")
  void testCanDoEventNoCorrectEventIdNullKey() {
    assertThat(vcs.canDoEvent("IncorrectLogin", null).getCanDo()).isFalse();
  }

  @Test
  @DisplayName("DoEventTest: no correct eventId and no empty or null key")
  void testDoEventNoEventId() {
    assertThat(vcs.doEvent("IncorrectLogin", "This is a test")).isFalse();
  }

  @Test
  @DisplayName("DoEventTest: correct eventId and no empty or null key")
  void testDoEventCorrectEventId() {
    assertThat(vcs.doEvent("testLogin", "This is a test")).isTrue();
  }

  @Test
  @DisplayName("DoEventTest: no correct eventId and empty key")
  void testDoEventNoCorrectEventIdEmptyKey() {
    assertThat(vcs.doEvent("IncorrectLogin", "")).isFalse();
  }

  @Test
  @DisplayName("DoEventTest: correct eventId and empty key")
  void testDoEventCorrectEventIdEmptyKey() {
    assertThat(vcs.doEvent("testLogin", "")).isFalse();
  }

  @Test
  @DisplayName("DoEventTest: correct eventId and empty key")
  void testDoEventCorrectEventIdNullKey() {
    assertThat(vcs.doEvent("testLogin", null)).isFalse();
  }

  @Test
  @DisplayName("DoEventTest: no correct eventId and null key")
  void testDoEventNoCorrectEventIdNullKey() {
    assertThat(vcs.doEvent("IncorrectLogin", null)).isFalse();
  }

  @Test
  @DisplayName("Reset: no correct eventId and no empty or null key")
  void testResetNoEventId() {
    assertThat(vcs.reset("IncorrectLogin", "This is a test")).isFalse();
  }

  @Test
  @DisplayName("Reset: correct eventId and no empty or null key")
  void testResetCorrectEventId() {
    assertThat(vcs.reset("testLogin", "This is a test")).isTrue();
  }

  @Test
  @DisplayName("Reset: no correct eventId and empty key")
  void testResetNoCorrectEventIdEmptyKey() {
    assertThat(vcs.reset("IncorrectLogin", "")).isFalse();
  }

  @Test
  @DisplayName("Reset: correct eventId and empty key")
  void testResetCorrectEventIdEmptyKey() {
    assertThat(vcs.reset("testLogin", "")).isFalse();
  }

  @Test
  @DisplayName("Reset: correct eventId and empty key")
  void testResetCorrectEventIdNullKey() {
    assertThat(vcs.reset("testLogin", null)).isFalse();
  }

  @Test
  @DisplayName("Reset: no correct eventId and empty key")
  void testResetNoCorrectEventIdNullKey() {
    assertThat(vcs.reset("IncorrectLogin", null)).isFalse();
  }

  @Test
  void testDuplicateEventKey() {
    EventConfig event1 = new EventConfig("aaa", 3, Duration.ZERO);
    EventConfig event2 = new EventConfig("aaa", 2, Duration.ofSeconds(5));
    String ipAddress = redis.getContainerIpAddress();
    Integer port = redis.getMappedPort(6379);
    assertThatExceptionOfType(DuplicatedEventKeyException.class).isThrownBy(() ->
      new RateLimiter(ipAddress, port, "secret", event1, event2)
    );
  }

  @Test
  void testWithResponse() {
    CanDoResponse canDoResponse = vcs.canDoEvent("testLogin", "This is a test");
    assertThat(canDoResponse.getCanDo()).isTrue();
    assertThat(canDoResponse.getReason()).isNull();
    assertThat(canDoResponse.getWaitMillis()).isZero();
  }

  @Test
  void testWithResponseReject() {
    CanDoResponse canDoResponse = vcs.canDoEvent("testLogin", "With Response Key");
    assertThat(canDoResponse.getCanDo()).isTrue();
    assertThat(canDoResponse.getReason()).isNull();
    assertThat(canDoResponse.getWaitMillis()).isZero();

    vcs.doEvent("testLogin", "With Response Key");
    vcs.doEvent("testLogin", "With Response Key");
    vcs.doEvent("testLogin", "With Response Key");
    vcs.doEvent("testLogin", "With Response Key");

    canDoResponse = vcs.canDoEvent("testLogin", "With Response Key");
    assertThat(canDoResponse.getCanDo()).isFalse();
    assertThat(canDoResponse.getReason()).isEqualTo(Reason.TOO_MANY_EVENTS);
    assertThat(canDoResponse.getWaitMillis()).isPositive();
  }

  @Test
  void testGetEventConfig() {
    Optional<EventConfig> eventConfig = vcs.getEventConfig("freeTrial");
    assertThat(eventConfig).isPresent();
  }

  @Test
  void testGetEventConfigNonExists() {
    Optional<EventConfig> eventConfig = vcs.getEventConfig("non.existing.event");
    assertThat(eventConfig).isNotPresent();
  }
}
