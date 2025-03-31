package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.exception;

import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.OpenApiService;

public class OpenApiNotIndexedException extends Throwable {

  public OpenApiNotIndexedException(
    OpenApiService.OpenApiIdentifier openApiIdentifier
  ) {
    super("OpenApi identifier not indexed: " + openApiIdentifier);
  }
}
