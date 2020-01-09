package org.sputnik.ratelimit.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
public class EventsRedisRepositoryTest {

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
  public void testAddEvent() {
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    assertEquals(1, redisClient.llen(eventKey(TEST_EVENT_ID, TEST_KEY)).intValue());
  }

  @Test
  public void testAddEventExpiration() throws InterruptedException {
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, Duration.ofMillis(1000));
    assertEquals(1, redisClient.llen(eventKey(TEST_EVENT_ID, TEST_KEY)).intValue());
    TimeUnit.MILLISECONDS.sleep(1200);
    assertEquals(0, redisClient.llen(eventKey(TEST_EVENT_ID, TEST_KEY)).intValue());
  }

  @Test
  public void testRemoveEvent() {
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    assertEquals(1, redisClient.llen(eventKey(TEST_EVENT_ID, TEST_KEY)).intValue());
    eventsRedisRepository.remove(TEST_EVENT_ID, TEST_KEY);
    assertEquals(0, redisClient.llen(eventKey(TEST_EVENT_ID, TEST_KEY)).intValue());
  }

  @Test
  public void testGetListLength() {
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    assertEquals(3, eventsRedisRepository.getListLength(TEST_EVENT_ID, TEST_KEY).intValue());
  }

  @Test
  public void testGetListFirstElement() throws InterruptedException {
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    TimeUnit.MILLISECONDS.sleep(100);
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    TimeUnit.MILLISECONDS.sleep(100);
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    assertEquals(3, eventsRedisRepository.getListLength(TEST_EVENT_ID, TEST_KEY).intValue());
    String firstElement = redisClient.lindex(eventKey(TEST_EVENT_ID, TEST_KEY), 0);
    assertEquals(Long.parseLong(firstElement), eventsRedisRepository.getListFirstElement(TEST_EVENT_ID, TEST_KEY).toEpochMilli());
  }

  @Test
  public void testGetListFirstElementWrongFormat() {
    redisClient.rpush(eventKey(TEST_EVENT_ID, TEST_KEY), "A", "B", "C");
    assertEquals(3, eventsRedisRepository.getListLength(TEST_EVENT_ID, TEST_KEY).intValue());
    assertNull(eventsRedisRepository.getListFirstElement(TEST_EVENT_ID, TEST_KEY));
  }

  @Test
  public void testGetListFirstEventElement() throws InterruptedException {
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    TimeUnit.MILLISECONDS.sleep(100);
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    TimeUnit.MILLISECONDS.sleep(100);
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    assertEquals(3, eventsRedisRepository.getListLength(TEST_EVENT_ID, TEST_KEY).intValue());
    String firstElement = redisClient.lindex(eventKey(TEST_EVENT_ID, TEST_KEY), 1);
    assertEquals(Long.parseLong(firstElement), eventsRedisRepository.getListFirstEventElement(TEST_EVENT_ID, TEST_KEY, 2L).toEpochMilli());
  }

  @Test
  public void testGetListFirstEventElementWrongFormat() {
    redisClient.rpush(eventKey(TEST_EVENT_ID, TEST_KEY), "A", "B", "C");
    assertEquals(3, eventsRedisRepository.getListLength(TEST_EVENT_ID, TEST_KEY).intValue());
    assertNull(eventsRedisRepository.getListFirstEventElement(TEST_EVENT_ID, TEST_KEY, 2L));
  }

  @Test
  public void testRemoveListFirstElement() throws InterruptedException {
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    TimeUnit.MILLISECONDS.sleep(100);
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    TimeUnit.MILLISECONDS.sleep(100);
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    assertEquals(3, eventsRedisRepository.getListLength(TEST_EVENT_ID, TEST_KEY).intValue());
    String firstElement = redisClient.lindex(eventKey(TEST_EVENT_ID, TEST_KEY), 0);
    assertEquals(Long.parseLong(firstElement), eventsRedisRepository.removeListFirstElement(TEST_EVENT_ID, TEST_KEY).toEpochMilli());
    assertEquals(2, eventsRedisRepository.getListLength(TEST_EVENT_ID, TEST_KEY).intValue());
  }

  @Test
  public void testRemoveListFirstElementWrongFormat() {
    redisClient.rpush(eventKey(TEST_EVENT_ID, TEST_KEY), "A", "B", "C");
    assertEquals(3, eventsRedisRepository.getListLength(TEST_EVENT_ID, TEST_KEY).intValue());
    assertNull(eventsRedisRepository.removeListFirstElement(TEST_EVENT_ID, TEST_KEY));
    assertEquals(2, eventsRedisRepository.getListLength(TEST_EVENT_ID, TEST_KEY).intValue());
  }


  private String eventKey(String eventId, String key) {
    return eventId + EventsRedisRepository.KEY_SEPARATOR + key;
  }
}
