package io.github.bbortt.snow.white.microservices.report.coordination.service.client.model.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.microservices.report.coordination.service.client.model.OpenApiCoverage;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.OpenApiCoverageConfig;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.QualityGateConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING)
public interface QualityGateConfigMapper {
  @Mapping(target = "openApiCoverageConfig", source = "openApiCoverage")
  QualityGateConfig fromDto(
    io.github.bbortt.snow.white.microservices.report.coordination.service.client.model.QualityGateConfig qualityGateConfig
  );

  @Mapping(target = "includesPathCoverage", source = "pathCoverage")
  @Mapping(
    target = "includesResponseCodeCoverage",
    source = "responseCodeCoverage"
  )
  @Mapping(
    target = "includesErrorResponseCoverage",
    source = "errorResponseCoverage"
  )
  @Mapping(
    target = "includesRequiredParameterCoverage",
    source = "requiredParameterCoverage"
  )
  @Mapping(
    target = "includesQueryParameterCoverage",
    source = "queryParameterCoverage"
  )
  @Mapping(
    target = "includesHeaderParameterCoverage",
    source = "headerParameterCoverage"
  )
  @Mapping(
    target = "includesRequestBodySchemaCoverage",
    source = "requestBodySchemaCoverage"
  )
  @Mapping(
    target = "includesContentTypeCoverage",
    source = "contentTypeCoverage"
  )
  OpenApiCoverageConfig mapOpenApiCoverageConfig(
    OpenApiCoverage openApiCoverage
  );
}
