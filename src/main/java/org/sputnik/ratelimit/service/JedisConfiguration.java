package org.sputnik.ratelimit.service;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

@Builder
@Getter
public class JedisConfiguration {

  @Default
  private final JedisPoolConfig poolConfig = new JedisPoolConfig();
  @Default
  private final String host = Protocol.DEFAULT_HOST;
  @Default
  private final int port = Protocol.DEFAULT_PORT;
  @Default
  private final int timeout = Protocol.DEFAULT_TIMEOUT;
  @Default
  private final String password = null;
  @Default
  private final int database = Protocol.DEFAULT_DATABASE;
  @Default
  private final String clientName = "rate-limiter";

  public JedisPool createPool() {
    return new JedisPool(poolConfig, host, port, timeout, password, database, clientName);
  }
}
