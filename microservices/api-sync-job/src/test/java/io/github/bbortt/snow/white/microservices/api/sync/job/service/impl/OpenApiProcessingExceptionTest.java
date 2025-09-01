/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpenApiProcessingExceptionTest {

  @Test
  void constructorAssignsMessageAndCause() {
    var cause = new RuntimeException("Something nasty happened!");

    var fixture = new OpenApiProcessingException(cause);

    assertThat(fixture).satisfies(
      f -> assertThat(f).hasMessage("Failed to transform OpenAPI to JSON!"),
      f -> assertThat(f).hasCause(cause)
    );
  }
}
