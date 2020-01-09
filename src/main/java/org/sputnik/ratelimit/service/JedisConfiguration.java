package org.sputnik.ratelimit.service;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

@Builder
@Getter
public class JedisConfiguration {

  @Default
  private JedisPoolConfig poolConfig = new JedisPoolConfig();
  @Default
  private String host = Protocol.DEFAULT_HOST;
  @Default
  private int port = Protocol.DEFAULT_PORT;
  @Default
  private int timeout = Protocol.DEFAULT_TIMEOUT;
  @Default
  private String password = null;
  @Default
  private int database = Protocol.DEFAULT_DATABASE;
  @Default
  private String clientName = "rate-limiter";
}
