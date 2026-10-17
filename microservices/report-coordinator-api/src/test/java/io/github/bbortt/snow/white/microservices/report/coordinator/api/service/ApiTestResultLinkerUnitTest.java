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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import clew.traceables.clew.annotation.VerifiesCon;
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

    @ParameterizedTest
    @MethodSource("nullOrEmptyList")
    @VerifiesSw(SwTraceables.SW_016_API_TEST_VERDICT_IS_GATE_SCOPED)
    void shouldSetApiTestStatusToPassed_whenNoResultIsIncluded(
      Set<String> includedOpenApiCoverageCriteria
    ) {
      var apiTest = ApiTest.builder().apiType(OPENAPI.getVal()).build();

      // The one result present would fail on its own, so PASSED here can only come from the
      // included set being empty -- a gate that selected nothing this API measured has nothing
      // to object to, and must not fail on absence of evidence.
      fixture.addApiTestResultsToApiTest(
        Set.of(
          ApiTestResult.builder()
            .apiTestCriteria(PATH_COVERAGE.name())
            .coverage(ZERO)
            .includedInReport(FALSE)
            .duration(Duration.ofSeconds(1))
            .apiTest(mock(ApiTest.class))
            .build()
        ),
        apiTest,
        includedOpenApiCoverageCriteria,
        80
      );

      assertThat(apiTest.getReportStatus()).isEqualTo(PASSED);
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
    void shouldSetApiTestStatusToFailed_whenOneIncludedResultMissesTheBar_howeverManyOthersPass() {
      var apiTest = ApiTest.builder().apiType(OPENAPI.getVal()).build();

      // 4 included results: 3 at full coverage, 1 at zero. The bar is a floor under each one,
      // so the single zero fails the API test no matter where the bar sits -- including at 75,
      // where three of four clearing it used to be a passing share.
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
      assertThat(apiTest2.getReportStatus()).isEqualTo(FAILED);

      // The same set passes once the gate stops selecting the criterion that missed: the floor
      // applies to the included set, and nothing outside it votes either way.
      var apiTest3 = ApiTest.builder().apiType(OPENAPI.getVal()).build();
      fixture.addApiTestResultsToApiTest(
        apiTestResults,
        apiTest3,
        Set.of("CRITERIA_1", "CRITERIA_2", "CRITERIA_3"),
        80
      );
      assertThat(apiTest3.getReportStatus()).isEqualTo(PASSED);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_016_API_TEST_VERDICT_IS_GATE_SCOPED)
    void shouldSetApiTestStatusToFailed_whenIncludedResultIsJustBelowTheBar() {
      var apiTest = ApiTest.builder().apiType(OPENAPI.getVal()).build();

      // The per-criterion comparison is exact and is now the only one: 0.79 against a bar of 80
      // fails, where 0.80 exactly would pass. Nothing rounds either side of it.
      Set<ApiTestResult> apiTestResults = Set.of(
        ApiTestResult.builder()
          .apiTestCriteria(PATH_COVERAGE.name())
          .coverage(new BigDecimal("0.79"))
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

      assertThat(apiTest.getReportStatus()).isEqualTo(FAILED);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_016_API_TEST_VERDICT_IS_GATE_SCOPED)
    void shouldSetApiTestStatusToPassed_whenIncludedResultSitsExactlyOnTheBar() {
      var apiTest = ApiTest.builder().apiType(OPENAPI.getVal()).build();

      Set<ApiTestResult> apiTestResults = Set.of(
        ApiTestResult.builder()
          .apiTestCriteria(PATH_COVERAGE.name())
          .coverage(new BigDecimal("0.80"))
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

    /**
     * A criterion emptied by waivers clears the bar, because the bar is a floor under each
     * criterion's own ratio and an empty judgement has nothing below it. What it must not do is
     * speak for its neighbours: the verdict beside it is the one its own coverage implies, and the
     * counts are what tell a reader which of the two rested on a waiver.
     */
    @Test
    @VerifiesCon(
      ConTraceables.CON_012_CRITERION_EMPTIED_BY_WAIVERS_IS_NEVER_A_COVERAGE_PASS
    )
    void shouldSetApiTestStatusToPassed_whenAFullyWaivedResultSitsBesideACoveredOne() {
      var apiTest = ApiTest.builder().apiType(OPENAPI.getVal()).build();

      Set<ApiTestResult> apiTestResults = Set.of(
        ApiTestResult.builder()
          .apiTestCriteria(PATH_COVERAGE.name())
          .coverage(ONE)
          .judgedTargetCount(10)
          .waivedTargetCount(0)
          .includedInReport(FALSE)
          .duration(Duration.ofSeconds(1))
          .apiTest(mock(ApiTest.class))
          .build(),
        ApiTestResult.builder()
          .apiTestCriteria("FULLY_WAIVED_CRITERIA")
          .coverage(ONE)
          .judgedTargetCount(0)
          .waivedTargetCount(3)
          .includedInReport(FALSE)
          .duration(Duration.ofSeconds(1))
          .apiTest(mock(ApiTest.class))
          .build()
      );

      fixture.addApiTestResultsToApiTest(
        apiTestResults,
        apiTest,
        Set.of(PATH_COVERAGE.name(), "FULLY_WAIVED_CRITERIA"),
        100
      );

      assertThat(apiTest.getReportStatus()).isEqualTo(PASSED);

      // The same 1.00 twice, and the counts are the only thing that separates the criterion that
      // earned it from the one that was excused it.
      assertThat(apiTest.getApiTestResults())
        .extracting(
          ApiTestResult::getApiTestCriteria,
          ApiTestResult::getCoverage,
          ApiTestResult::getJudgedTargetCount,
          ApiTestResult::getWaivedTargetCount
        )
        .containsExactlyInAnyOrder(
          tuple(PATH_COVERAGE.name(), ONE, 10, 0),
          tuple("FULLY_WAIVED_CRITERIA", ONE, 0, 3)
        );
    }

    @Test
    @VerifiesCon(
      ConTraceables.CON_012_CRITERION_EMPTIED_BY_WAIVERS_IS_NEVER_A_COVERAGE_PASS
    )
    void shouldSetApiTestStatusToFailed_whenAGenuineFailureSitsBesideAFullyWaivedResult() {
      var apiTest = ApiTest.builder().apiType(OPENAPI.getVal()).build();

      Set<ApiTestResult> apiTestResults = Set.of(
        ApiTestResult.builder()
          .apiTestCriteria("FULLY_WAIVED_CRITERIA")
          .coverage(ONE)
          .judgedTargetCount(0)
          .waivedTargetCount(3)
          .includedInReport(FALSE)
          .duration(Duration.ofSeconds(1))
          .apiTest(mock(ApiTest.class))
          .build(),
        ApiTestResult.builder()
          .apiTestCriteria(PATH_COVERAGE.name())
          .coverage(ZERO)
          .judgedTargetCount(4)
          .waivedTargetCount(0)
          .includedInReport(FALSE)
          .duration(Duration.ofSeconds(1))
          .apiTest(mock(ApiTest.class))
          .build()
      );

      fixture.addApiTestResultsToApiTest(
        apiTestResults,
        apiTest,
        Set.of(PATH_COVERAGE.name(), "FULLY_WAIVED_CRITERIA"),
        100
      );

      // A waiver rescues nothing it was not written against: the failing criterion still fails the
      // API test with a fully-waived one beside it.
      assertThat(apiTest.getReportStatus()).isEqualTo(FAILED);
    }

    /**
     * And where every included criterion was emptied, the API test passes with nothing judged -
     * which is a true statement about a gate that had nothing left to object to, and a different one
     * from passing on coverage. The counts on every result are what say which happened.
     */
    @Test
    @VerifiesCon(
      ConTraceables.CON_012_CRITERION_EMPTIED_BY_WAIVERS_IS_NEVER_A_COVERAGE_PASS
    )
    void shouldSetApiTestStatusToPassed_whenEveryIncludedResultIsFullyWaived() {
      var apiTest = ApiTest.builder().apiType(OPENAPI.getVal()).build();

      Set<ApiTestResult> apiTestResults = Set.of(
        ApiTestResult.builder()
          .apiTestCriteria(PATH_COVERAGE.name())
          .coverage(ONE)
          .judgedTargetCount(0)
          .waivedTargetCount(2)
          .includedInReport(FALSE)
          .duration(Duration.ofSeconds(1))
          .apiTest(mock(ApiTest.class))
          .build(),
        ApiTestResult.builder()
          .apiTestCriteria("OTHER_CRITERIA")
          .coverage(ONE)
          .judgedTargetCount(0)
          .waivedTargetCount(5)
          .includedInReport(FALSE)
          .duration(Duration.ofSeconds(1))
          .apiTest(mock(ApiTest.class))
          .build()
      );

      fixture.addApiTestResultsToApiTest(
        apiTestResults,
        apiTest,
        Set.of(PATH_COVERAGE.name(), "OTHER_CRITERIA"),
        100
      );

      assertThat(apiTest.getReportStatus()).isEqualTo(PASSED);

      assertThat(apiTest.getApiTestResults()).allSatisfy(result -> {
        assertThat(result.getJudgedTargetCount()).isZero();
        assertThat(result.getWaivedTargetCount()).isPositive();
      });
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
