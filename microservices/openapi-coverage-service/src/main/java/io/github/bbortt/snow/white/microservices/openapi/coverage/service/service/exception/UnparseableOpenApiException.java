package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.exception;

import static java.lang.String.join;
import static java.util.Objects.isNull;

import java.util.List;

public class UnparseableOpenApiException extends Throwable {

  public UnparseableOpenApiException(List<String> messages) {
    super(
      "Unparsable OpenAPI" +
      ((isNull(messages) || messages.isEmpty())
          ? " detected"
          : ": " + join(", ", messages))
    );
  }
}
