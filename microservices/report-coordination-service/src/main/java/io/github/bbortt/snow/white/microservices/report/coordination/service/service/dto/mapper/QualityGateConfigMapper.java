package io.github.bbortt.snow.white.microservices.report.coordination.service.service.dto.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.microservices.report.coordination.service.service.dto.QualityGateConfig;
import org.mapstruct.Mapper;

@Mapper(componentModel = SPRING)
public interface QualityGateConfigMapper {
  QualityGateConfig fromDto(
    io.github.bbortt.snow.white.microservices.report.coordination.service.api.client.qualitygateapi.dto.QualityGateConfig qualityGateConfig
  );
}
