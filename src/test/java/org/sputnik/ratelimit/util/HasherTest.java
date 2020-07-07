package org.sputnik.ratelimit.util;

import static org.junit.Assert.assertEquals;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;

class HasherTest {

  @Test
  void testPasswordHasher()
    throws InvalidKeyException, NoSuchAlgorithmException {
    String hashed = Hasher.convertToHmacSHA256("mi barba tiene 3 pelos", "note lo digo");
    assertEquals("IpdSn+krpu8J9lx+6NG9MmCxEP6fjqBpPK25EYTNp+c=", hashed);
  }
}
