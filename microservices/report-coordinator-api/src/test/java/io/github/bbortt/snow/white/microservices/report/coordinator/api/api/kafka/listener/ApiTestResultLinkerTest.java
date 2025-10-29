/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.kafka.listener;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static java.lang.Boolean.FALSE;
import static java.math.BigDecimal.ONE;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
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

  private ApiTestResultLinker fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiTestResultLinker();
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
    }

    @ParameterizedTest
    @MethodSource("nullOrEmptyList")
    void shouldReturnApiTestWithLinkedResults_notIncludedInOpenApiCriteria(
      Set<String> includedOpenApiCriteria
    ) {
      var apiTest = ApiTest.builder().build();

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
    void shouldReturnApiTestWithLinkedResults_includedInOpenApiCriteria() {
      var apiTest = ApiTest.builder().build();

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
    }
  }
}
