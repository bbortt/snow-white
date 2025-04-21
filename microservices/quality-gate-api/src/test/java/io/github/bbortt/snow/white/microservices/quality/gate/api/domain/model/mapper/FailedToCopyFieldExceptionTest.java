/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FailedToCopyFieldExceptionTest {

  @Test
  void constructorAssignsMessageAndCause() {
    var message = "message";
    var cause = new Exception();

    var fixture = new FailedToCopyFieldException(message, cause);

    assertThat(fixture).hasMessage("Failed to copy field: %s", message);
    assertThat(fixture.getCause()).isEqualTo(cause);
  }
}
