/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.PASSED;
import static java.lang.Boolean.FALSE;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestFinding;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.ApiTestRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class ApiTestResultLinkerUnitTest {

  @Mock
  private ApiTestRepository apiTestRepositoryMock;

  @InjectMocks
  private ApiTestResultLinker fixture;

  @Nested
  class AddApiTestResultsToApiTest {

    public static <T> Stream<Set<T>> nullOrEmptyList() {
      return Stream.of(null, emptySet());
    }

    @ParameterizedTest
    @MethodSource("nullOrEmptyList")
    @VerifiesSw(SwTraceables.SW_016_API_TEST_VERDICT_IS_GATE_SCOPED)
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
    @VerifiesSw(SwTraceables.SW_016_API_TEST_VERDICT_IS_GATE_SCOPED)
    void shouldReturnApiTestWithLinkedResults_notIncludedInOpenApiCoverageCriteria(
      Set<String> includedOpenApiCoverageCriteria
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
        includedOpenApiCoverageCriteria,
        100
      );

      assertThat(apiTest.getApiTestResults())
        .hasSize(1)
        .first()
        .isEqualTo(apiTestResult);

      verify(apiTestResult).withIncludedInReport(false);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_016_API_TEST_VERDICT_IS_GATE_SCOPED)
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
    @VerifiesSw(SwTraceables.SW_016_API_TEST_VERDICT_IS_GATE_SCOPED)
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
    @VerifiesSw(SwTraceables.SW_016_API_TEST_VERDICT_IS_GATE_SCOPED)
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
    @VerifiesSw(SwTraceables.SW_016_API_TEST_VERDICT_IS_GATE_SCOPED)
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

    @Test
    @VerifiesSw(
      SwTraceables.SW_020_REDELIVERED_CRITERION_RESULT_REPLACES_EXISTING_ONE
    )
    void shouldReplaceExistingResult_whenRedeliveredForSameCriterion() {
      var apiTest = ApiTest.builder().apiType(OPENAPI.getVal()).build();

      var includedCriteria = Set.of(PATH_COVERAGE.name(), "OTHER_CRITERIA");

      fixture.addApiTestResultsToApiTest(
        Set.of(
          ApiTestResult.builder()
            .apiTestCriteria(PATH_COVERAGE.name())
            .coverage(ONE)
            .includedInReport(FALSE)
            .duration(Duration.ofSeconds(1))
            .apiTest(apiTest)
            .build(),
          ApiTestResult.builder()
            .apiTestCriteria("OTHER_CRITERIA")
            .coverage(ONE)
            .includedInReport(FALSE)
            .duration(Duration.ofSeconds(1))
            .apiTest(apiTest)
            .build()
        ),
        apiTest,
        includedCriteria,
        100
      );

      // Nothing was superseded yet, so there is no insert-before-delete collision to flush around.
      verify(apiTestRepositoryMock, never()).saveAndFlush(any());

      var redeliveredDuration = Duration.ofSeconds(5);

      assertThatCode(() ->
        fixture.addApiTestResultsToApiTest(
          Set.of(
            ApiTestResult.builder()
              .apiTestCriteria(PATH_COVERAGE.name())
              .coverage(ZERO)
              .includedInReport(FALSE)
              .duration(redeliveredDuration)
              .apiTest(apiTest)
              .build()
          ),
          apiTest,
          includedCriteria,
          100
        )
      ).doesNotThrowAnyException();

      // The superseded result is gone from the set; its delete has to reach the database before the
      // replacement's insert claims the same composite key.
      verify(apiTestRepositoryMock).saveAndFlush(apiTest);

      assertThat(apiTest.getApiTestResults())
        .hasSize(2)
        .filteredOn(result ->
          PATH_COVERAGE.name().equals(result.getApiTestCriteria())
        )
        .singleElement()
        .satisfies(
          result -> assertThat(result.getCoverage()).isEqualTo(ZERO),
          result ->
            assertThat(result.getDuration()).isEqualTo(redeliveredDuration)
        );

      assertThat(apiTest.getApiTestResults())
        .filteredOn(result ->
          "OTHER_CRITERIA".equals(result.getApiTestCriteria())
        )
        .singleElement()
        .satisfies(result -> assertThat(result.getCoverage()).isEqualTo(ONE));
    }

    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_012_FINDINGS_ON_THE_EVENT_COVERAGE_AS_CACHE
    )
    void shouldPointEveryFindingAtTheResultThatIsLinked() {
      var apiTest = ApiTest.builder().apiType(OPENAPI.getVal()).build();

      var apiTestResult = ApiTestResult.builder()
        .apiTestCriteria(PATH_COVERAGE.name())
        .coverage(ONE)
        .includedInReport(FALSE)
        .duration(Duration.ofSeconds(1))
        .apiTest(apiTest)
        .build();
      apiTestResult
        .getFindings()
        .add(
          ApiTestFinding.builder()
            .specPointer("/paths/~1api")
            .status(COVERED.getVal())
            .apiTestResult(apiTestResult)
            .build()
        );

      fixture.addApiTestResultsToApiTest(
        Set.of(apiTestResult),
        apiTest,
        Set.of(PATH_COVERAGE.name()),
        100
      );

      // Flagging the result included hands back a copy: a finding still pointing at the original
      // would write its foreign key from an instance that never reaches the session.
      assertThat(apiTest.getApiTestResults())
        .singleElement()
        .satisfies(linked ->
          assertThat(linked.getFindings())
            .singleElement()
            .satisfies(finding ->
              assertThat(finding.getApiTestResult()).isSameAs(linked)
            )
        );
    }

    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_012_FINDINGS_ON_THE_EVENT_COVERAGE_AS_CACHE
    )
    @VerifiesSw(
      SwTraceables.SW_020_REDELIVERED_CRITERION_RESULT_REPLACES_EXISTING_ONE
    )
    void shouldReplaceTheFindingsAlongWithTheResultTheyExplain() {
      var apiTest = ApiTest.builder().apiType(OPENAPI.getVal()).build();
      var includedCriteria = Set.of(PATH_COVERAGE.name());

      fixture.addApiTestResultsToApiTest(
        Set.of(resultWithFinding(apiTest, "/paths/~1superseded")),
        apiTest,
        includedCriteria,
        100
      );

      fixture.addApiTestResultsToApiTest(
        Set.of(resultWithFinding(apiTest, "/paths/~1redelivered")),
        apiTest,
        includedCriteria,
        100
      );

      assertThat(apiTest.getApiTestResults())
        .singleElement()
        .satisfies(linked ->
          assertThat(linked.getFindings())
            .singleElement()
            .satisfies(finding ->
              assertThat(finding.getSpecPointer()).isEqualTo(
                "/paths/~1redelivered"
              )
            )
        );
    }

    private static ApiTestResult resultWithFinding(
      ApiTest apiTest,
      String specPointer
    ) {
      var apiTestResult = ApiTestResult.builder()
        .apiTestCriteria(PATH_COVERAGE.name())
        .coverage(ONE)
        .includedInReport(FALSE)
        .duration(Duration.ofSeconds(1))
        .apiTest(apiTest)
        .build();

      apiTestResult
        .getFindings()
        .add(
          ApiTestFinding.builder()
            .specPointer(specPointer)
            .status(COVERED.getVal())
            .apiTestResult(apiTestResult)
            .build()
        );

      return apiTestResult;
    }
  }
}
