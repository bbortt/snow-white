package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.OpenApiCoverageConfig;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.OpenApiCoverageConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING)
public interface QualityGateConfigurationMapper {
  @Mapping(
    target = "openApiCoverageConfiguration",
    source = "openApiCoverageConfig"
  )
  QualityGateConfiguration fromDto(QualityGateConfig qualityGateConfig);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "includePathCoverage", source = "pathCoverage")
  @Mapping(
    target = "includeResponseCodeCoverage",
    source = "responseCodeCoverage"
  )
  @Mapping(
    target = "includeRequiredParameterCoverage",
    source = "requiredParameterCoverage"
  )
  @Mapping(
    target = "includeQueryParameterCoverage",
    source = "queryParameterCoverage"
  )
  @Mapping(
    target = "includeHeaderParameterCoverage",
    source = "headerParameterCoverage"
  )
  @Mapping(
    target = "includeRequestBodySchemaCoverage",
    source = "requestBodySchemaCoverage"
  )
  @Mapping(
    target = "includeErrorResponseCoverage",
    source = "errorResponseCoverage"
  )
  @Mapping(
    target = "includeContentTypeCoverage",
    source = "contentTypeCoverage"
  )
  OpenApiCoverageConfiguration map(OpenApiCoverageConfig openApiCoverageConfig);

  @Mapping(
    target = "openApiCoverageConfig",
    source = "openApiCoverageConfiguration"
  )
  QualityGateConfig toDto(QualityGateConfiguration qualityGateConfig);

  @Mapping(target = "pathCoverage", source = "includePathCoverage")
  @Mapping(
    target = "responseCodeCoverage",
    source = "includeResponseCodeCoverage"
  )
  @Mapping(
    target = "requiredParameterCoverage",
    source = "includeRequiredParameterCoverage"
  )
  @Mapping(
    target = "queryParameterCoverage",
    source = "includeQueryParameterCoverage"
  )
  @Mapping(
    target = "headerParameterCoverage",
    source = "includeHeaderParameterCoverage"
  )
  @Mapping(
    target = "requestBodySchemaCoverage",
    source = "includeRequestBodySchemaCoverage"
  )
  @Mapping(
    target = "errorResponseCoverage",
    source = "includeErrorResponseCoverage"
  )
  @Mapping(
    target = "contentTypeCoverage",
    source = "includeContentTypeCoverage"
  )
  OpenApiCoverageConfig map(
    OpenApiCoverageConfiguration openApiCoverageConfiguration
  );
}
