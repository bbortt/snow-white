/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.TestData.defaultApiInformation;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpenApiNotIndexedExceptionTest {

  @Test
  void shouldConstructMessage() {
    assertThat(
      new OpenApiNotIndexedException(defaultApiInformation())
    ).hasMessage(
      "OpenApi identifier not indexed: { \"serviceName\": \"serviceName\", \"apiName\": \"apiName\", \"apiVersion\": \"apiVersion\" }"
    );
  }
}
