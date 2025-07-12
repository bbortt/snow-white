/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UnmodifiableConfigurationExceptionTest {

  @Test
  void shouldConstructMessage() {
    var name = "quality-gate-configuration-name";

    assertThat(new UnmodifiableConfigurationException(name)).hasMessage(
      "The Quality-Gate configuration '%s' is not modifiable!",
      name
    );
  }
}
