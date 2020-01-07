package org.sputnik.ratelimit.util;

import static org.junit.Assert.assertEquals;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;

public class PasswordHasherTest {

  @Test
  public void testPasswordHasher()
      throws InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {
    String hashed = PasswordHasher.convertToHmacSHA256("mi barba tiene 3 pelos", "note lo digo");
    assertEquals("IpdSn+krpu8J9lx+6NG9MmCxEP6fjqBpPK25EYTNp+c=", hashed);
  }
}
