/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka.stream.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SerializationExceptionTest {

  @Test
  void constructorAssignsMessageAndCause() {
    var message = "message";
    var cause = new Exception();

    var fixture = new SerializationException(message, cause);

    assertThat(fixture).hasMessage(message);
    assertThat(fixture.getCause()).isEqualTo(cause);
  }
}
