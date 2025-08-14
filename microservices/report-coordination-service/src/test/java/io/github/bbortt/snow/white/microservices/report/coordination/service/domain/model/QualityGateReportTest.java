/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class QualityGateReportTest {

  @Nested
  class Builder {

    @Nested
    class Build {

      @Test
      void shouldAutofillCalculationId() {
        QualityGateReport qualityGateReport =
          QualityGateReport.builder().build();

        assertThat(qualityGateReport.getCalculationId()).isNotNull();
      }
    }
  }
}
