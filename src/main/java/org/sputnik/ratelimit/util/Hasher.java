package org.sputnik.ratelimit.util;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


public class Hasher {

  private final Mac mac;

  /**
   * @param secret secret to use
   * @throws NoSuchAlgorithmException if no Provider supports a MacSpi implementation for the specified algorithm.
   * @throws InvalidKeyException      if the given key is inappropriate for initializing this MAC.
   */
  public Hasher(String secret) throws NoSuchAlgorithmException, InvalidKeyException {
    SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    mac = Mac.getInstance(key.getAlgorithm());
    mac.init(key);
  }

  /**
   * Hash text using HMAC SHA 256 and secret defined in constructor.
   *
   * @param text String to hash.
   * @return hashed string.
   */
  public String convertToHmacSHA256(String text) {
    // Encode the text into bytes using UTF-8 and digest it
    byte[] digest = mac.doFinal(text.getBytes(StandardCharsets.UTF_8));

    // convert the digest into a string
    return Base64.getEncoder().encodeToString(digest);
  }
}
