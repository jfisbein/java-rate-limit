package org.sputnik.ratelimit.dao;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnik.ratelimit.util.Constants;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisDao {

  private static final Logger logger = LoggerFactory.getLogger(RedisDao.class);
  private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  protected final JedisPool jedisPool;

  /**
   * Constructor.
   *
   * @param jedisPool Jedis Pool.
   */
  public RedisDao(JedisPool jedisPool) {
    this.jedisPool = jedisPool;
  }

  /**
   * Add Event.
   *
   * @param eventId Event id.
   * @param key Key.
   * @param duration Max duration.
   */
  public void addEvent(String eventId, String key, Duration duration) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.rpush(eventId + Constants.KEY_SEPARATOR + key, dtf.format(LocalDateTime.now()));
      if (duration != null) {
        jedis.expire(eventId + Constants.KEY_SEPARATOR + key, (int) duration.getSeconds());
      }
    }
  }

  /**
   * Get number of events for an event id, and a key.
   *
   * @param eventId Event id.
   * @param key Key.
   * @return number of events.
   */
  public Long getListLength(String eventId, String key) {
    Long result;
    try (Jedis jedis = jedisPool.getResource()) {
      result = jedis.llen(eventId + Constants.KEY_SEPARATOR + key);
    }

    return result;
  }

  /**
   * Get first list element date for an event id, and a key.
   *
   * @param eventId Event id.
   * @param key Key.
   * @return First list element.
   */
  public Instant getListFirstElement(String eventId, String key) {
    Instant result;
    String aux;
    try (Jedis jedis = jedisPool.getResource()) {
      aux = jedis.lindex(eventId + Constants.KEY_SEPARATOR + key, 0);
      result = parseDateTime(aux);
    }

    return result;
  }

  public Instant getListFirstEventElement(String eventId, String key, Long eventMaxIntents) {
    Instant result;
    String aux;
    try (Jedis jedis = jedisPool.getResource()) {
      String redisKey = eventId + Constants.KEY_SEPARATOR + key;
      long length = jedis.llen(redisKey);
      long index = Math.max(0, length - eventMaxIntents);
      aux = jedis.lindex(redisKey, index);
      result = parseDateTime(aux);
    }

    return result;
  }

  /**
   * Remove first list element for an event id, and a key.
   *
   * @param eventId Event id.
   * @param key Key.
   * @return first list element.
   */
  public Instant removeListFirstElement(String eventId, String key) {
    Instant result = null;
    String aux;
    try (Jedis jedis = jedisPool.getResource()) {
      aux = jedis.lpop(eventId + Constants.KEY_SEPARATOR + key);
      if (aux != null) {
        result = parseDateTime(aux);
      }
    }

    return result;
  }

  /**
   * Add expiration to a list.
   *
   * @param eventId Event id.
   * @param key Key.
   * @param seconds seconds to expire.
   */
  public void setListExpires(String eventId, String key, Integer seconds) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.expire(eventId + Constants.KEY_SEPARATOR + key, seconds);
    }
  }

  /**
   * Remove list of events for an event id, and a key.
   *
   * @param eventId Event id.
   * @param key Key.
   */
  public void remove(String eventId, String key) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.del(eventId + Constants.KEY_SEPARATOR + key);
    }
  }

  private Instant parseDateTime(String dateTime) {
    Instant result = null;
    try {

      LocalDateTime parse = LocalDateTime.parse(dateTime, dtf);
      result = parse.atZone(ZoneId.systemDefault()).toInstant();
    } catch (DateTimeParseException e) {
      logger.warn("Error parsing date: {}", dateTime);
    }

    return result;
  }
}
