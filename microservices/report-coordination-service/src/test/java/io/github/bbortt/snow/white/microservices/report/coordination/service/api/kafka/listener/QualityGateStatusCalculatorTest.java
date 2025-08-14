/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.kafka.listener;

import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.NOT_STARTED;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.PASSED;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import java.util.Set;
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
  class WithUpdatedReportStatus {

    static Stream<Set<ApiTest>> shouldReturnNotStartedWhenApiTestsEmpty() {
      return Stream.of(emptySet(), null);
    }

    @MethodSource
    @ParameterizedTest
    void shouldReturnNotStartedWhenApiTestsEmpty(Set<ApiTest> apiTests) {
      QualityGateReport report = new QualityGateReport().withApiTests(apiTests);

      QualityGateReport result = fixture.withUpdatedReportStatus(report);

      assertThat(result.getReportStatus()).isEqualTo(NOT_STARTED);
    }

    public static Stream<
      Set<ApiTestResult>
    > shouldReturnInProgressWhenAnyTestResultsHaveNotArrivedYet() {
      return Stream.of(emptySet(), null);
    }

    @MethodSource
    @ParameterizedTest
    void shouldReturnInProgressWhenAnyTestResultsHaveNotArrivedYet(
      Set<ApiTestResult> apiTestResults
    ) {
      QualityGateReport report = new QualityGateReport().withApiTests(
        Set.of(
          new ApiTest().withApiTestResults(apiTestResults),
          new ApiTest().withApiTestResults(Set.of(new ApiTestResult()))
        )
      );

      QualityGateReport result = fixture.withUpdatedReportStatus(report);

      assertThat(result.getReportStatus()).isEqualTo(IN_PROGRESS);
    }

    @Test
    void shouldReturnFailedWhenAnyTestFailedAndNoneInProgress() {
      QualityGateReport report = new QualityGateReport().withApiTests(
        Set.of(
          new ApiTest().withApiTestResults(
            Set.of(new ApiTestResult().withCoverage(ONE))
          ),
          new ApiTest().withApiTestResults(
            Set.of(new ApiTestResult().withCoverage(ZERO))
          )
        )
      );

      QualityGateReport result = fixture.withUpdatedReportStatus(report);

      assertThat(result.getReportStatus()).isEqualTo(FAILED);
    }

    @Test
    void shouldReturnPassedWhenAllTestsPassedAndNoneInProgress() {
      QualityGateReport report = new QualityGateReport().withApiTests(
        Set.of(
          new ApiTest().withApiTestResults(
            Set.of(new ApiTestResult().withCoverage(ONE))
          ),
          new ApiTest().withApiTestResults(
            Set.of(new ApiTestResult().withCoverage(ONE))
          )
        )
      );

      QualityGateReport result = fixture.withUpdatedReportStatus(report);

      assertThat(result.getReportStatus()).isEqualTo(PASSED);
    }

    @Test
    void shouldReturnNotStartedWhenNoConditionsMatch() {
      QualityGateReport report = new QualityGateReport().withApiTests(
        Set.of(new ApiTest())
      );

      QualityGateReport result = fixture.withUpdatedReportStatus(report);

      assertThat(result.getReportStatus()).isEqualTo(NOT_STARTED);
    }
  }
}
