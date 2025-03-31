package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.exception;

import java.util.List;

public class UnparseableOpenApiException extends Throwable {

  public UnparseableOpenApiException(List<String> messages) {
    super("Unparsable OpenAPI: " + String.join(", ", messages));
  }
}
