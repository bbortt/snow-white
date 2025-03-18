package io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.converter;

import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.OpenApiCoverage;
import io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.QualityGateConfig;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public final class QualityGateConfigurationConverter
  implements Converter<QualityGateConfiguration, QualityGateConfig> {

  @Override
  public QualityGateConfig convert(
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
}
