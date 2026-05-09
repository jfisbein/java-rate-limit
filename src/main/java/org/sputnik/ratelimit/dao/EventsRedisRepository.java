package org.sputnik.ratelimit.dao;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.resps.Tuple;

/**
 * Repository to manage Events persistence.
 */
public class EventsRedisRepository {

  protected static final String KEY_SEPARATOR = "-";
  protected final JedisPool jedisPool;

  /**
   * Constructor.
   *
   * @param jedisPool Jedis Pool.
   */
  public EventsRedisRepository(JedisPool jedisPool) {
    this.jedisPool = Objects.requireNonNull(jedisPool, "jedisPool must not be null");
  }

  /**
   * Add Event.
   *
   * @param eventId  Event id.
   * @param key      Key.
   * @param duration Max duration.
   */
  public void addEvent(String eventId, String key, Duration duration) {
    try (Jedis jedis = jedisPool.getResource()) {
      String redisKey = eventKey(eventId, key);
      long now = System.currentTimeMillis();
      String member = UUID.randomUUID().toString();
      jedis.zadd(redisKey, now, member);
      if (duration != null) {
        jedis.pexpire(redisKey, Math.max(1, duration.toMillis()));
      }
    }
  }

  /**
   * Get number of events for an event id, and a key.
   *
   * @param eventId Event id.
   * @param key     Key.
   * @return number of events.
   */
  public long getEventsCount(String eventId, String key) {
    long result;
    try (Jedis jedis = jedisPool.getResource()) {
      result = jedis.zcard(eventKey(eventId, key));
    }

    return result;
  }

  /**
   * Get oldest event timestamp for an event id, and a key.
   *
   * @param eventId Event id.
   * @param key     Key.
   * @return First event date or null if no events are found.
   */
  public Instant getOldestEvent(String eventId, String key) {
    try (Jedis jedis = jedisPool.getResource()) {
      var iterator = jedis.zrangeWithScores(eventKey(eventId, key), 0, 0).iterator();
      if (iterator.hasNext()) {
        Tuple tuple = iterator.next();
        return Instant.ofEpochMilli((long) tuple.getScore());
      }
    }

    return null;
  }

  /**
   * Remove all events older than threshold instant, using an exclusive upper bound.
   *
   * @param eventId    Event id.
   * @param key        Key.
   * @param threshold  Threshold instant.
   * @return number of removed entries.
   */
  public long removeEventsOlderThan(String eventId, String key, Instant threshold) {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.zremrangeByScore(eventKey(eventId, key), "-inf", "(" + threshold.toEpochMilli());
    }
  }

  /**
   * Remove list of events for an event id, and a key.
   *
   * @param eventId Event id.
   * @param key     Key.
   */
  public void remove(String eventId, String key) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.del(eventKey(eventId, key));
    }
  }

  private String eventKey(String eventId, String key) {
    return eventId + KEY_SEPARATOR + key;
  }
}
