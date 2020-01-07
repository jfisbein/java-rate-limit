package org.sputnik.ratelimit.service;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.Test;


public class JedisConfigurationTest {

  @Test
  public void testSetGetHost() {
    String host = RandomStringUtils.randomAlphanumeric(10);
    assertEquals(host, new JedisConfiguration().setHost(host).getHost());
  }

  @Test
  public void testSetGetPoolConfig() {
    GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
    poolConfig.setTestOnCreate(true);
    assertEquals(poolConfig, new JedisConfiguration().setPoolConfig(poolConfig).getPoolConfig());
  }

  @Test
  public void testSetGetPort() {
    int port = Integer.parseInt(RandomStringUtils.randomNumeric(3));
    assertEquals(port, new JedisConfiguration().setPort(port).getPort());
  }

  @Test
  public void testSetGetTimeout() {
    int timeout = Integer.parseInt(RandomStringUtils.randomNumeric(3));
    assertEquals(timeout, new JedisConfiguration().setTimeout(timeout).getTimeout());
  }

  @Test
  public void testSetGetPassword() {
    String password = RandomStringUtils.randomAlphanumeric(10);
    assertEquals(password, new JedisConfiguration().setPassword(password).getPassword());
  }

  @Test
  public void testSetGetDatabase() {
    int database = Integer.parseInt(RandomStringUtils.randomNumeric(3));
    assertEquals(database, new JedisConfiguration().setDatabase(database).getDatabase());
  }

  @Test
  public void testSetGetClientName() {
    String clientName = RandomStringUtils.randomAlphanumeric(10);
    assertEquals(clientName, new JedisConfiguration().setClientName(clientName).getClientName());
  }
}
