package org.sputnik.ratelimit.service;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Protocol;

public class JedisConfiguration {

  private GenericObjectPoolConfig poolConfig;

  private String host = Protocol.DEFAULT_HOST;

  private int port = Protocol.DEFAULT_PORT;

  private int timeout = Protocol.DEFAULT_TIMEOUT;

  private String password = null;

  private int database = Protocol.DEFAULT_DATABASE;

  private String clientName = null;

  public JedisConfiguration setHost(String host) {
    this.host = host;
    return this;
  }

  public JedisConfiguration setPoolConfig(GenericObjectPoolConfig poolConfig) {
    this.poolConfig = poolConfig;
    return this;
  }

  public JedisConfiguration setPort(int port) {
    this.port = port;
    return this;
  }

  public JedisConfiguration setTimeout(int timeout) {
    this.timeout = timeout;
    return this;
  }

  public JedisConfiguration setPassword(String password) {
    this.password = password;
    return this;
  }

  public JedisConfiguration setDatabase(int database) {
    this.database = database;
    return this;
  }

  public JedisConfiguration setClientName(String clientName) {
    this.clientName = clientName;
    return this;
  }

  public GenericObjectPoolConfig getPoolConfig() {
    if (poolConfig == null) {
			poolConfig = new GenericObjectPoolConfig();
    }
    return poolConfig;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public int getTimeout() {
    return timeout;
  }

  public String getPassword() {
    return password;
  }

  public int getDatabase() {
    return database;
  }

  public String getClientName() {
    return clientName;
  }
}
