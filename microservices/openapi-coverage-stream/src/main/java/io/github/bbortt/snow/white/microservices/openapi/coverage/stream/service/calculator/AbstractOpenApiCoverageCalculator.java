/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.NOT_APPLICABLE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.CalculatorUtils.getStartedStopWatch;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.MathUtils.calculatePercentage;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import clew.traceables.clew.annotation.RealizesCon;
import io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenApiCoverageCalculator;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Base class of every coverage calculator.
 * A calculator states what it found per target; it never states a ratio. The ratio is derived here,
 * in one place, from the findings the subclass returned.
 */
@RealizesArch(ArchTraceables.ARCH_010_COVERAGE_DERIVED_FROM_FINDINGS)
abstract class AbstractOpenApiCoverageCalculator
  implements OpenApiCoverageCalculator
{

  @Override
  public boolean accepts(OpenApiCoverageCriteria openApiCriteria) {
    return getSupportedOpenApiCoverageCriteria().equals(openApiCriteria);
  }

  @Override
  public final OpenApiTestResult calculate(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var stopWatch = getStartedStopWatch();

    var findings = calculateFindings(
      pathToOpenAPIOperationMap,
      pathToTelemetryMap
    );

    return new OpenApiTestResult(
      getSupportedOpenApiCoverageCriteria(),
      deriveCoverage(findings),
      stopWatch.getDuration(),
      getAdditionalInformationOrNull(
        new Calculation(pathToOpenAPIOperationMap, pathToTelemetryMap, findings)
      ),
      findings
    );
  }

  protected abstract @NonNull OpenApiCoverageCriteria getSupportedOpenApiCoverageCriteria();

  /**
   * One finding per target the criterion looked at, including the targets it does not judge.
   */
  protected abstract @NonNull List<ApiTestFinding> calculateFindings(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  );

  /**
   * The free-text message that accompanies the ratio, which most criteria render from their
   * findings alone.
   * It is handed the whole calculation rather than only the findings because two messages describe
   * the input as well as the verdicts, and the findings cannot answer either:
   * {@code CONTENT_TYPE_COVERAGE} distinguishes an untested media type from header capture never
   * having been switched on, and {@code REQUIRED_ERROR_FIELDS_COVERAGE} names the required fields
   * of the schema behind an uncovered entry.
   */
  protected abstract @Nullable String getAdditionalInformationOrNull(
    @NonNull Calculation calculation
  );

  /**
   * One calculator's run: what it was asked about, and what it concluded.
   */
  protected record Calculation(
    @NonNull Map<String, Operation> pathToOpenAPIOperationMap,
    @NonNull Map<String, List<OpenTelemetryData>> pathToTelemetryMap,
    @NonNull List<ApiTestFinding> findings
  ) {}

  /**
   * The covered share of the targets the criterion judged. A {@code NOT_APPLICABLE} finding enters
   * neither side of the fraction.
   * This is the only place a coverage ratio is produced, which is what keeps the stored ratio and
   * the findings beside it from ever disagreeing: there is no second implementation to drift.
   */
  @RealizesArch(ArchTraceables.ARCH_010_COVERAGE_DERIVED_FROM_FINDINGS)
  @RealizesCon(ConTraceables.CON_009_COVERAGE_AGREES_WITH_FINDINGS)
  private static BigDecimal deriveCoverage(
    @NonNull List<ApiTestFinding> findings
  ) {
    var covered = 0;
    var judged = 0;

    for (ApiTestFinding finding : findings) {
      if (NOT_APPLICABLE.equals(finding.status())) {
        continue;
      }

      judged++;

      if (COVERED.equals(finding.status())) {
        covered++;
      }
    }

    return calculatePercentage(covered, judged);
  }
}
