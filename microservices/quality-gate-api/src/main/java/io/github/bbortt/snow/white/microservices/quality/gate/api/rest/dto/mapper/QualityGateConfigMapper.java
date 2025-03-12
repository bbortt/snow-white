package io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.mapper;

import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.OpenApiCoverage;
import io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.QualityGateConfig;

public final class QualityGateConfigMapper {

  private QualityGateConfigMapper() {
    // Static mapper class
  }

  public static QualityGateConfig toDto(
    QualityGateConfiguration qualityGateConfiguration
  ) {
    return QualityGateConfig.builder()
      .name(qualityGateConfiguration.getName())
      .description(qualityGateConfiguration.getDescription())
      .openApiCoverage(
        OpenApiCoverage.builder()
          .pathCoverage(qualityGateConfiguration.getIncludePathCoverage())
          .responseCodeCoverage(
            qualityGateConfiguration.getIncludeResponseCodeCoverage()
          )
          .requiredParameterCoverage(
            qualityGateConfiguration.getIncludeRequiredParameterCoverage()
          )
          .queryParameterCoverage(
            qualityGateConfiguration.getIncludeQueryParameterCoverage()
          )
          .headerParameterCoverage(
            qualityGateConfiguration.getIncludeHeaderParameterCoverage()
          )
          .requestBodySchemaCoverage(
            qualityGateConfiguration.getIncludeRequestBodySchemaCoverage()
          )
          .errorResponseCoverage(
            qualityGateConfiguration.getIncludeErrorResponseCoverage()
          )
          .contentTypeCoverage(
            qualityGateConfiguration.getIncludeContentTypeCoverage()
          )
          .build()
      )
      .build();
  }

  public static QualityGateConfiguration toEntity(
    QualityGateConfig qualityGateConfig
  ) {
    var qualityGateConfigCriteria = qualityGateConfig.getOpenApiCoverage();

    return QualityGateConfiguration.builder()
      .name(qualityGateConfig.getName())
      .description(qualityGateConfig.getDescription())
      .includePathCoverage(qualityGateConfigCriteria.getPathCoverage())
      .includeResponseCodeCoverage(
        qualityGateConfigCriteria.getResponseCodeCoverage()
      )
      .includeRequiredParameterCoverage(
        qualityGateConfigCriteria.getRequiredParameterCoverage()
      )
      .includeQueryParameterCoverage(
        qualityGateConfigCriteria.getQueryParameterCoverage()
      )
      .includeHeaderParameterCoverage(
        qualityGateConfigCriteria.getHeaderParameterCoverage()
      )
      .includeRequestBodySchemaCoverage(
        qualityGateConfigCriteria.getRequestBodySchemaCoverage()
      )
      .includeErrorResponseCoverage(
        qualityGateConfigCriteria.getErrorResponseCoverage()
      )
      .includeContentTypeCoverage(
        qualityGateConfigCriteria.getContentTypeCoverage()
      )
      .build();
  }
}
