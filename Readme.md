java-rate-limit
=================

Library to embed velocity control functionality inside applications.

Usage
-----

```java
class LoginManager {
	VelocityControl vc = new VelocityControl(
		new JedisConfiguration().setHost("localhost"), 
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