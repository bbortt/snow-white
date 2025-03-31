package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING)
public interface ReportMapper {
  @Mapping(target = "calculationRequest", ignore = true)
  @Mapping(target = "initiatedAt", source = "createdAt")
  @Mapping(target = "status", source = "reportStatus")
  io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.QualityGateReport toDto(
    QualityGateReport qualityGateReport
  );

  default OffsetDateTime map(Instant instant) {
    return OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
  }

  default io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.QualityGateReport.StatusEnum map(
    ReportStatus reportStatus
  ) {
    return switch (reportStatus) {
      case IN_PROGRESS -> io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.QualityGateReport.StatusEnum.IN_PROGRESS;
      case FAILED -> io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.QualityGateReport.StatusEnum.FAILED;
      case PASSED -> io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.QualityGateReport.StatusEnum.PASSED;
    };
  }
}
