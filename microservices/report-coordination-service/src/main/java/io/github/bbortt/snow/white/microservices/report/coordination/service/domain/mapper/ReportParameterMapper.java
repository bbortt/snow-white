package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.ReportParameters;
import io.github.bbortt.snow.white.microservices.report.coordination.service.rest.dto.QualityGateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING)
public interface ReportParameterMapper {
  @Mapping(target = "id", ignore = true)
  ReportParameters fromDto(QualityGateRequest qualityGateRequest);
}
