/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import org.junit.jupiter.api.Test;

class OpenApiNotIndexedExceptionTest {

  @Test
  void shouldConstructMessage() {
    assertThat(
      new OpenApiNotIndexedException(
        ApiInformation.builder()
          .serviceName("otelServiceName")
          .apiName("apiName")
          .apiVersion("apiVersion")
          .build()
      )
    ).hasMessage(
      "OpenApi identifier not indexed: { \"serviceName\": \"otelServiceName\",\"apiName\": \"apiName\",\"apiVersion\": \"apiVersion\" }"
    );
  }
}
