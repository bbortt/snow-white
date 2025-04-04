package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.exception;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.OpenApiService;
import org.junit.jupiter.api.Test;

class OpenApiNotIndexedExceptionTest {

  @Test
  void shouldConstructMessage() {
    assertThat(
      new OpenApiNotIndexedException(
        new OpenApiService.OpenApiIdentifier(
          "otelServiceName",
          "apiName",
          "apiVersion"
        )
      )
    ).hasMessage(
      "OpenApi identifier not indexed: OpenApiIdentifier[otelServiceName=otelServiceName, apiName=apiName, apiVersion=apiVersion]"
    );
  }
}
