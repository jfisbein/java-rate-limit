package org.sputnik.ratelimit.service;

import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.LoggerFactory;
import org.sputnik.ratelimit.dao.EventsRedisRepository;
import org.sputnik.ratelimit.domain.CanDoResponse;
import org.sputnik.ratelimit.domain.CanDoResponse.CanDoResponseBuilder;
import org.sputnik.ratelimit.domain.CanDoResponse.Reason;
import org.sputnik.ratelimit.exeception.DuplicatedEventKeyException;
import org.sputnik.ratelimit.util.EventConfig;
import org.sputnik.ratelimit.util.Hasher;
import redis.clients.jedis.JedisPool;

public class RateLimiter implements Closeable {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RateLimiter.class);
  private final EventsRedisRepository eventsRedisRepository;
  private final String hashingSecret;
  private final Map<String, EventConfig> eventsConfig = new HashMap<>();
  private final JedisPool jedisPool;

  /**
   * Constructor.
   *
   * @param jedisConf     Jedis configuration.
   * @param hashingSecret secret for hashing values
   * @param eventConfigs  Events configuration.
   */
  public RateLimiter(JedisConfiguration jedisConf, String hashingSecret, EventConfig... eventConfigs) {
    this.hashingSecret = hashingSecret;
    jedisPool = new JedisPool(jedisConf.getPoolConfig(), jedisConf.getHost(), jedisConf.getPort(),
      jedisConf.getTimeout(), jedisConf.getPassword(), jedisConf.getDatabase(), jedisConf.getClientName());
    eventsRedisRepository = new EventsRedisRepository(jedisPool);
    validateEventsConfig(eventConfigs);
    eventsConfig.putAll(Stream.of(eventConfigs).collect(Collectors.toMap(EventConfig::getEventId, Function.identity())));
  }

  /**
   * Constructor.
   *
   * @param host          Redis host.
   * @param port          Redis port.
   * @param hashingSecret secret for hashing values
   * @param eventConfigs  Events configuration.
   */
  public RateLimiter(String host, int port, String hashingSecret, EventConfig... eventConfigs) {
    this(JedisConfiguration.builder().host(host).port(port).build(), hashingSecret, eventConfigs);
  }

  /**
   * Checks if the event can be done without exceeding the configured limits.
   *
   * @param eventId Event identifier.
   * @param key     event execution key.
   * @return Response object with information about if the event can be done, the reason, and wait time if it cannot be done because
   * exceeding event limits.
   */
  public CanDoResponse canDoEvent(String eventId, String key) {
    CanDoResponseBuilder builder = CanDoResponse.builder();
    if (isValidRequest(eventId, key)) {
      String hashedKey = hashText(key);
      logger.debug("Event ({}) exists, checking if it could be performed", eventId);

      EventConfig eventConfig = eventsConfig.get(eventId);
      Duration eventTime = eventConfig.getMinTime();
      Long eventMaxIntents = eventConfig.getMaxIntents();
      Long eventIntents = eventsRedisRepository.getListLength(eventId, hashedKey);

      if (eventIntents != null && eventIntents >= eventMaxIntents) {
        logger.debug("Checking dates");
        builder.eventsIntents(eventIntents);
        Instant firstDate = eventsRedisRepository.getListFirstEventElement(eventId, hashedKey, eventMaxIntents);

        long millisDifference = ChronoUnit.MILLIS.between(firstDate, Instant.now());

        if (millisDifference > eventTime.toMillis()) {
          eventsRedisRepository.removeListFirstElement(eventId, hashedKey);
          logger.info("Event [{}] could be performed [{}/{}]", eventId, eventIntents, eventMaxIntents);
          builder.canDo(true);
        } else {
          builder.reason(Reason.TOO_MANY_EVENTS);
          builder.waitMillis(eventTime.toMillis() - millisDifference);
          builder.canDo(false);
        }
      } else {
        logger.info("Event [{}] could be performed [{}/{}]", eventId, eventIntents, eventMaxIntents);
        builder.eventsIntents(eventIntents != null ? eventIntents : 0L);
        builder.canDo(true);
      }
    } else {
      builder.reason(Reason.INVALID_REQUEST);
      builder.canDo(false);
    }

    CanDoResponse response = builder.build();

    if (!response.getCanDo()) {
      logger.info("The event: {} could NOT be performed", eventId);
    }

    return response;
  }

  /**
   * Indicates that the event has been done.
   *
   * @param eventId Event identifier.
   * @param key     event execution key.
   * @return <code>true</code> if the event execution has been recorded successfully, <code>false</code> otherwise.
   */
  public boolean doEvent(String eventId, String key) {
    boolean eventRecorded = false;
    if (isValidRequest(eventId, key)) {
      EventConfig eventConfig = eventsConfig.get(eventId);

      eventsRedisRepository.addEvent(eventId, hashText(key), eventConfig.getMinTime());
      logger.info("Event [{}] recorded", eventId);
      eventRecorded = true;
    }

    return eventRecorded;
  }

  /**
   * Clear all the event execution for the provided key.
   *
   * @param eventId Event identifier.
   * @param key     event execution key.
   * @return <code>true</code> if the event execution has been cleared successfully, <code>false</code> otherwise.
   */
  public boolean reset(String eventId, String key) {
    boolean eventDeleted = false;
    if (isValidRequest(eventId, key)) {
      eventsRedisRepository.remove(eventId, hashText(key));
      logger.info("Event [{}] deleted", eventId);
      eventDeleted = true;
    }

    return eventDeleted;
  }

  /**
   * Get information about an event.
   *
   * @param eventId Event identifier.
   * @return Event configuration.
   */
  public Optional<EventConfig> getEventConfig(String eventId) {
    return Optional.ofNullable(eventsConfig.get(eventId));
  }

  /**
   * Hash text using Hasher utility class.
   *
   * @param text Texto to be hashed.
   * @return Hashed text.
   * @see Hasher
   */
  private String hashText(String text) {
    String hash = text;
    try {
      logger.debug("Hashing key");
      hash = Hasher.convertToHmacSHA256(text, hashingSecret);
    } catch (Exception e) {
      logger.warn("Error hashing text, using clear text: {}", e.getMessage());
    }

    return hash;
  }

  /**
   * Validates the request. Checks if the key is not blank, and the eventId is configured.
   */
  private boolean isValidRequest(String eventId, String key) {
    boolean valid = false;
    if (isNotBlank(key)) {
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

  /**
   * Validates no key are duplicated in the events config supplied.
   *
   * @param eventConfigs List of event configs to validate.
   * @throws DuplicatedEventKeyException when a key is duplicated.
   */
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

  /**
   * <p>Checks if a CharSequence is empty (""), null or whitespace only.</p>
   *
   * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
   *
   * <p><strong>Copied from Apache Commons StringUtils</strong></p>
   *
   * <pre>
   * isBlank(null)      = true
   * isBlank("")        = true
   * isBlank(" ")       = true
   * isBlank("bob")     = false
   * isBlank("  bob  ") = false
   * </pre>
   *
   * @param cs the CharSequence to check, may be null
   * @return {@code true} if the CharSequence is null, empty or whitespace only
   */
  private boolean isBlank(CharSequence cs) {
    int strLen;
    if (cs == null || (strLen = cs.length()) == 0) {
      return true;
    }
    for (int i = 0; i < strLen; i++) {
      if (!Character.isWhitespace(cs.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * <p>Checks if a CharSequence is not empty (""), not null and not whitespace only.</p>
   *
   * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
   *
   * <p><strong>Copied from Apache Commons StringUtils</strong></p>
   *
   * <pre>
   * isNotBlank(null)      = false
   * isNotBlank("")        = false
   * isNotBlank(" ")       = false
   * isNotBlank("bob")     = true
   * isNotBlank("  bob  ") = true
   * </pre>
   *
   * @param cs the CharSequence to check, may be null
   * @return {@code true} if the CharSequence is not empty and not null and not whitespace only
   */
  private boolean isNotBlank(CharSequence cs) {
    return !isBlank(cs);
  }

  @Override
  public void close() {
    jedisPool.close();
  }
}
