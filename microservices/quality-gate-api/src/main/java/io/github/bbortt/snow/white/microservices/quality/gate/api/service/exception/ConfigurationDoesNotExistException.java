package io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception;

import static java.lang.String.format;

public class ConfigurationDoesNotExistException extends Exception {

  public ConfigurationDoesNotExistException(String name) {
    super(format("Quality-Gate configuration '%s' does not exist!", name));
  }
}
