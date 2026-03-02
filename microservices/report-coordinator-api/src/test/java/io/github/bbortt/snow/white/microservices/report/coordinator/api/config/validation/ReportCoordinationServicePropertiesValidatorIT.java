/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.config.validation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.AbstractReportCoordinationServiceIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReportCoordinationServicePropertiesValidatorIT
  extends AbstractReportCoordinationServiceIT
{

  @Autowired
  private ReportCoordinationServicePropertiesValidator reportCoordinationServicePropertiesValidator;

  @Test
  void shouldBeRegisteredWithinSpringContext() {
    assertThat(reportCoordinationServicePropertiesValidator).isNotNull();
  }
}
