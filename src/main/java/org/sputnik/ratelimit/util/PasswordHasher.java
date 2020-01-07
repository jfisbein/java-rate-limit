package org.sputnik.ratelimit.util;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;

public class PasswordHasher {

  private PasswordHasher() {
  }

  /**
   * Hash text using HMAC SHA 256.
   *
   * @param chain String to hash.
   * @param secret secret to use.
   * @return hashed string.
   * @throws NoSuchAlgorithmException if no Provider supports a MacSpi implementation for the specified algorithm.
   * @throws InvalidKeyException if the given key is inappropriate for initializing this MAC.
   */
  public static String convertToHmacSHA256(String chain, String secret)
      throws NoSuchAlgorithmException, InvalidKeyException {

    SecretKey key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
    Mac mac = Mac.getInstance(key.getAlgorithm());
    mac.init(key);

    // Encode the chain into bytes using UTF-8 and digest it
    byte[] digest = mac.doFinal(chain.getBytes(StandardCharsets.UTF_8));

    // convert the digest into a string
    return new String(Base64.encodeBase64(digest));
  }
}
