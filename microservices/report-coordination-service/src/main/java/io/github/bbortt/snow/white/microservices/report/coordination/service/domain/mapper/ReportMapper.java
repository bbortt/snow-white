package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.Report;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.ReportStatus;
import io.github.bbortt.snow.white.microservices.report.coordination.service.rest.dto.QualityGate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING)
public interface ReportMapper {
  @Mapping(target = "initiatedAt", ignore = true)
  @Mapping(target = "status", source = "reportStatus")
  @Mapping(target = "qualityGateRequest", ignore = true)
  QualityGate toDto(Report report);

  default QualityGate.StatusEnum mapStatusEnum(ReportStatus reportStatus) {
    return switch (reportStatus) {
      case IN_PROGRESS -> QualityGate.StatusEnum.IN_PROGRESS;
      case FAILED -> QualityGate.StatusEnum.FAILED;
      case PASSED -> QualityGate.StatusEnum.PASSED;
    };
  }
}
