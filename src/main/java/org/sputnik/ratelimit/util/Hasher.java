package org.sputnik.ratelimit.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Hasher {

  /**
   * Hash text using HMAC SHA 256.
   *
   * @param text String to hash.
   * @param secret secret to use.
   * @return hashed string.
   * @throws NoSuchAlgorithmException if no Provider supports a MacSpi implementation for the specified algorithm.
   * @throws InvalidKeyException if the given key is inappropriate for initializing this MAC.
   */
  public static String convertToHmacSHA256(String text, String secret)
      throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {

    SecretKey key = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
    Mac mac = Mac.getInstance(key.getAlgorithm());
    mac.init(key);

    // Encode the text into bytes using UTF-8 and digest it
    byte[] digest = mac.doFinal(text.getBytes(StandardCharsets.UTF_8));

    // convert the digest into a string
    return Base64.getEncoder().encodeToString(digest);
  }
}
