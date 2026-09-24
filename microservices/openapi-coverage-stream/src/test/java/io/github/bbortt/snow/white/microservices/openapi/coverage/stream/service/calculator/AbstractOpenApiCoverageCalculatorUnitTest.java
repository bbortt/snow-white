/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.NOT_APPLICABLE;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.UNCOVERED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.PATH_COVERAGE;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Collections.nCopies;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import clew.traceables.clew.annotation.VerifiesCon;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding;
import io.github.bbortt.snow.white.commons.event.dto.FindingEvidence;
import io.github.bbortt.snow.white.commons.event.dto.FindingStatus;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AbstractOpenApiCoverageCalculatorUnitTest {

  private static final Map<String, Operation> NO_OPERATIONS = emptyMap();
  private static final Map<String, List<OpenTelemetryData>> NO_TELEMETRY =
    emptyMap();

  private static BigDecimal deriveCoverageOf(FindingStatus... statuses) {
    return deriveCoverageOf(stream(statuses).map(Fixture::finding).toList());
  }

  private static BigDecimal deriveCoverageOf(List<ApiTestFinding> findings) {
    return new Fixture(findings)
      .calculate(NO_OPERATIONS, NO_TELEMETRY)
      .coverage();
  }

  private static BigDecimal ratioOf(double value) {
    return BigDecimal.valueOf(value).setScale(2, HALF_UP);
  }

  @Nested
  class CalculateTest {

    @Test
    @VerifiesArch(ArchTraceables.ARCH_010_COVERAGE_DERIVED_FROM_FINDINGS)
    void shouldDeriveRatioAsCoveredOverJudgedFindings() {
      var result = deriveCoverageOf(COVERED, COVERED, COVERED, UNCOVERED);

      assertThat(result).isEqualTo(ratioOf(0.75));
    }

    @Test
    @VerifiesSw(SwTraceables.SW_030_UNJUDGED_TARGET_IS_NOT_APPLICABLE)
    void shouldLeaveNotApplicableFindingsOutOfBothSidesOfTheFraction() {
      var withInapplicable = deriveCoverageOf(
        COVERED,
        NOT_APPLICABLE,
        UNCOVERED,
        NOT_APPLICABLE
      );

      assertThat(withInapplicable)
        .isEqualTo(ratioOf(0.5))
        .isEqualTo(deriveCoverageOf(COVERED, UNCOVERED));
    }

    @Test
    void shouldReportVacuousTruthWhenEveryFindingIsNotApplicable() {
      var result = deriveCoverageOf(NOT_APPLICABLE, NOT_APPLICABLE);

      assertThat(result).isEqualTo(ratioOf(1.0));
    }

    @Test
    void shouldReportVacuousTruthWhenThereIsNoTargetAtAll() {
      var result = deriveCoverageOf();

      assertThat(result).isEqualTo(ratioOf(1.0));
    }

    @Test
    void shouldReportZeroWhenNoJudgedFindingIsCovered() {
      var result = deriveCoverageOf(UNCOVERED, UNCOVERED, NOT_APPLICABLE);

      assertThat(result).isEqualTo(ratioOf(0.0));
    }

    @Test
    void shouldClampANearCompleteRatioBelowOne() {
      var findings = IntStream.range(0, 1000)
        .mapToObj(index -> Fixture.finding(index == 0 ? UNCOVERED : COVERED))
        .toList();

      var result = deriveCoverageOf(findings);

      assertThat(result).isEqualTo(ratioOf(0.99));
    }

    @Test
    void shouldRoundHalfUpToTwoDecimals() {
      var result = deriveCoverageOf(COVERED, COVERED, UNCOVERED);

      assertThat(result).isEqualTo(ratioOf(0.67));
    }

    @Test
    void shouldReportTheSupportedCriterionAndAMeasuredDuration() {
      var result = new Fixture(List.of(Fixture.finding(COVERED))).calculate(
        NO_OPERATIONS,
        NO_TELEMETRY
      );

      assertThat(result.openApiCriteria()).isEqualTo(PATH_COVERAGE);
      assertThat(result.duration()).isNotNull();
    }

    @Test
    @VerifiesArch(ArchTraceables.ARCH_010_COVERAGE_DERIVED_FROM_FINDINGS)
    void shouldHandTheWholeCalculationToTheMessageHook() {
      var findings = List.of(
        Fixture.finding(COVERED),
        Fixture.finding(NOT_APPLICABLE)
      );
      var fixture = new Fixture(findings);

      fixture.calculate(NO_OPERATIONS, NO_TELEMETRY);

      assertThat(fixture.lastCalculation).isEqualTo(
        new AbstractOpenApiCoverageCalculator.Calculation(
          NO_OPERATIONS,
          NO_TELEMETRY,
          findings
        )
      );
    }

    @Test
    void shouldDelegateAdditionalInformationToTheCalculator() {
      var result = new Fixture(List.of(Fixture.finding(UNCOVERED))).calculate(
        NO_OPERATIONS,
        NO_TELEMETRY
      );

      assertThat(result.additionalInformation()).isEqualTo("1 finding");
    }
  }

  @Nested
  class CoverageAgreesWithItsFindingsTest {

    /**
     * Every combination of covered, uncovered and inapplicable counts up to a handful each, plus
     * the two boundaries the ratio rules single out: nothing judged at all, and a ratio close
     * enough to complete that it has to be clamped.
     */
    static Stream<Arguments> findingCounts() {
      var combinations = Stream.<Arguments>builder();

      for (var covered = 0; covered <= 4; covered++) {
        for (var uncovered = 0; uncovered <= 4; uncovered++) {
          for (var notApplicable = 0; notApplicable <= 2; notApplicable++) {
            combinations.add(arguments(covered, uncovered, notApplicable));
          }
        }
      }

      combinations.add(arguments(999, 1, 0));
      combinations.add(arguments(0, 0, 3));

      return combinations.build();
    }

    @MethodSource("findingCounts")
    @ParameterizedTest
    @VerifiesCon(ConTraceables.CON_009_COVERAGE_AGREES_WITH_FINDINGS)
    void shouldEqualTheRatioItsOwnFindingsImply(
      int covered,
      int uncovered,
      int notApplicable
    ) {
      var findings = Stream.of(
        nCopies(covered, COVERED),
        nCopies(uncovered, UNCOVERED),
        nCopies(notApplicable, NOT_APPLICABLE)
      )
        .flatMap(List::stream)
        .map(Fixture::finding)
        .toList();

      var result = new Fixture(findings).calculate(NO_OPERATIONS, NO_TELEMETRY);

      assertThat(result.coverage()).isEqualTo(expectedRatio(result.findings()));
    }

    @Test
    @VerifiesCon(ConTraceables.CON_009_COVERAGE_AGREES_WITH_FINDINGS)
    void shouldNotFlagAResultThatCarriesNoFindingsAtAll() {
      // A result written before the findings migration keeps the ratio it was calculated with and
      // an empty collection beside it. The exemption is what this asserts: the pair is absent, so
      // there is nothing for the ratio to disagree with.
      var preMigrationResult = new OpenApiTestResult(
        PATH_COVERAGE,
        ratioOf(0.42),
        Duration.ofSeconds(1)
      );

      assertThat(preMigrationResult.findings()).isEmpty();
      assertThat(expectedRatio(preMigrationResult.findings())).isEqualTo(
        ratioOf(1.0)
      );
      assertThat(preMigrationResult.coverage())
        .isEqualTo(ratioOf(0.42))
        .isNotEqualTo(expectedRatio(preMigrationResult.findings()));
    }

    /**
     * The ratio rules spelled out again, independently of the implementation under test: covered
     * over covered plus uncovered, inapplicable outside both sides, two decimals half up, clamped
     * below one unless every judged target is covered, and one when nothing was judged.
     */
    private static BigDecimal expectedRatio(List<ApiTestFinding> findings) {
      var covered = findings
        .stream()
        .filter(finding -> COVERED.equals(finding.status()))
        .count();
      var judged = findings
        .stream()
        .filter(finding -> !NOT_APPLICABLE.equals(finding.status()))
        .count();

      if (judged == 0) {
        return ratioOf(1.0);
      }

      var ratio = BigDecimal.valueOf(covered).divide(
        BigDecimal.valueOf(judged),
        2,
        HALF_UP
      );

      return covered != judged && ratio.compareTo(BigDecimal.ONE) == 0
        ? new BigDecimal("0.99")
        : ratio;
    }
  }

  private static final class Fixture extends AbstractOpenApiCoverageCalculator {

    private final List<ApiTestFinding> findings;

    private @Nullable Calculation lastCalculation;

    private Fixture(List<ApiTestFinding> findings) {
      this.findings = findings;
    }

    private static ApiTestFinding finding(FindingStatus status) {
      return ApiTestFinding.builder()
        .specPointer("/paths/~1api")
        .status(status)
        .evidence(
          COVERED.equals(status)
            ? List.of(new FindingEvidence("traceId", null))
            : List.of()
        )
        .build();
    }

    @Override
    protected @NonNull OpenApiCoverageCriteria getSupportedOpenApiCoverageCriteria() {
      return PATH_COVERAGE;
    }

    @Override
    protected @NonNull List<ApiTestFinding> calculateFindings(
      Map<String, Operation> pathToOpenAPIOperationMap,
      Map<String, List<OpenTelemetryData>> pathToTelemetryMap
    ) {
      return findings;
    }

    @Override
    protected @Nullable String getAdditionalInformationOrNull(
      @NonNull Calculation calculation
    ) {
      this.lastCalculation = calculation;
      return calculation.findings().size() + " finding";
    }
  }
}
