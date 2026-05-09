package org.sputnik.ratelimit.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.redis.testcontainers.RedisContainer;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Testcontainers
class EventsRedisRepositoryTest {

  private static final String TEST_KEY = RandomStringUtils.insecure().nextAlphanumeric(5);

  private static final String TEST_EVENT_ID = RandomStringUtils.insecure().nextAlphanumeric(5);

  private static final Duration TEST_TIMEOUT = Duration.ofSeconds(10);
  @Container
  private static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7.4.0"));

  private static EventsRedisRepository eventsRedisRepository;
  private static Jedis redisClient;

  @BeforeAll
  public static void init() {
    JedisPool jedisPool = new JedisPool(redis.getRedisHost(), redis.getRedisPort());
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
    assertThat(redisClient.zcard(eventKey(TEST_EVENT_ID, TEST_KEY))).isOne();
  }

  @Test
  void testAddEventExpiration() throws InterruptedException {
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, Duration.ofMillis(1000));
    assertThat(redisClient.zcard(eventKey(TEST_EVENT_ID, TEST_KEY))).isOne();
    TimeUnit.MILLISECONDS.sleep(1200);
    assertThat(redisClient.zcard(eventKey(TEST_EVENT_ID, TEST_KEY))).isZero();
  }

  @Test
  void testRemoveEvent() {
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    assertThat(redisClient.zcard(eventKey(TEST_EVENT_ID, TEST_KEY))).isOne();
    eventsRedisRepository.remove(TEST_EVENT_ID, TEST_KEY);
    assertThat(redisClient.zcard(eventKey(TEST_EVENT_ID, TEST_KEY))).isZero();
  }

  @Test
  void testGetEventsCount() {
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    assertThat(eventsRedisRepository.getEventsCount(TEST_EVENT_ID, TEST_KEY)).isEqualTo(3);
  }

  @Test
  void testGetOldestEvent() throws InterruptedException {
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    TimeUnit.MILLISECONDS.sleep(100);
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    TimeUnit.MILLISECONDS.sleep(100);
    eventsRedisRepository.addEvent(TEST_EVENT_ID, TEST_KEY, TEST_TIMEOUT);
    assertThat(eventsRedisRepository.getEventsCount(TEST_EVENT_ID, TEST_KEY)).isEqualTo(3);
    Double firstScore = redisClient.zscore(eventKey(TEST_EVENT_ID, TEST_KEY), redisClient.zrange(eventKey(TEST_EVENT_ID, TEST_KEY), 0, 0).iterator().next());
    assertThat(eventsRedisRepository.getOldestEvent(TEST_EVENT_ID, TEST_KEY).toEpochMilli()).isEqualTo(firstScore.longValue());
  }

  @Test
  void testGetOldestEventEmpty() {
    assertThat(eventsRedisRepository.getOldestEvent(TEST_EVENT_ID, TEST_KEY)).isNull();
  }

  @Test
  void testRemoveEventsOlderThan() {
    String key = eventKey(TEST_EVENT_ID, TEST_KEY);
    long now = System.currentTimeMillis();
    redisClient.zadd(key, now - 10_000, "old-1");
    redisClient.zadd(key, now - 5_000, "old-2");
    redisClient.zadd(key, now - 500, "new-1");
    redisClient.zadd(key, now, "new-2");

    long removed = eventsRedisRepository.removeEventsOlderThan(TEST_EVENT_ID, TEST_KEY, Instant.ofEpochMilli(now - 1_000));
    assertThat(removed).isEqualTo(2);
    assertThat(eventsRedisRepository.getEventsCount(TEST_EVENT_ID, TEST_KEY)).isEqualTo(2);
  }

  @Test
  void testRemoveEventsOlderThanBoundaryExclusive() {
    String key = eventKey(TEST_EVENT_ID, TEST_KEY);
    long threshold = System.currentTimeMillis() - 2_000;
    redisClient.zadd(key, threshold - 1, "old");
    redisClient.zadd(key, threshold, "boundary");
    redisClient.zadd(key, threshold + 1, "new");

    long removed = eventsRedisRepository.removeEventsOlderThan(TEST_EVENT_ID, TEST_KEY, Instant.ofEpochMilli(threshold));
    assertThat(removed).isOne();
    assertThat(eventsRedisRepository.getEventsCount(TEST_EVENT_ID, TEST_KEY)).isEqualTo(2);
  }

  private String eventKey(String eventId, String key) {
    return eventId + EventsRedisRepository.KEY_SEPARATOR + key;
  }
}
