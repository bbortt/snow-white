/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGate202Response;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGateRequest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.GetReportByCalculationId200Response;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.ListQualityGateReports200ResponseInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.ListQualityGateReports200ResponseInnerCalculationRequest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(
  componentModel = SPRING,
  uses = {
    ApiTestMapper.class,
    ReportParameterMapper.class,
    ReportStatusMapper.class,
  }
)
public interface QualityGateReportMapper {
  @Mapping(
    target = "calculationRequest",
    source = ".",
    qualifiedByName = "toIncludeApis"
  )
  @Mapping(target = "initiatedAt", source = "createdAt")
  @Mapping(target = "interfaces", source = "apiTests")
  @Mapping(target = "status", source = "reportStatus")
  CalculateQualityGate202Response toDto(QualityGateReport qualityGateReport);

  @Named("toIncludeApis")
  @Mapping(target = "includeApis", source = "apiTests")
  @Mapping(target = "lookbackWindow", source = "reportParameter.lookbackWindow")
  @Mapping(
    target = "attributeFilters",
    source = "reportParameter.attributeFilters"
  )
  CalculateQualityGateRequest toIncludeApis(
    QualityGateReport qualityGateReport
  );

  @Mapping(
    target = "calculationRequest",
    source = ".",
    qualifiedByName = "toListIncludeApis"
  )
  @Mapping(target = "initiatedAt", source = "createdAt")
  @Mapping(target = "interfaces", source = "apiTests")
  @Mapping(target = "status", source = "reportStatus")
  ListQualityGateReports200ResponseInner toListDto(
    QualityGateReport qualityGateReport
  );

  @Named("toListIncludeApis")
  @Mapping(target = "includeApis", source = "apiTests")
  @Mapping(target = "lookbackWindow", source = "reportParameter.lookbackWindow")
  @Mapping(
    target = "attributeFilters",
    source = "reportParameter.attributeFilters"
  )
  ListQualityGateReports200ResponseInnerCalculationRequest toListIncludeApis(
    QualityGateReport qualityGateReport
  );

  /**
   * The single-report shape, which differs from {@link #toListDto(QualityGateReport)} in the
   * findings hanging off every criterion result and nothing else. The calculation request is
   * shared: the generator deduplicated it, so there is one DTO for both reads.
   */
  @RealizesSw(SwTraceables.SW_031_FINDINGS_SERVED_WITH_THE_REPORT)
  @Mapping(
    target = "calculationRequest",
    source = ".",
    qualifiedByName = "toListIncludeApis"
  )
  @Mapping(target = "initiatedAt", source = "createdAt")
  @Mapping(target = "interfaces", source = "apiTests")
  @Mapping(target = "status", source = "reportStatus")
  GetReportByCalculationId200Response toReportDto(
    QualityGateReport qualityGateReport
  );

  default OffsetDateTime map(Instant instant) {
    return OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
  }
}
