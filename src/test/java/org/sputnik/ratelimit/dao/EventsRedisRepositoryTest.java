package org.sputnik.ratelimit.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Testcontainers
class EventsRedisRepositoryTest {

  private static final String TEST_KEY = RandomStringUtils.randomAlphanumeric(5);

  private static final String TEST_EVENT_ID = RandomStringUtils.randomAlphanumeric(5);

  private static final Duration TEST_TIMEOUT = Duration.ofSeconds(10);
  @Container
  public static final GenericContainer redis = new GenericContainer("redis:3.0.6").withExposedPorts(6379);

  private static EventsRedisRepository eventsRedisRepository;
  private static Jedis redisClient;

  @BeforeAll
  public static void init() {
    JedisPool jedisPool = new JedisPool(redis.getContainerIpAddress(), redis.getMappedPort(6379));
    redisClient = jedisPool.getResource();
    eventsRedisRepository = new EventsRedisRepository(jedisPool);
  }

  @BeforeEach
  public void cleanRedis() {
    redisClient.flushAll();
  }


  @Test
  void testAddEvent() {
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    assertThat(redisClient.llen(eventKey(TEST_EVENT_ID, TEST_KEY))).isOne();
  }

  @Test
  void testAddEventExpiration() throws InterruptedException {
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, Duration.ofMillis(1000));
    assertThat(redisClient.llen(eventKey(TEST_EVENT_ID, TEST_KEY))).isOne();
    TimeUnit.MILLISECONDS.sleep(1200);
    assertThat(redisClient.llen(eventKey(TEST_EVENT_ID, TEST_KEY))).isZero();
  }

  @Test
  void testRemoveEvent() {
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    assertThat(redisClient.llen(eventKey(TEST_EVENT_ID, TEST_KEY))).isOne();
    eventsRedisRepository.remove(TEST_EVENT_ID, TEST_KEY);
    assertThat(redisClient.llen(eventKey(TEST_EVENT_ID, TEST_KEY))).isZero();
  }

  @Test
  void testGetListLength() {
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    assertThat(eventsRedisRepository.getListLength(TEST_EVENT_ID, TEST_KEY)).isEqualTo(3);
  }

  @Test
  void testGetListFirstElement() throws InterruptedException {
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    TimeUnit.MILLISECONDS.sleep(100);
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    TimeUnit.MILLISECONDS.sleep(100);
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    assertThat(eventsRedisRepository.getListLength(TEST_EVENT_ID, TEST_KEY)).isEqualTo(3);
    String firstElement = redisClient.lindex(eventKey(TEST_EVENT_ID, TEST_KEY), 0);
    assertThat(eventsRedisRepository.getListFirstElement(TEST_EVENT_ID, TEST_KEY).toEpochMilli()).isEqualTo(Long.parseLong(firstElement));
  }

  @Test
  void testGetListFirstElementWrongFormat() {
    redisClient.rpush(eventKey(TEST_EVENT_ID, TEST_KEY), "A", "B", "C");
    assertThat(eventsRedisRepository.getListLength(TEST_EVENT_ID, TEST_KEY)).isEqualTo(3);
    assertThat(eventsRedisRepository.getListFirstElement(TEST_EVENT_ID, TEST_KEY)).isNull();
  }

  @Test
  void testGetListFirstEventElement() throws InterruptedException {
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    TimeUnit.MILLISECONDS.sleep(100);
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    TimeUnit.MILLISECONDS.sleep(100);
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    assertThat(eventsRedisRepository.getListLength(TEST_EVENT_ID, TEST_KEY)).isEqualTo(3);
    String firstElement = redisClient.lindex(eventKey(TEST_EVENT_ID, TEST_KEY), 1);
    assertThat(eventsRedisRepository.getListFirstEventElement(TEST_EVENT_ID, TEST_KEY, 2L).toEpochMilli())
      .isEqualTo(Long.parseLong(firstElement));
  }

  @Test
  void testGetListFirstEventElementWrongFormat() {
    redisClient.rpush(eventKey(TEST_EVENT_ID, TEST_KEY), "A", "B", "C");
    assertThat(eventsRedisRepository.getListLength(TEST_EVENT_ID, TEST_KEY)).isEqualTo(3);
    assertThat(eventsRedisRepository.getListFirstEventElement(TEST_EVENT_ID, TEST_KEY, 2L)).isNull();
  }

  @Test
  void testRemoveListFirstElement() throws InterruptedException {
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    TimeUnit.MILLISECONDS.sleep(100);
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    TimeUnit.MILLISECONDS.sleep(100);
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    assertThat(eventsRedisRepository.getListLength(TEST_EVENT_ID, TEST_KEY)).isEqualTo(3);
    String firstElement = redisClient.lindex(eventKey(TEST_EVENT_ID, TEST_KEY), 0);
    assertThat(eventsRedisRepository.removeListFirstElement(TEST_EVENT_ID, TEST_KEY).toEpochMilli())
      .isEqualTo(Long.parseLong(firstElement));
    assertThat(eventsRedisRepository.getListLength(TEST_EVENT_ID, TEST_KEY)).isEqualTo(2);
  }

  @Test
  void testRemoveListFirstElementWrongFormat() {
    redisClient.rpush(eventKey(TEST_EVENT_ID, TEST_KEY), "A", "B", "C");
    assertThat(eventsRedisRepository.getListLength(TEST_EVENT_ID, TEST_KEY)).isEqualTo(3);
    assertThat(eventsRedisRepository.removeListFirstElement(TEST_EVENT_ID, TEST_KEY)).isNull();
    assertThat(eventsRedisRepository.getListLength(TEST_EVENT_ID, TEST_KEY)).isEqualTo(2);
  }


  private String eventKey(String eventId, String key) {
    return eventId + EventsRedisRepository.KEY_SEPARATOR + key;
  }
}
