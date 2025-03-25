package io.github.bbortt.snow.white.microservices.openapi.coverage.service.domain.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.domain.OpenApiCoverage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING)
public interface OpenApiCoverageMapper {
  @Mapping(target = "withPathCoverage", ignore = true)
  @Mapping(target = "withResponseCodeCoverage", ignore = true)
  @Mapping(target = "withRequiredParameterCoverage", ignore = true)
  @Mapping(target = "withQueryParameterCoverage", ignore = true)
  @Mapping(target = "withHeaderParameterCoverage", ignore = true)
  @Mapping(target = "withRequestBodySchemaCoverage", ignore = true)
  @Mapping(target = "withErrorResponseCoveredAtLeastOnce", ignore = true)
  @Mapping(target = "withErrorResponseCoverage", ignore = true)
  @Mapping(target = "withContentTypeCoverage", ignore = true)
  OpenApiCoverageResponseEvent toResponseEvent(OpenApiCoverage openApiCoverage);
}
