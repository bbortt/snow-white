/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.CalculateQualityGate202Response;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.CalculateQualityGate202ResponseOpenApiTestResultsInner;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.ListQualityGateReports200ResponseInner;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.ListQualityGateReports200ResponseInnerOpenApiTestResultsInner;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING)
public interface QualityGateReportMapper {
  @Mapping(target = "calculationRequest", source = "reportParameters")
  @Mapping(target = "initiatedAt", source = "createdAt")
  @Mapping(target = "status", source = "reportStatus")
  CalculateQualityGate202Response toCalculateQualityGateResponse(
    QualityGateReport qualityGateReport
  );

  @Mapping(target = "id", source = "openApiTestCriteria")
  @Mapping(target = "isIncludedInQualityGate", source = "includedInReport")
  CalculateQualityGate202ResponseOpenApiTestResultsInner toCalculateQualityGateResponse(
    OpenApiTestResult openApiTestResult
  );

  @Mapping(target = "calculationRequest", source = "reportParameters")
  @Mapping(target = "initiatedAt", source = "createdAt")
  @Mapping(target = "status", source = "reportStatus")
  ListQualityGateReports200ResponseInner toListQualityGateReportsResponse(
    QualityGateReport qualityGateReport
  );

  @Mapping(target = "id", source = "openApiTestCriteria")
  @Mapping(target = "isIncludedInQualityGate", source = "includedInReport")
  ListQualityGateReports200ResponseInnerOpenApiTestResultsInner toListQualityGateReportsResponse(
    OpenApiTestResult openApiTestResult
  );

  default OffsetDateTime map(Instant instant) {
    return OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
  }

  default CalculateQualityGate202Response.StatusEnum toCalculateQualityGateResponse(
    ReportStatus reportStatus
  ) {
    return switch (reportStatus) {
      case
        NOT_STARTED,
        IN_PROGRESS -> CalculateQualityGate202Response.StatusEnum.IN_PROGRESS;
      case FAILED -> CalculateQualityGate202Response.StatusEnum.FAILED;
      case PASSED -> CalculateQualityGate202Response.StatusEnum.PASSED;
    };
  }

  default ListQualityGateReports200ResponseInner.StatusEnum toListQualityGateReportsResponse(
    ReportStatus reportStatus
  ) {
    return switch (reportStatus) {
      case
        NOT_STARTED,
        IN_PROGRESS -> ListQualityGateReports200ResponseInner.StatusEnum.IN_PROGRESS;
      case FAILED -> ListQualityGateReports200ResponseInner.StatusEnum.FAILED;
      case PASSED -> ListQualityGateReports200ResponseInner.StatusEnum.PASSED;
    };
  }
}
