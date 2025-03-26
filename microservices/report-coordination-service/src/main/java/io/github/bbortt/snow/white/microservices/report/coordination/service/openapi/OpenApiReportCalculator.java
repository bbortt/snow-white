package io.github.bbortt.snow.white.microservices.report.coordination.service.openapi;

import static java.math.BigDecimal.ONE;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.OpenApiCoverage;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.OpenApiCoverageConfig;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OpenApiReportCalculator {

  private final OpenApiCoverageResponseEvent openApiCoverageResponseEvent;
  private final OpenApiCoverageConfig openApiCoverageConfig;

  public OpenApiCoverage calculateReport() {
    return OpenApiCoverage.builder()
      .pathCoverage(openApiCoverageResponseEvent.getPathCoverage())
      .pathCoverageMet(
        !openApiCoverageConfig.getIncludesPathCoverage() ||
        ONE.equals(openApiCoverageResponseEvent.getPathCoverage())
      )
      .responseCodeCoverage(
        openApiCoverageResponseEvent.getResponseCodeCoverage()
      )
      .responseCodeCoverageMet(
        !openApiCoverageConfig.getIncludesResponseCodeCoverage() ||
        ONE.equals(openApiCoverageResponseEvent.getResponseCodeCoverage())
      )
      .errorResponseCoverage(
        openApiCoverageResponseEvent.getErrorResponseCoverage()
      )
      .errorResponseCoverageMet(
        !openApiCoverageConfig.getIncludesErrorResponseCoverage() ||
        ONE.equals(openApiCoverageResponseEvent.getErrorResponseCoverage())
      )
      .requiredParameterCoverage(
        openApiCoverageResponseEvent.getRequiredParameterCoverage()
      )
      .requiredParameterCoverageMet(
        !openApiCoverageConfig.getIncludesRequiredParameterCoverage() ||
        ONE.equals(openApiCoverageResponseEvent.getRequiredParameterCoverage())
      )
      .headerParameterCoverage(
        openApiCoverageResponseEvent.getHeaderParameterCoverage()
      )
      .headerParameterCoverageMet(
        !openApiCoverageConfig.getIncludesHeaderParameterCoverage() ||
        ONE.equals(openApiCoverageResponseEvent.getHeaderParameterCoverage())
      )
      .queryParameterCoverage(
        openApiCoverageResponseEvent.getQueryParameterCoverage()
      )
      .queryParameterCoverageMet(
        !openApiCoverageConfig.getIncludesQueryParameterCoverage() ||
        ONE.equals(openApiCoverageResponseEvent.getQueryParameterCoverage())
      )
      .requestBodySchemaCoverage(
        openApiCoverageResponseEvent.getRequestBodySchemaCoverage()
      )
      .requestBodySchemaCoverageMet(
        !openApiCoverageConfig.getIncludesRequestBodySchemaCoverage() ||
        ONE.equals(openApiCoverageResponseEvent.getRequestBodySchemaCoverage())
      )
      .contentTypeCoverage(
        openApiCoverageResponseEvent.getContentTypeCoverage()
      )
      .contentTypeCoverageMet(
        !openApiCoverageConfig.getIncludesContentTypeCoverage() ||
        ONE.equals(openApiCoverageResponseEvent.getContentTypeCoverage())
      )
      .build();
  }
}
