package io.github.bbortt.snow.white.microservices.report.coordination.service.service.dto.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.microservices.report.coordination.service.service.dto.OpenApiCoverageConfig;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.dto.QualityGateConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING)
public interface QualityGateConfigMapper {
  QualityGateConfig fromDto(
    io.github.bbortt.snow.white.microservices.report.coordination.service.api.client.qualitygateapi.dto.QualityGateConfig qualityGateConfig
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
    io.github.bbortt.snow.white.microservices.report.coordination.service.api.client.qualitygateapi.dto.OpenApiCoverageConfig openApiCoverageConfig
  );
}
