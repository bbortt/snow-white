/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGateRequest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING)
public interface ReportParameterMapper {
  @Mapping(target = "calculationId", source = "calculationId")
  @Mapping(target = "lookbackWindow", defaultValue = "1h")
  @Mapping(target = "qualityGateReport", ignore = true)
  ReportParameter fromDto(
    CalculateQualityGateRequest qualityGateCalculationRequest,
    UUID calculationId
  );
}
