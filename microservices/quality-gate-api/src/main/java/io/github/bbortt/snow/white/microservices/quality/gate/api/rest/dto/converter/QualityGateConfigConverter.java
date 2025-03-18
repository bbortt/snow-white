package io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.converter;

import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.QualityGateConfig;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public final class QualityGateConfigConverter
  implements Converter<QualityGateConfig, QualityGateConfiguration> {

  @Override
  public QualityGateConfiguration convert(QualityGateConfig qualityGateConfig) {
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
