package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.QualityGateConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING)
public interface QualityGateConfigurationMapper {
  @Mapping(
    target = "includePathCoverage",
    source = "openApiCoverage.pathCoverage"
  )
  @Mapping(
    target = "includeResponseCodeCoverage",
    source = "openApiCoverage.responseCodeCoverage"
  )
  @Mapping(
    target = "includeRequiredParameterCoverage",
    source = "openApiCoverage.requiredParameterCoverage"
  )
  @Mapping(
    target = "includeQueryParameterCoverage",
    source = "openApiCoverage.queryParameterCoverage"
  )
  @Mapping(
    target = "includeHeaderParameterCoverage",
    source = "openApiCoverage.headerParameterCoverage"
  )
  @Mapping(
    target = "includeRequestBodySchemaCoverage",
    source = "openApiCoverage.requestBodySchemaCoverage"
  )
  @Mapping(
    target = "includeErrorResponseCoverage",
    source = "openApiCoverage.errorResponseCoverage"
  )
  @Mapping(
    target = "includeContentTypeCoverage",
    source = "openApiCoverage.contentTypeCoverage"
  )
  QualityGateConfiguration fromDto(QualityGateConfig qualityGateConfig);

  @Mapping(
    source = "includePathCoverage",
    target = "openApiCoverage.pathCoverage"
  )
  QualityGateConfig toDto(QualityGateConfiguration qualityGateConfig);
}
