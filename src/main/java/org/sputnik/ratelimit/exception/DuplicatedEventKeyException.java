package org.sputnik.ratelimit.exception;

public class DuplicatedEventKeyException extends RuntimeException {

  public DuplicatedEventKeyException(String duplicateKey) {
    super("Event Key " + duplicateKey + " duplicated");
  }
}
