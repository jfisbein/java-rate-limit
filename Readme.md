[![](https://github.com/jfisbein/java-rate-limit/workflows/Java%20CI/badge.svg)](https://github.com/jfisbein/java-rate-limit)
[![JitPack](https://jitpack.io/v/jfisbein/java-rate-limit.svg)](https://jitpack.io/#jfisbein/java-rate-limit)

java-rate-limit
=================

Java library to embed rate limit control functionality inside applications.

Usage
-----

```java
import org.sputnik.ratelimit.domain.CanDoResponse;import org.sputnik.ratelimit.service.JedisConfiguration;
import org.sputnik.ratelimit.service.RateLimiter;

class LoginManager {
    // Configure rate limiter to allow maximum 3 attempts every hour.
  RateLimiter vc = new RateLimiter(JedisConfiguration.builder().setHost("localhost").build(), 
		new EventConfig("testLogin", 3, Duration.ofSeconds(3600)));
	
  private boolean doLogin(String username, String password) {
    boolean isValid = false;
    CanDoResponse response = vc.canDoEvent("testLogin", username);
    if (response.getCanDo()) {
      vc.doEvent("testLogin", username);
      // TODO: check credentials and set isValid value
    } else {
      log.warn("User " + username + " blocked due to exceeding number of login attempt, for " + (response.getWaitMillis()/1000) + " seconds");
    }
		
    if (isValid) {
      vc.reset("testLogin", username);
    }
		
    return isValid;
  }
} 
```

Maven & Gradle
--------------

You can integrate the library in mave or gradle using JitPack: https://jitpack.io/#jfisbein/java-rate-limit


Javadoc
-------
Javadoc is available at https://javadoc.jitpack.io/com/github/jfisbein/java-rate-limit/latest/javadoc/index.html


Limitations
-----------

As no synchronization is implemented, some edge race conditions could lead to get a false positive or negative response from the method `canDoEvent`. 
