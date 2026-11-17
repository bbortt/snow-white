/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QualityGateNotFoundExceptionUnitTest {

  @Test
  void shouldConstructMessage() {
    assertThat(QualityGateNotFoundException.forGateName("foo")).hasMessage(
      "Quality-Gate configuration 'foo' does not exist!"
    );
  }

  @Test
  void shouldSurfaceTheOwnersMessageVerbatim() {
    assertThat(
      QualityGateNotFoundException.surfacing("whatever the owner said")
    ).hasMessage("whatever the owner said");
  }
}
