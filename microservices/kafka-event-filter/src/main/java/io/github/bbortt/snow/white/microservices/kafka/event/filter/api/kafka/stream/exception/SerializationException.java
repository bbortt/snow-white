package io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka.stream.exception;

public class SerializationException extends RuntimeException {

  public SerializationException(String message, Exception cause) {
    super(message, cause);
  }
}
