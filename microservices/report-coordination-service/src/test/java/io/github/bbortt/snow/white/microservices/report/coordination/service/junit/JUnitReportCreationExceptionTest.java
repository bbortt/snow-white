/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

class JUnitReportCreationExceptionTest {

  @Test
  void constructorAddsMessage() {
    var cause = mock(Throwable.class);

    var fixture = new JUnitReportCreationException(cause);

    assertThat(fixture).satisfies(
      exception -> assertThat(exception.getCause()).isEqualTo(cause),
      exception ->
        assertThat(exception.getMessage()).isEqualTo(
          "Failed to create JUnit report!"
        )
    );
  }
}
