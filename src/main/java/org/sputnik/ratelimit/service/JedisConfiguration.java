package org.sputnik.ratelimit.service;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Protocol;

@Builder
@Getter
public class JedisConfiguration {

  @Default
  private GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
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
  private String clientName = null;
}
