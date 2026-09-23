/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.MathUtils.calculatePercentage;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus.NOT_APPLICABLE;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.ApiTestFinding;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Base class of every calculator that has been migrated to the findings contract.
 * It exists beside {@link AbstractOpenApiCoverageCalculator}'s own
 * {@link AbstractOpenApiCoverageCalculator#calculateCoverage} only while the migration runs:
 * once the last criterion extends this class, the two collapse into one.
 */
abstract class AbstractFindingBasedCoverageCalculator
  extends AbstractOpenApiCoverageCalculator
{

  @Override
  protected final @NonNull CoverageCalculationResult calculateCoverage(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var findings = calculateFindings(
      pathToOpenAPIOperationMap,
      pathToTelemetryMap
    );

    return new CoverageCalculationResult(
      deriveCoverage(findings),
      getAdditionalInformationOrNull(findings),
      findings
    );
  }

  protected abstract @NonNull List<ApiTestFinding> calculateFindings(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  );

  protected abstract @Nullable String getAdditionalInformationOrNull(
    @NonNull List<ApiTestFinding> findings
  );

  @RealizesArch(ArchTraceables.ARCH_010_COVERAGE_DERIVED_FROM_FINDINGS)
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
