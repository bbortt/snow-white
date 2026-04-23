/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.FINISHED_EXCEPTIONALLY;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.NOT_STARTED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.PASSED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.service.QualityGateStatusCalculator.TERMINAL_STATUS;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class QualityGateStatusCalculatorTest {

  private QualityGateStatusCalculator fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new QualityGateStatusCalculator();
  }

  @Nested
  class WithUpdatedReportStatusTest {

    @Test
    void shouldReturnNotStarted_whenNoApiTestsArePresent() {
      var report = QualityGateReport.builder()
        .calculationId(UUID.fromString("694b4335-3748-42e2-9901-48705374ddb7"))
        .apiTests(emptySet())
        .reportParameter(mock(ReportParameter.class))
        .build();

      QualityGateReport result = fixture.withUpdatedReportStatus(report);

      assertThat(result.getReportStatus()).isEqualTo(NOT_STARTED);
    }

    @Test
    void shouldReturnInProgress_whenAnyApiTestIsStillInProgress() {
      var report = QualityGateReport.builder()
        .calculationId(UUID.fromString("87ddf55c-4f38-4a00-b067-c3dc7abbdfcb"))
        .apiTests(
          Set.of(
            ApiTest.builder()
              .reportStatus(IN_PROGRESS.getVal())
              .apiType(OPENAPI.getVal())
              .build(),
            ApiTest.builder()
              .reportStatus(PASSED.getVal())
              .apiType(OPENAPI.getVal())
              .build()
          )
        )
        .reportParameter(mock(ReportParameter.class))
        .build();

      QualityGateReport result = fixture.withUpdatedReportStatus(report);

      assertThat(result.getReportStatus()).isEqualTo(IN_PROGRESS);
    }

    @Test
    void shouldReturnFailed_whenAnyApiTestFailedAndNoneInProgress() {
      var report = QualityGateReport.builder()
        .calculationId(UUID.fromString("26578776-7886-4be4-84ac-110c4e5307b8"))
        .apiTests(
          Set.of(
            ApiTest.builder()
              .reportStatus(PASSED.getVal())
              .apiType(OPENAPI.getVal())
              .build(),
            ApiTest.builder()
              .reportStatus(FAILED.getVal())
              .apiType(OPENAPI.getVal())
              .build()
          )
        )
        .reportParameter(mock(ReportParameter.class))
        .build();

      QualityGateReport result = fixture.withUpdatedReportStatus(report);

      assertThat(result.getReportStatus()).isEqualTo(FAILED);
    }

    @Test
    void shouldReturnFinishedExceptionally_whenAnyApiTestFailedAndNoneInProgress() {
      var report = QualityGateReport.builder()
        .calculationId(UUID.fromString("26578776-7886-4be4-84ac-110c4e5307b8"))
        .apiTests(
          Set.of(
            ApiTest.builder()
              .reportStatus(PASSED.getVal())
              .apiType(OPENAPI.getVal())
              .build(),
            ApiTest.builder()
              .reportStatus(FINISHED_EXCEPTIONALLY.getVal())
              .apiType(OPENAPI.getVal())
              .build()
          )
        )
        .reportParameter(mock(ReportParameter.class))
        .build();

      QualityGateReport result = fixture.withUpdatedReportStatus(report);

      assertThat(result.getReportStatus()).isEqualTo(FINISHED_EXCEPTIONALLY);
    }

    @Test
    void shouldReturnPassedWhenAllApiTestsPassed() {
      var report = QualityGateReport.builder()
        .calculationId(UUID.fromString("9c18ee4b-0fc4-4677-90e1-47eae4092d9b"))
        .apiTests(
          Set.of(
            ApiTest.builder()
              .reportStatus(PASSED.getVal())
              .apiType(OPENAPI.getVal())
              .build(),
            ApiTest.builder()
              .reportStatus(PASSED.getVal())
              .apiType(OPENAPI.getVal())
              .build()
          )
        )
        .reportParameter(mock(ReportParameter.class))
        .build();

      QualityGateReport result = fixture.withUpdatedReportStatus(report);

      assertThat(result.getReportStatus()).isEqualTo(PASSED);
    }

    public static Stream<
      ReportStatus
    > shouldReturnImmediately_whenReportStatusIsAlreadyTerminal() {
      return TERMINAL_STATUS.stream();
    }

    @MethodSource
    @ParameterizedTest
    void shouldReturnImmediately_whenReportStatusIsAlreadyTerminal(
      ReportStatus terminalStatus
    ) {
      var qualityGateReport = mock(QualityGateReport.class);
      doReturn(terminalStatus).when(qualityGateReport).getReportStatus();

      assertThat(fixture.withUpdatedReportStatus(qualityGateReport)).isEqualTo(
        qualityGateReport
      );

      verify(qualityGateReport).getReportStatus();
      verifyNoMoreInteractions(qualityGateReport);
    }
  }
}
