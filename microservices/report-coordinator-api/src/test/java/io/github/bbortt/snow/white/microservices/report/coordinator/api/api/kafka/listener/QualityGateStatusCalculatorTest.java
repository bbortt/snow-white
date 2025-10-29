/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.kafka.listener;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.NOT_STARTED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.PASSED;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class QualityGateStatusCalculatorTest {

  private QualityGateStatusCalculator fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new QualityGateStatusCalculator();
  }

  @Nested
  class WithUpdatedReportStatus {

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
    void shouldReturnInProgress_whenAnyTestResultsHaveNotArrivedYet() {
      var report = QualityGateReport.builder()
        .calculationId(UUID.fromString("87ddf55c-4f38-4a00-b067-c3dc7abbdfcb"))
        .apiTests(
          Set.of(
            ApiTest.builder().apiTestResults(emptySet()).build(),
            ApiTest.builder()
              .apiTestResults(Set.of(new ApiTestResult()))
              .build()
          )
        )
        .reportParameter(mock(ReportParameter.class))
        .build();

      QualityGateReport result = fixture.withUpdatedReportStatus(report);

      assertThat(result.getReportStatus()).isEqualTo(IN_PROGRESS);
    }

    @Test
    void shouldReturnFailed_whenAnyIncludedTestFailedAndNoneInProgress() {
      var report = QualityGateReport.builder()
        .calculationId(UUID.fromString("26578776-7886-4be4-84ac-110c4e5307b8"))
        .apiTests(
          Set.of(
            ApiTest.builder()
              .apiTestResults(
                Set.of(
                  ApiTestResult.builder()
                    .apiTestCriteria("criteria1")
                    .coverage(ONE)
                    .includedInReport(TRUE)
                    .duration(Duration.ofSeconds(1))
                    .apiTest(mock(ApiTest.class))
                    .build()
                )
              )
              .build(),
            ApiTest.builder()
              .apiTestResults(
                Set.of(
                  ApiTestResult.builder()
                    .apiTestCriteria("criteria1")
                    .coverage(ZERO)
                    .includedInReport(TRUE)
                    .duration(Duration.ofSeconds(1))
                    .apiTest(mock(ApiTest.class))
                    .build()
                )
              )
              .build()
          )
        )
        .reportParameter(mock(ReportParameter.class))
        .build();

      QualityGateReport result = fixture.withUpdatedReportStatus(report);

      assertThat(result.getReportStatus()).isEqualTo(FAILED);
    }

    @Test
    void should_not_returnFailed_whenAnyUnincludedTestFailed() {
      var report = QualityGateReport.builder()
        .calculationId(UUID.fromString("fadeeadc-8a60-4a2f-ac87-66c339c175f2"))
        .apiTests(
          Set.of(
            ApiTest.builder()
              .apiTestResults(
                Set.of(
                  ApiTestResult.builder()
                    .apiTestCriteria("criteria")
                    .coverage(ZERO)
                    .includedInReport(FALSE)
                    .duration(Duration.ofSeconds(1))
                    .apiTest(mock(ApiTest.class))
                    .build()
                )
              )
              .build()
          )
        )
        .reportParameter(mock(ReportParameter.class))
        .build();

      QualityGateReport result = fixture.withUpdatedReportStatus(report);

      assertThat(result.getReportStatus()).isNotEqualTo(FAILED);
    }

    @Test
    void shouldReturnPassedWhenAllIncludedTestsPassedAndNoneInProgress() {
      var report = QualityGateReport.builder()
        .calculationId(UUID.fromString("9c18ee4b-0fc4-4677-90e1-47eae4092d9b"))
        .apiTests(
          Set.of(
            ApiTest.builder()
              .apiTestResults(
                Set.of(
                  ApiTestResult.builder()
                    .apiTestCriteria("criteria1")
                    .coverage(ZERO)
                    .includedInReport(FALSE)
                    .duration(Duration.ofSeconds(1))
                    .apiTest(mock(ApiTest.class))
                    .build()
                )
              )
              .build(),
            ApiTest.builder()
              .apiTestResults(
                Set.of(
                  ApiTestResult.builder()
                    .apiTestCriteria("criteria2")
                    .coverage(ONE)
                    .includedInReport(TRUE)
                    .duration(Duration.ofSeconds(1))
                    .apiTest(mock(ApiTest.class))
                    .build()
                )
              )
              .build()
          )
        )
        .reportParameter(mock(ReportParameter.class))
        .build();

      QualityGateReport result = fixture.withUpdatedReportStatus(report);

      assertThat(result.getReportStatus()).isEqualTo(PASSED);
    }
  }
}
