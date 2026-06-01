/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class QualityGateReportUnitTest {

  private QualityGateReport.QualityGateReportBuilder qualityGateReportBuilder;

  @BeforeEach
  void beforeEachSetup() {
    qualityGateReportBuilder = QualityGateReport.builder()
      .calculationId(UUID.fromString("577f2963-0fb4-44f8-930c-e8cc5e7e3476"))
      .reportParameter(mock());
  }

  @Nested
  class GetReportStatusTest {

    @EnumSource
    @ParameterizedTest
    void shouldTransformShortToEnumValue(ReportStatus reportStatus) {
      var fixture = qualityGateReportBuilder
        .reportStatus(reportStatus.getVal())
        .build();

      assertThat(fixture.getReportStatus()).isEqualTo(reportStatus);
    }
  }

  @Nested
  class WithReportStatusTest {

    @EnumSource
    @ParameterizedTest
    void shouldAppendReportStatus(ReportStatus reportStatus) {
      assertThat(
        qualityGateReportBuilder.build().withReportStatus(reportStatus)
      )
        .extracting(QualityGateReport::getReportStatus)
        .isEqualTo(reportStatus);
    }
  }
}
