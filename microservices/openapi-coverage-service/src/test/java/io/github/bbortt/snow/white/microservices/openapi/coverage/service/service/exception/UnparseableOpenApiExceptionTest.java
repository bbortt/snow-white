/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.exception;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class UnparseableOpenApiExceptionTest {

  @Test
  void shouldConstructMessage_withoutAnyMessages() {
    assertThat(new UnparseableOpenApiException(emptyList())).hasMessage(
      "Unparsable OpenAPI detected"
    );
  }

  @Test
  void shouldConstructMessage_withSingleMessage() {
    assertThat(
      new UnparseableOpenApiException(singletonList("foo"))
    ).hasMessage("Unparsable OpenAPI: foo");
  }

  @Test
  void shouldConstructMessage_withListOfMessageS() {
    assertThat(
      new UnparseableOpenApiException(List.of("foo", "bar"))
    ).hasMessage("Unparsable OpenAPI: foo, bar");
  }
}
