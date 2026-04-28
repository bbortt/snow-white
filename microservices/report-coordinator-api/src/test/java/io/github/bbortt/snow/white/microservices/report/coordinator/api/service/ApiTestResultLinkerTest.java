/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.PASSED;
import static java.lang.Boolean.FALSE;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.ApiTestRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class ApiTestResultLinkerTest {

  @Mock
  private QualityGateReport qualityGateReportMock;

  @Mock
  private ApiTestRepository apiTestRepositoryMock;

  private ApiTestResultLinker fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiTestResultLinker(apiTestRepositoryMock);
  }

  @Nested
  class AddApiTestResultsToApiTest {

    public static <T> Stream<Set<T>> nullOrEmptyList() {
      return Stream.of(null, emptySet());
    }

    @ParameterizedTest
    @MethodSource("nullOrEmptyList")
    void shouldReturnImmediately_whenThereAreNoApiTestResults(
      Set<ApiTestResult> apiTestResults
    ) {
      var apiTestMock = mock(ApiTest.class);

      fixture.addApiTestResultsToApiTest(
        apiTestResults,
        apiTestMock,
        emptySet(),
        100
      );

      verifyNoInteractions(apiTestMock);
      verify(apiTestRepositoryMock, never()).save(any());
    }

    @ParameterizedTest
    @MethodSource("nullOrEmptyList")
    void shouldReturnApiTestWithLinkedResults_notIncludedInOpenApiCriteria(
      Set<String> includedOpenApiCriteria
    ) {
      var apiTest = ApiTest.builder().apiType(OPENAPI.getVal()).build();

      var apiTestResult = spy(
        ApiTestResult.builder()
          .apiTestCriteria(PATH_COVERAGE.name())
          .coverage(ONE)
          .includedInReport(FALSE)
          .duration(Duration.ofSeconds(1))
          .apiTest(mock(ApiTest.class))
          .build()
      );

      fixture.addApiTestResultsToApiTest(
        Set.of(apiTestResult),
        apiTest,
        includedOpenApiCriteria,
        100
      );

      assertThat(apiTest.getApiTestResults())
        .hasSize(1)
        .first()
        .isEqualTo(apiTestResult);

      verify(apiTestResult).withIncludedInReport(false);
    }

    @Test
    void shouldSetApiTestStatusToPassed_whenAllIncludedResultsFullyCovered() {
      var apiTest = ApiTest.builder().apiType(OPENAPI.getVal()).build();

      Set<ApiTestResult> apiTestResults = Set.of(
        ApiTestResult.builder()
          .apiTestCriteria(PATH_COVERAGE.name())
          .coverage(ONE)
          .includedInReport(FALSE)
          .duration(Duration.ofSeconds(1))
          .apiTest(mock(ApiTest.class))
          .build()
      );

      fixture.addApiTestResultsToApiTest(
        apiTestResults,
        apiTest,
        Set.of(PATH_COVERAGE.name()),
        100
      );

      assertThat(apiTest.getApiTestResults())
        .hasSize(1)
        .allSatisfy(result ->
          assertThat(result.getIncludedInReport()).isTrue()
        );
      assertThat(apiTest.getReportStatus()).isEqualTo(PASSED);
      verify(apiTestRepositoryMock).save(apiTest);
    }

    @Test
    void shouldSetApiTestStatusToFailed_whenAnyIncludedResultIsNotFullyCovered() {
      var apiTest = ApiTest.builder().apiType(OPENAPI.getVal()).build();

      Set<ApiTestResult> apiTestResults = Set.of(
        ApiTestResult.builder()
          .apiTestCriteria(PATH_COVERAGE.name())
          .coverage(ZERO)
          .includedInReport(FALSE)
          .duration(Duration.ofSeconds(1))
          .apiTest(mock(ApiTest.class))
          .build()
      );

      fixture.addApiTestResultsToApiTest(
        apiTestResults,
        apiTest,
        Set.of(PATH_COVERAGE.name()),
        100
      );

      assertThat(apiTest.getReportStatus()).isEqualTo(FAILED);
      verify(apiTestRepositoryMock).save(apiTest);
    }

    @Test
    void shouldSetApiTestStatusToPassed_whenPassRateMeetsMinCoveragePercentage() {
      var apiTest = ApiTest.builder().apiType(OPENAPI.getVal()).build();

      // 4 results: 3 with full coverage (1.0), 1 with zero coverage = 75% pass rate
      // But minCoveragePercentage = 80, so this should FAIL
      Set<ApiTestResult> apiTestResults = Set.of(
        ApiTestResult.builder()
          .apiTestCriteria("CRITERIA_1")
          .coverage(ONE)
          .includedInReport(FALSE)
          .duration(Duration.ofSeconds(1))
          .apiTest(mock(ApiTest.class))
          .build(),
        ApiTestResult.builder()
          .apiTestCriteria("CRITERIA_2")
          .coverage(ONE)
          .includedInReport(FALSE)
          .duration(Duration.ofSeconds(1))
          .apiTest(mock(ApiTest.class))
          .build(),
        ApiTestResult.builder()
          .apiTestCriteria("CRITERIA_3")
          .coverage(ONE)
          .includedInReport(FALSE)
          .duration(Duration.ofSeconds(1))
          .apiTest(mock(ApiTest.class))
          .build(),
        ApiTestResult.builder()
          .apiTestCriteria("CRITERIA_4")
          .coverage(ZERO)
          .includedInReport(FALSE)
          .duration(Duration.ofSeconds(1))
          .apiTest(mock(ApiTest.class))
          .build()
      );

      Set<String> allCriteria = Set.of(
        "CRITERIA_1",
        "CRITERIA_2",
        "CRITERIA_3",
        "CRITERIA_4"
      );

      fixture.addApiTestResultsToApiTest(
        apiTestResults,
        apiTest,
        allCriteria,
        80
      );
      assertThat(apiTest.getReportStatus()).isEqualTo(FAILED);

      var apiTest2 = ApiTest.builder().apiType(OPENAPI.getVal()).build();
      fixture.addApiTestResultsToApiTest(
        apiTestResults,
        apiTest2,
        allCriteria,
        75
      );
      assertThat(apiTest2.getReportStatus()).isEqualTo(PASSED);
    }

    @Test
    void shouldSetApiTestStatusToPassed_whenCoverageThresholdMeetsMinCoveragePercentage() {
      var apiTest = ApiTest.builder().apiType(OPENAPI.getVal()).build();

      // Result with 0.85 coverage, threshold is 80% -> should PASS
      Set<ApiTestResult> apiTestResults = Set.of(
        ApiTestResult.builder()
          .apiTestCriteria(PATH_COVERAGE.name())
          .coverage(new BigDecimal("0.85"))
          .includedInReport(FALSE)
          .duration(Duration.ofSeconds(1))
          .apiTest(mock(ApiTest.class))
          .build()
      );

      fixture.addApiTestResultsToApiTest(
        apiTestResults,
        apiTest,
        Set.of(PATH_COVERAGE.name()),
        80
      );

      assertThat(apiTest.getReportStatus()).isEqualTo(PASSED);
    }
  }
}
