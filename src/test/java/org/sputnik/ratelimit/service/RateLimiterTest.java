package org.sputnik.ratelimit.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.redis.testcontainers.RedisContainer;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sputnik.ratelimit.domain.CanDoResponse;
import org.sputnik.ratelimit.domain.CanDoResponse.Reason;
import org.sputnik.ratelimit.exeception.DuplicatedEventKeyException;
import org.sputnik.ratelimit.util.EventConfig;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Slf4j
@Testcontainers
class RateLimiterTest {

  private static RateLimiter vcs;

  @Container
  private static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7.4.0"));

  @BeforeAll
  @SneakyThrows
  static void init() {
    EventConfig[] eventsConfig = new EventConfig[]{
      new EventConfig("testLogin", 3, Duration.ofSeconds(3600)),
      new EventConfig("freeTrial", 1, Duration.ofSeconds(600)),
      new EventConfig("maxLoginAttempts", 3, Duration.ofSeconds(6)),
      new EventConfig("recurrenceTest", 3, Duration.ofSeconds(10)),
      new EventConfig("logMessageTest", 3, Duration.ofSeconds(2)),
      new EventConfig("longRecurrenceTest", 1000, Duration.ofSeconds(1))
    };

    vcs = new RateLimiter(redis.getRedisHost(), redis.getRedisPort(), "hashSecret", eventsConfig);
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
    assertThat(vcs.canDoEvent(testEventId, testKey).canDo()).isTrue();

    assertThat(vcs.doEvent(testEventId, testKey)).isTrue(); // 1
    assertThat(vcs.canDoEvent(testEventId, testKey).canDo()).isTrue();

    assertThat(vcs.doEvent(testEventId, testKey)).isTrue(); // 2
    assertThat(vcs.canDoEvent(testEventId, testKey).canDo()).isTrue();

    assertThat(vcs.doEvent(testEventId, testKey)).isTrue(); // 3
    assertThat(vcs.canDoEvent(testEventId, testKey).canDo()).isFalse();

    // Wait for the event attempts to expire
    TimeUnit.MILLISECONDS.sleep(2200);
    assertThat(vcs.canDoEvent(testEventId, testKey).canDo()).isTrue();
  }

  @Test
  void testCanDoEventFullFlowEdgeCase() throws InterruptedException {
    String testEventId = "logMessageTest";
    String testKey = "my_test_key_2";

    // 0
    assertThat(vcs.canDoEvent(testEventId, testKey).canDo()).isTrue();

    assertThat(vcs.doEvent(testEventId, testKey)).isTrue(); // 1
    assertThat(vcs.canDoEvent(testEventId, testKey).canDo()).isTrue();
    TimeUnit.MILLISECONDS.sleep(300);

    assertThat(vcs.doEvent(testEventId, testKey)).isTrue(); // 2
    assertThat(vcs.canDoEvent(testEventId, testKey).canDo()).isTrue();

    TimeUnit.MILLISECONDS.sleep(300);
    assertThat(vcs.doEvent(testEventId, testKey)).isTrue(); // 3
    assertThat(vcs.canDoEvent(testEventId, testKey).canDo()).isFalse();

    // Wait for the event attempts to expire
    TimeUnit.MILLISECONDS.sleep(1600);
    assertThat(vcs.canDoEvent(testEventId, testKey).canDo()).isTrue();
  }

  @Test
  @DisplayName("CanDoEventTest: no correct eventId and no empty or null key")
  void testCanDoEventNoEventId() {
    assertThat(vcs.canDoEvent("IncorrectLogin", "This is a test").canDo()).isFalse();
  }

  @Test
  @DisplayName("CanDoEventTest: correct eventId and no empty or null key")
  void testCanDoEventCorrectEventId() {
    assertThat(vcs.canDoEvent("testLogin", "This is a test").canDo()).isTrue();
  }

  @Test
  @DisplayName("CanDoEventTest: no correct eventId and empty key")
  void testCanDoEventNoCorrectEventIdEmptyKey() {
    assertThat(vcs.canDoEvent("IncorrectLogin", "").canDo()).isFalse();
  }

  @Test
  @DisplayName("CanDoEventTest: correct eventId and empty key")
  void testCanDoEventCorrectEventIdEmptyKey() {
    assertThat(vcs.canDoEvent("testLogin", "").canDo()).isFalse();
  }

  @Test
  @DisplayName("CanDoEventTest: correct eventId and empty key")
  void testCanDoEventCorrectEventIdNullKey() {
    assertThat(vcs.canDoEvent("testLogin", null).canDo()).isFalse();
  }

  @Test
  @DisplayName("CanDoEventTest: no correct eventId and empty key")
  void testCanDoEventNoCorrectEventIdNullKey() {
    assertThat(vcs.canDoEvent("IncorrectLogin", null).canDo()).isFalse();
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
    EventConfig event1 = new EventConfig("aaa", 3, Duration.ofSeconds(1));
    EventConfig event2 = new EventConfig("aaa", 2, Duration.ofSeconds(5));
    String ipAddress = redis.getRedisHost();
    int port = redis.getRedisPort();
    assertThatExceptionOfType(DuplicatedEventKeyException.class).isThrownBy(() ->
      new RateLimiter(ipAddress, port, "secret", event1, event2)
    );
  }

  @Test
  void testWithResponse() {
    CanDoResponse canDoResponse = vcs.canDoEvent("testLogin", "This is a test");
    assertThat(canDoResponse.canDo()).isTrue();
    assertThat(canDoResponse.reason()).isNull();
    assertThat(canDoResponse.waitMillis()).isZero();
  }

  @Test
  void testWithResponseReject() {
    CanDoResponse canDoResponse = vcs.canDoEvent("testLogin", "With Response Key");
    assertThat(canDoResponse.canDo()).isTrue();
    assertThat(canDoResponse.reason()).isNull();
    assertThat(canDoResponse.waitMillis()).isZero();

    // Do multiple events to force a reject later
    vcs.doEvent("testLogin", "With Response Key");
    vcs.doEvent("testLogin", "With Response Key");
    vcs.doEvent("testLogin", "With Response Key");
    vcs.doEvent("testLogin", "With Response Key");

    canDoResponse = vcs.canDoEvent("testLogin", "With Response Key");
    assertThat(canDoResponse.canDo()).isFalse();
    assertThat(canDoResponse.reason()).isEqualTo(Reason.TOO_MANY_EVENTS);
    assertThat(canDoResponse.waitMillis()).isPositive();
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

  @Test
  void pseudoPerformanceTest() {
    String testEventId = "longRecurrenceTest";
    String testKey = "my_test_key_performance";

    int totalEvents = 10_000;
    int canDoCount = 0;

    long startTime = System.currentTimeMillis();

    for (int i = 0; i < totalEvents; i++) {
      if (vcs.canDoEvent(testEventId, testKey).canDo()) {
        vcs.doEvent(testEventId, testKey);
        canDoCount++;
      }
    }

    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;

    log.info("Total events processed: {}", totalEvents);
    log.info("Events allowed: {}", canDoCount);
    log.info("Total time taken (ms): {}", duration);
    log.info("Average time per event (ms): {}", (double) duration / totalEvents);

    assertThat(duration).isLessThan(10_000); // Ensure the test runs within a reasonable time
  }
}
