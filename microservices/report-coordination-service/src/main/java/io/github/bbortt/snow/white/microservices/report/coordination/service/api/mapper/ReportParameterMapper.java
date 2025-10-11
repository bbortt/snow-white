/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.CalculateQualityGateRequest;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportParameter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING)
public interface ReportParameterMapper {
  @Mapping(target = "calculationId", ignore = true)
  @Mapping(target = "lookbackWindow", defaultValue = "1h")
  @Mapping(target = "qualityGateReport", ignore = true)
  ReportParameter fromDto(
    CalculateQualityGateRequest qualityGateCalculationRequest
  );
}
