package org.sputnik.ratelimit.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sputnik.ratelimit.util.Constants;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Testcontainers
public class RedisDaoTest {

  private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private static final String TEST_KEY = RandomStringUtils.randomAlphanumeric(5);

  private static final String TEST_EVENT_ID = RandomStringUtils.randomAlphanumeric(5);

  private static final Duration TEST_TIMEOUT = Duration.ofSeconds(10);
  @Container
  public static final GenericContainer redis = new GenericContainer("redis:3.0.6").withExposedPorts(6379);

  private static RedisDao redisDao;
  private static Jedis redisClient;

  @BeforeAll
  public static void init() {
    JedisPool jedisPool = new JedisPool(redis.getContainerIpAddress(), redis.getMappedPort(6379));
    redisClient = jedisPool.getResource();
    redisDao = new RedisDao(jedisPool);
  }

  @BeforeEach
  public void cleanRedis() {
    redisClient.flushAll();
  }


  @Test
  public void testAddEvent() {
    redisDao.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    assertEquals(1, redisClient.llen(TEST_EVENT_ID + Constants.KEY_SEPARATOR + TEST_KEY).intValue());
  }

  @Test
  public void testAddEventExpiration() throws InterruptedException {
    redisDao.addEvent(TEST_EVENT_ID, TEST_KEY, Duration.ofSeconds(1));
    assertEquals(1, redisClient.llen(TEST_EVENT_ID + Constants.KEY_SEPARATOR + TEST_KEY).intValue());
    TimeUnit.MILLISECONDS.sleep(1200);
    assertEquals(0, redisClient.llen(TEST_EVENT_ID + Constants.KEY_SEPARATOR + TEST_KEY).intValue());
  }

  @Test
  public void testRemoveEvent() {
    redisDao.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    assertEquals(1, redisClient.llen(TEST_EVENT_ID + Constants.KEY_SEPARATOR + TEST_KEY).intValue());
    redisDao.remove(TEST_EVENT_ID, TEST_KEY);
    assertEquals(0, redisClient.llen(TEST_EVENT_ID + Constants.KEY_SEPARATOR + TEST_KEY).intValue());
  }

  @Test
  public void testGetListLength() {
    redisDao.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    redisDao.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    redisDao.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    assertEquals(3, redisDao.getListLength(TEST_EVENT_ID, TEST_KEY).intValue());
  }

  @Test
  public void testGetListFirstElement() throws InterruptedException {
    redisDao.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    TimeUnit.MILLISECONDS.sleep(500);
    redisDao.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    TimeUnit.MILLISECONDS.sleep(500);
    redisDao.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    assertEquals(3, redisDao.getListLength(TEST_EVENT_ID, TEST_KEY).intValue());
    String firstElement = redisClient.lindex(TEST_EVENT_ID + Constants.KEY_SEPARATOR + TEST_KEY, 0);
    assertEquals(Long.parseLong(firstElement), redisDao.getListFirstElement(TEST_EVENT_ID, TEST_KEY).toEpochMilli());
  }

  @Test
  public void testGetListFirstElementWrongFormat() {
    redisClient.rpush(TEST_EVENT_ID + Constants.KEY_SEPARATOR + TEST_KEY, "A", "B", "C");
    assertEquals(3, redisDao.getListLength(TEST_EVENT_ID, TEST_KEY).intValue());
    assertNull(redisDao.getListFirstElement(TEST_EVENT_ID, TEST_KEY));
  }

  @Test
  public void testGetListFirstEventElement() throws InterruptedException {
    redisDao.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    TimeUnit.MILLISECONDS.sleep(500);
    redisDao.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    TimeUnit.MILLISECONDS.sleep(500);
    redisDao.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    assertEquals(3, redisDao.getListLength(TEST_EVENT_ID, TEST_KEY).intValue());
    String firstElement = redisClient.lindex(TEST_EVENT_ID + Constants.KEY_SEPARATOR + TEST_KEY, 1);
    assertEquals(Long.parseLong(firstElement), redisDao.getListFirstEventElement(TEST_EVENT_ID, TEST_KEY, 2L).toEpochMilli());
  }

  @Test
  public void testGetListFirstEventElementWrongFormat() {
    redisClient.rpush(TEST_EVENT_ID + Constants.KEY_SEPARATOR + TEST_KEY, "A", "B", "C");
    assertEquals(3, redisDao.getListLength(TEST_EVENT_ID, TEST_KEY).intValue());
    assertNull(redisDao.getListFirstEventElement(TEST_EVENT_ID, TEST_KEY, 2L));
  }

  @Test
  public void testRemoveListFirstElement() throws InterruptedException {
    redisDao.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    TimeUnit.MILLISECONDS.sleep(500);
    redisDao.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    TimeUnit.MILLISECONDS.sleep(500);
    redisDao.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    assertEquals(3, redisDao.getListLength(TEST_EVENT_ID, TEST_KEY).intValue());
    String firstElement = redisClient.lindex(TEST_EVENT_ID + Constants.KEY_SEPARATOR + TEST_KEY, 0);
    assertEquals(Long.parseLong(firstElement), redisDao.removeListFirstElement(TEST_EVENT_ID, TEST_KEY).toEpochMilli());
    assertEquals(2, redisDao.getListLength(TEST_EVENT_ID, TEST_KEY).intValue());
  }

  @Test
  public void testRemoveListFirstElementWrongFormat() {
    redisClient.rpush(TEST_EVENT_ID + Constants.KEY_SEPARATOR + TEST_KEY, "A", "B", "C");
    assertEquals(3, redisDao.getListLength(TEST_EVENT_ID, TEST_KEY).intValue());
    assertNull(redisDao.removeListFirstElement(TEST_EVENT_ID, TEST_KEY));
    assertEquals(2, redisDao.getListLength(TEST_EVENT_ID, TEST_KEY).intValue());
  }
}
