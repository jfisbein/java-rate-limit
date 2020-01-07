package org.sputnik.ratelimit.service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.sputnik.ratelimit.dao.RedisDao;
import org.sputnik.ratelimit.exeception.DuplicatedEventKeyException;
import org.sputnik.ratelimit.util.EventConfig;
import org.sputnik.ratelimit.util.PasswordHasher;
import redis.clients.jedis.JedisPool;

public class VelocityControl {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(VelocityControl.class);
  private final RedisDao redisDao;
  private final String hashingSecret;
  private final Map<String, EventConfig> eventsConfig = new HashMap<>();

  /**
   * Constructor.
   *
   * @param jedisConf Jedis configuration.
   * @param hashingSecret secret for hashing values
   * @param eventConfigs Events configuration.
   */
  public VelocityControl(JedisConfiguration jedisConf, String hashingSecret, EventConfig... eventConfigs) {
    this.hashingSecret = hashingSecret;
    JedisPool jedisPool = new JedisPool(jedisConf.getPoolConfig(), jedisConf.getHost(), jedisConf.getPort(),
        jedisConf.getTimeout(), jedisConf.getPassword(), jedisConf.getDatabase(), jedisConf.getClientName());
    redisDao = new RedisDao(jedisPool);
    validateEventsConfig(eventConfigs);
    eventsConfig.putAll(Stream.of(eventConfigs).collect(Collectors.toMap(EventConfig::getEventId, Function.identity())));
  }

  /**
   * Constructor.
   *
   * @param host Redis host.
   * @param port Redis port.
   * @param hashingSecret secret for hashing values
   * @param eventsConf Events configuration.
   */
  public VelocityControl(String host, int port, String hashingSecret, EventConfig... eventsConf) {
    this(new JedisConfiguration().setHost(host).setPort(port), hashingSecret, eventsConf);
  }

  /**
   * Checks if the event can be done without exceeding the configured limits.
   *
   * @param eventId Event identifier.
   * @param key event execution key.
   * @return <code>true</code> if the event can be done, <code>false</code> otherwise.
   */
  public boolean canDoEvent(String eventId, String key) {
    boolean confirm = false;

    if (isValidRequest(eventId, key)) {
      String hashedKey = hashText(key);
      logger.debug("Event ({}) exists, checking if it could be performed", eventId);

      EventConfig eventConfig = eventsConfig.get(eventId);
      Duration eventTime = eventConfig.getMinTime();
      Long eventMaxIntents = eventConfig.getMaxIntents();
      Long eventIntents = redisDao.getListLength(eventId, hashedKey);
      if (eventIntents != null && eventIntents >= eventMaxIntents) {
        logger.debug("Checking dates");
        Instant firstDate = redisDao.getListFirstEventElement(eventId, hashedKey, eventMaxIntents);

        long millisDifference = ChronoUnit.MILLIS.between(firstDate, Instant.now());

        if (millisDifference > eventTime.toMillis()) {
          redisDao.removeListFirstElement(eventId, hashedKey);
          logger.info("Event [{}] could be performed [{}/{}]", eventId, eventIntents, eventMaxIntents);
          confirm = true;
        }
      } else {
        logger.info("Event [{}] could be performed [{}/{}]", eventId, eventIntents, eventMaxIntents);
        confirm = true;
      }
    }

    if (!confirm) {
      logger.info("The event: {} could NOT be performed", eventId);
    }

    return confirm;
  }

  /**
   * Indicates that the event has been done.
   *
   * @param eventId Event identifier.
   * @param key event execution key.
   * @return <code>true</code> if the event execution has been recorded successfully, <code>false</code> otherwise.
   */
  public boolean doEvent(String eventId, String key) {
    boolean eventRecorded = false;
    if (isValidRequest(eventId, key)) {
      EventConfig eventConfig = eventsConfig.get(eventId);

      redisDao.addEvent(eventId, hashText(key), eventConfig.getMinTime());
      logger.info("Event [{}] recorded", eventId);
      eventRecorded = true;
    }

    return eventRecorded;
  }

  /**
   * Clear all the event execution for the provided key.
   *
   * @param eventId Event identifier.
   * @param key event execution key.
   * @return <code>true</code> if the event execution has been cleared successfully, <code>false</code> otherwise.
   */
  public boolean reset(String eventId, String key) {
    boolean eventDeleted = false;
    if (isValidRequest(eventId, key)) {
      redisDao.remove(eventId, hashText(key));
      logger.info("Event [{}] deleted", eventId);
      eventDeleted = true;
    }

    return eventDeleted;
  }

  private String hashText(String text) {
    String hash = text;
    try {
      logger.debug("Hashing key");
      hash = PasswordHasher.convertToHmacSHA256(text, hashingSecret);
    } catch (Exception e) {
      logger.warn("Error hashing text, using clear text: {}", e.getMessage(), e);
    }

    return hash;
  }

  /**
   * Validates the request. Checks if the key is not blank and the eventId is configured.
   */
  private boolean isValidRequest(String eventId, String key) {
    boolean valid = false;
    if (StringUtils.isNotBlank(key)) {
      logger.debug("Checking the existence of event: {}", eventId);
      if (eventsConfig.containsKey(eventId)) {
        valid = true;
      } else {
        logger.error("Invalid request - The eventId [{}] is not found", eventId);
      }
    } else {
      logger.error("Invalid request - The key is blank");
    }

    return valid;
  }

  private void validateEventsConfig(EventConfig... eventConfigs) {
    Set<String> keys = new HashSet<>();
    for (EventConfig cfg : eventConfigs) {
      String eventId = cfg.getEventId();
      if (keys.contains(eventId)) {
        throw new DuplicatedEventKeyException(eventId);
      }
      keys.add(eventId);
    }
  }
}
