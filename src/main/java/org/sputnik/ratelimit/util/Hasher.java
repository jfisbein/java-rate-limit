package org.sputnik.ratelimit.util;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


public class Hasher {

  private final byte[] secretBytes;


  /**
   * @param secret secret to use
   */
  public Hasher(String secret) {
    secretBytes = secret.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Hash text using HMAC SHA 256 and secret defined in constructor.
   *
   * @param text String to hash.
   * @return hashed string.
   */
  public String convertToHmacSHA256(String text) {
    // Encode the text into bytes using UTF-8 and digest it
    byte[] digest = newMacInstance().doFinal(text.getBytes(StandardCharsets.UTF_8));

    // convert the digest into a string
    return Base64.getEncoder().encodeToString(digest);
  }

  private Mac newMacInstance() {
    Mac newMac;
    try {
      SecretKey key = new SecretKeySpec(secretBytes, "HmacSHA256");
      newMac = Mac.getInstance(key.getAlgorithm());
      newMac.init(key);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException(e);
    }

    return newMac;
  }
}
