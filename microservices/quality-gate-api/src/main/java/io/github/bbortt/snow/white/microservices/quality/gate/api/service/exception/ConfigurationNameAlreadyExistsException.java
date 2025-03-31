package io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception;

import static java.lang.String.format;

public class ConfigurationNameAlreadyExistsException extends Throwable {

  public ConfigurationNameAlreadyExistsException(String name) {
    super(format("Quality-Gate configuration '%s' does already exist!", name));
  }
}
