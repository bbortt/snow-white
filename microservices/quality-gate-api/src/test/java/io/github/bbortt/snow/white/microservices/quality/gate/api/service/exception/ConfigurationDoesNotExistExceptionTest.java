/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConfigurationDoesNotExistExceptionTest {

  @Test
  void shouldConstructMessage() {
    var name = "quality-gate-configuration-name";

    assertThat(new ConfigurationDoesNotExistException(name)).hasMessage(
      "Quality-Gate configuration '%s' does not exist!",
      name
    );
  }
}
