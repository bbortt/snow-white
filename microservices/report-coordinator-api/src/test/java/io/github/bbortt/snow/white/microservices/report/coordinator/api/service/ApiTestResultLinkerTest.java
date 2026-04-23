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
        emptySet()
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
        includedOpenApiCriteria
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

      Set<String> includedOpenApiCriteria = Set.of(PATH_COVERAGE.name());

      fixture.addApiTestResultsToApiTest(
        apiTestResults,
        apiTest,
        includedOpenApiCriteria
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

      Set<String> includedOpenApiCriteria = Set.of(PATH_COVERAGE.name());

      fixture.addApiTestResultsToApiTest(
        apiTestResults,
        apiTest,
        includedOpenApiCriteria
      );

      assertThat(apiTest.getReportStatus()).isEqualTo(FAILED);
      verify(apiTestRepositoryMock).save(apiTest);
    }
  }
}
