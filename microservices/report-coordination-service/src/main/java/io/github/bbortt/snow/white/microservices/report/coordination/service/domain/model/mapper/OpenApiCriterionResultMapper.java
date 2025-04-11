package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper;

import static java.lang.Boolean.TRUE;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.OpenApiCriterionResult;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = SPRING)
public interface OpenApiCriterionResultMapper {
  String OPEN_API_CRITERIA_TO_INCLUDED_IN_REPORT =
    "openApiCriteriaToIncludedInReport";
  String OPEN_API_CRITERIA_TO_NAME = "openApiCriteriaToName";

  Set<OpenApiCriterionResult> map(
    Set<
      io.github.bbortt.snow.white.commons.event.dto.OpenApiCriterionResult
    > openApiCriterionResults
  );

  @Mapping(
    target = "name",
    source = "openApiCriteria",
    qualifiedByName = OPEN_API_CRITERIA_TO_NAME
  )
  @Mapping(
    target = "includedInReport",
    source = "openApiCriteria",
    qualifiedByName = OPEN_API_CRITERIA_TO_INCLUDED_IN_REPORT
  )
  @Mapping(target = "qualityGateReport", ignore = true)
  OpenApiCriterionResult map(
    io.github.bbortt.snow.white.commons.event.dto.OpenApiCriterionResult source
  );

  @Named(OPEN_API_CRITERIA_TO_INCLUDED_IN_REPORT)
  default Boolean openApiCriteriaToIncludedInReport(
    OpenApiCriteria openApiCriteria
  ) {
    return TRUE;
  }

  @Named(OPEN_API_CRITERIA_TO_NAME)
  default String openApiCriteriaToName(OpenApiCriteria openApiCriteria) {
    return openApiCriteria.name();
  }
}
