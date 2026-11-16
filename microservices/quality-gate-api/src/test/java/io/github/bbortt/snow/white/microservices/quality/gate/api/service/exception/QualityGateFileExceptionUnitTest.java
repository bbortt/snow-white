/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class QualityGateFileExceptionUnitTest {

  @Nested
  class ConstructorTest {

    @Test
    void shouldNameLocationAndProblem() {
      var fixture = new QualityGateFileException(
        "file:/etc/gates.yaml",
        "it does not exist"
      );

      assertThat(fixture)
        .hasMessage(
          "Quality-Gate file 'file:/etc/gates.yaml' cannot be used: it does not exist!"
        )
        .hasNoCause();
    }

    @Test
    void shouldKeepCause() {
      var cause = new IllegalStateException("boom");

      var fixture = new QualityGateFileException(
        "file:/etc/gates.yaml",
        "it could not be parsed",
        cause
      );

      assertThat(fixture)
        .hasMessage(
          "Quality-Gate file 'file:/etc/gates.yaml' cannot be used: it could not be parsed!"
        )
        .hasCause(cause);
    }

    @Test
    void shouldBeUnchecked() {
      assertThat(new QualityGateFileException("l", "p")).isInstanceOf(
        RuntimeException.class
      );
    }
  }
}
