package org.sputnik.ratelimit.dao;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Repository to manage Events persistence.
 */
public class EventsRedisRepository {

  private static final Logger logger = LoggerFactory.getLogger(EventsRedisRepository.class);
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
      jedis.rpush(redisKey, Long.toString(System.currentTimeMillis()));
      if (duration != null) {
        jedis.expire(redisKey, (int) duration.getSeconds());
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
  public long getListLength(String eventId, String key) {
    long result;
    try (Jedis jedis = jedisPool.getResource()) {
      result = jedis.llen(eventKey(eventId, key));
    }

    return result;
  }

  /**
   * Get first list element date for an event id, and a key.
   *
   * @param eventId Event id.
   * @param key     Key.
   * @return First list element.
   */
  public Instant getListFirstElement(String eventId, String key) {
    Instant result;
    try (Jedis jedis = jedisPool.getResource()) {
      String aux = jedis.lindex(eventKey(eventId, key), 0);
      result = parseTimeStamp(aux);
    }

    return result;
  }

  public Instant getListFirstEventElement(String eventId, String key, Long eventMaxIntents) {
    Instant result;
    try (Jedis jedis = jedisPool.getResource()) {
      String redisKey = eventKey(eventId, key);
      long length = jedis.llen(redisKey);
      long index = Math.max(0, length - eventMaxIntents);
      String aux = jedis.lindex(redisKey, index);
      result = parseTimeStamp(aux);
    }

    return result;
  }

  /**
   * Remove first list element for an event id, and a key.
   *
   * @param eventId Event id.
   * @param key     Key.
   * @return first list element.
   */
  public Instant removeListFirstElement(String eventId, String key) {
    Instant result = null;
    String aux;
    try (Jedis jedis = jedisPool.getResource()) {
      aux = jedis.lpop(eventKey(eventId, key));
      if (aux != null) {
        result = parseTimeStamp(aux);
      }
    }

    return result;
  }

  private Instant parseTimeStamp(String textTimeInMillis) {
    Instant instant = null;

    try {
      instant = Instant.ofEpochMilli(Long.parseLong(textTimeInMillis));
    } catch (NumberFormatException e) {
      logger.warn("Error parsing date: {}", textTimeInMillis);
    }

    return instant;
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
