package org.sputnik.ratelimit.exeception;

public class DuplicatedEventKeyException extends RuntimeException {

  public DuplicatedEventKeyException(String duplicateKey) {
    super("Event Key " + duplicateKey + " duplicated");
  }
}
