/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.service;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.HTTP_METHOD_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.PASSED;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.SET;

import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiReportCalculatorTest {

  @Mock
  private QualityGateReport qualityGateReportMock;

  @Nested
  class Calculate {

    public static Stream<List<String>> nullOrEmptyList() {
      return Stream.of(null, emptyList());
    }

    @ParameterizedTest
    @MethodSource("nullOrEmptyList")
    void shouldReturn_statusPassed_whenNoCriteriaAreBeingIncluded(
      List<String> includedOpenApiCriteria
    ) {
      Set<OpenApiTestResult> openApiTestCriteria = Set.of(
        OpenApiTestResult.builder()
          .openApiTestCriteria(PATH_COVERAGE.name())
          .coverage(ONE)
          .build()
      );

      var fixture = new OpenApiReportCalculator(
        qualityGateReportMock,
        includedOpenApiCriteria,
        openApiTestCriteria
      );

      var calculationResult = fixture.calculate();

      assertThat(calculationResult).satisfies(
        r -> assertThat(r.status()).isEqualTo(PASSED),
        r ->
          assertThat(r.openApiTestResults())
            .isNotEmpty()
            .allSatisfy(criterionResult ->
              assertThat(criterionResult.getIncludedInReport()).isFalse()
            )
            .allSatisfy(criterionResult ->
              assertThat(criterionResult.getQualityGateReport()).isEqualTo(
                qualityGateReportMock
              )
            )
      );
    }

    @Test
    void shouldReturn_statusPassed_whenAllIncludedCriteriaAreCovered() {
      var pathCoverage = PATH_COVERAGE.name();

      List<String> includedOpenApiCriteria = singletonList(pathCoverage);
      Set<OpenApiTestResult> openApiTestCriteria = Set.of(
        OpenApiTestResult.builder()
          .openApiTestCriteria(pathCoverage)
          .coverage(ONE)
          .build(),
        OpenApiTestResult.builder()
          .openApiTestCriteria(HTTP_METHOD_COVERAGE.name())
          .coverage(ZERO)
          .build()
      );

      var fixture = new OpenApiReportCalculator(
        qualityGateReportMock,
        includedOpenApiCriteria,
        openApiTestCriteria
      );

      var calculationResult = fixture.calculate();

      assertThatCalculationResultHasStatus(calculationResult, PASSED);

      assertThat(calculationResult)
        .extracting(
          OpenApiReportCalculator.CalculationResult::openApiTestResults
        )
        .asInstanceOf(SET)
        .satisfiesOnlyOnce(criterionResult ->
          assertThat(
            ((OpenApiTestResult) criterionResult).getIncludedInReport()
          ).isTrue()
        );
    }

    @Test
    void shouldReturn_statusFailed_whenIncludedCriteriaHasInsufficientCoverage() {
      var pathCoverage = PATH_COVERAGE.name();

      List<String> includedOpenApiCriteria = singletonList(pathCoverage);
      Set<OpenApiTestResult> openApiTestCriteria = Set.of(
        OpenApiTestResult.builder()
          .openApiTestCriteria(pathCoverage)
          .coverage(BigDecimal.valueOf(0.99))
          .build()
      );

      var fixture = new OpenApiReportCalculator(
        qualityGateReportMock,
        includedOpenApiCriteria,
        openApiTestCriteria
      );

      var calculationResult = fixture.calculate();

      assertThatCalculationResultHasStatus(calculationResult, FAILED);
    }

    @Test
    void shouldReturn_statusFailed_whenAnIncludedCriterionIsNotPresent() {
      var pathCoverage = PATH_COVERAGE.name();
      var httpMethodCoverage = HTTP_METHOD_COVERAGE.name();

      // Include both criteria in the required list
      List<String> includedOpenApiCriteria = List.of(
        pathCoverage,
        httpMethodCoverage
      );

      // But only provide one in the results
      Set<OpenApiTestResult> openApiTestCriteria = Set.of(
        OpenApiTestResult.builder()
          .openApiTestCriteria(pathCoverage)
          .coverage(ONE)
          .build()
      );

      var fixture = new OpenApiReportCalculator(
        qualityGateReportMock,
        includedOpenApiCriteria,
        openApiTestCriteria
      );

      var calculationResult = fixture.calculate();

      assertThat(calculationResult).satisfies(
        r -> assertThat(r.status()).isEqualTo(FAILED),
        r ->
          assertThat(r.openApiTestResults())
            .isNotEmpty()
            .satisfies(criteriaResults ->
              assertThat(criteriaResults)
                .map(OpenApiTestResult::getOpenApiTestCriteria)
                .containsExactly(pathCoverage)
            )
            .allSatisfy(criterionResult ->
              assertThat(criterionResult.getIncludedInReport()).isTrue()
            )
            .allSatisfy(criterionResult ->
              assertThat(criterionResult.getQualityGateReport()).isEqualTo(
                qualityGateReportMock
              )
            )
      );
    }

    @Test
    void shouldCorrectlySetIncludedInReportFlag() {
      var pathCoverage = PATH_COVERAGE.name();

      // Only include pathCoverage in the included criteria
      List<String> includedOpenApiCriteria = singletonList(pathCoverage);

      // Provide both criteria in the results
      Set<OpenApiTestResult> openApiTestCriteria = Set.of(
        OpenApiTestResult.builder()
          .openApiTestCriteria(pathCoverage)
          .coverage(ONE)
          .build(),
        OpenApiTestResult.builder()
          .openApiTestCriteria(HTTP_METHOD_COVERAGE.name())
          .coverage(ONE)
          .build()
      );

      var fixture = new OpenApiReportCalculator(
        qualityGateReportMock,
        includedOpenApiCriteria,
        openApiTestCriteria
      );

      var calculationResult = fixture.calculate();

      assertThat(calculationResult).satisfies(
        r -> assertThat(r.status()).isEqualTo(PASSED),
        r ->
          assertThat(r.openApiTestResults())
            .isNotEmpty()
            .satisfies(criteriaResults -> {
              criteriaResults.forEach(result -> {
                if (result.getOpenApiTestCriteria().equals(pathCoverage)) {
                  assertThat(result.getIncludedInReport()).isTrue();
                } else {
                  assertThat(result.getIncludedInReport()).isFalse();
                }
                assertThat(result.getQualityGateReport()).isEqualTo(
                  qualityGateReportMock
                );
              });
            })
      );
    }

    private void assertThatCalculationResultHasStatus(
      OpenApiReportCalculator.CalculationResult calculationResult,
      ReportStatus passed
    ) {
      assertThat(calculationResult).satisfies(
        r -> assertThat(r.status()).isEqualTo(passed),
        r ->
          assertThat(r.openApiTestResults())
            .isNotEmpty()
            .satisfiesOnlyOnce(criterionResult ->
              assertThat(criterionResult.getIncludedInReport()).isTrue()
            )
            .allSatisfy(criterionResult ->
              assertThat(criterionResult.getQualityGateReport()).isEqualTo(
                qualityGateReportMock
              )
            )
      );
    }
  }
}
