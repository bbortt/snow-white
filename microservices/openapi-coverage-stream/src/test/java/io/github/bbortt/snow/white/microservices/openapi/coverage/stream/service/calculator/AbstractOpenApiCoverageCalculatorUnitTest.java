/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus.NOT_APPLICABLE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus.UNCOVERED;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.ApiTestFinding;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingEvidence;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
