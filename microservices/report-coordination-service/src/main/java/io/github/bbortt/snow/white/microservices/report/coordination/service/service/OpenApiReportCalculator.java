/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.service;

import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.PASSED;
import static java.math.BigDecimal.ONE;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class OpenApiReportCalculator {

  private final QualityGateReport qualityGateReport;
  private final @Nullable List<String> includedOpenApiCriteria;
  private final Set<OpenApiTestResult> openApiTestCriteria;

  public CalculationResult calculate() {
    var updatedOpenApiCriteria = updateQualityGateReportInformation();

    var reportStatus = new AtomicReference<>(PASSED);
    if (nonNull(includedOpenApiCriteria)) {
      assertThatEachOpenApiCriterionIsCovered(
        updatedOpenApiCriteria,
        reportStatus
      );
    }

    logger.debug(
      "Calculated OpenAPI coverage ({}): {}",
      reportStatus.get(),
      updatedOpenApiCriteria
    );

    return new CalculationResult(reportStatus.get(), updatedOpenApiCriteria);
  }

  private void assertThatEachOpenApiCriterionIsCovered(
    Set<OpenApiTestResult> updatedOpenApiCriteria,
    AtomicReference<ReportStatus> reportStatus
  ) {
    requireNonNull(includedOpenApiCriteria).forEach(criterionName -> {
      boolean criterionFound = false;

      for (OpenApiTestResult result : updatedOpenApiCriteria) {
        if (result.getOpenApiTestCriteria().getName().equals(criterionName)) {
          criterionFound = true;

          if (ONE.compareTo(result.getCoverage()) != 0) {
            logger.trace(
              "Criterion {} has insufficient coverage: {}",
              criterionName,
              result.getCoverage()
            );

            reportStatus.set(FAILED);
          }

          break;
        }
      }

      if (!criterionFound) {
        logger.trace("Required criterion is missing: {}", criterionName);
        reportStatus.set(FAILED);
      }
    });
  }

  private Set<OpenApiTestResult> updateQualityGateReportInformation() {
    return openApiTestCriteria
      .parallelStream()
      .map(openApiCriterionResult ->
        openApiCriterionResult
          .withIncludedInReport(
            nonNull(includedOpenApiCriteria) &&
            includedOpenApiCriteria.contains(
              openApiCriterionResult.getOpenApiTestCriteria().getName()
            )
          )
          .withQualityGateReport(qualityGateReport)
      )
      .collect(toSet());
  }

  public record CalculationResult(
    ReportStatus status,
    Set<OpenApiTestResult> openApiTestResults
  ) {}
}
