java-rate-limit
=================

Java library to embed rate limit control functionality inside applications.

Usage
-----

```java
import org.sputnik.ratelimit.service.JedisConfiguration;
import org.sputnik.ratelimit.service.RateLimiter;

class LoginManager {
    // Configure rate limiter to allow maximum 3 attempts every hour.
  RateLimiter vc = new Limiter(JedisConfiguration.builder().setHost("localhost").build(), 
		new EventConfig("testLogin", 3, Duration.ofSeconds(3600)));
	
  private boolean doLogin(String username, String password) {
    boolean isValid = false;
    if (vc.canDoEvent("testLogin", username)) {
      vc.doEvent("testLogin", username);
      // check credentials and set isValid value
    } else {
      log.warn("User " + username + " blocked due to exceeding number of login attempts");
    }
		
    if (isValid) {
      vc.reset("testLogin", username);
    }
		
    return isValid;
  }
} 
```

Limitations
-----------

Some edge race conditions could lead to get a false positive or negative response from the method `canDoEvent`. 