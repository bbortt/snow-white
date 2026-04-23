/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGate202Response;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGate202ResponseInterfacesInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.ListQualityGateReports200ResponseInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.ListQualityGateReports200ResponseInnerInterfacesInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus;
import org.mapstruct.Mapper;
import org.mapstruct.ValueMapping;

@Mapper(componentModel = SPRING)
public interface ReportStatusMapper {
  @ValueMapping(target = "IN_PROGRESS", source = "NOT_STARTED")
  @ValueMapping(target = "IN_PROGRESS", source = "IN_PROGRESS")
  CalculateQualityGate202Response.StatusEnum toResponseStatusEnum(
    ReportStatus reportStatus
  );

  @ValueMapping(target = "IN_PROGRESS", source = "NOT_STARTED")
  @ValueMapping(target = "IN_PROGRESS", source = "IN_PROGRESS")
  ListQualityGateReports200ResponseInner.StatusEnum toResponseInnerStatusEnum(
    ReportStatus reportStatus
  );

  @ValueMapping(target = "IN_PROGRESS", source = "NOT_STARTED")
  @ValueMapping(target = "IN_PROGRESS", source = "IN_PROGRESS")
  CalculateQualityGate202ResponseInterfacesInner.StatusEnum toResponseInterfacesInnerStatusEnum(
    ReportStatus reportStatus
  );

  @ValueMapping(target = "IN_PROGRESS", source = "NOT_STARTED")
  @ValueMapping(target = "IN_PROGRESS", source = "IN_PROGRESS")
  ListQualityGateReports200ResponseInnerInterfacesInner.StatusEnum toResponseInnerInterfacesInnerStatusEnum(
    ReportStatus reportStatus
  );
}
