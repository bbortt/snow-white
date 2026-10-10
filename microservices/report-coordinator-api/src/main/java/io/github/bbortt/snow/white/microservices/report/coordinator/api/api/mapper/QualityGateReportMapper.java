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
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGateRequestWaiversInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.GetReportByCalculationId200Response;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.ListQualityGateReports200ResponseInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.ListQualityGateReports200ResponseInnerCalculationRequest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.ListQualityGateReports200ResponseInnerCalculationRequestWaiversInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.Waiver;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * Every read shape is projected here, which is what lets {@code minCoveragePercentage} reach all
 * four of them at once: the three methods below cover the paginated list read, the single-report
 * {@code 200}, the calculation trigger's {@code 202} and - through {@link #toListDto(QualityGateReport)},
 * which both report endpoints answer a running report with - the two polls.
 * <p>
 * The property is mapped by name off the report entity, so no {@code @Mapping} is written for it.
 * That is not left to trust: the module compiles with
 * {@code -Amapstruct.unmappedTargetPolicy=ERROR}, so a name that stopped matching fails the build
 * rather than silently serving a null threshold.
 */
@Mapper(
  componentModel = SPRING,
  uses = {
    ApiTestMapper.class,
    ReportParameterMapper.class,
    ReportStatusMapper.class,
  }
)
@RealizesSw(SwTraceables.SW_039_REPORT_PUBLISHES_ITS_PINNED_THRESHOLD)
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
  @Mapping(
    target = "waivers",
    source = "reportParameter.waivers",
    qualifiedByName = "toRequestWaivers"
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
  @Mapping(
    target = "waivers",
    source = "reportParameter.waivers",
    qualifiedByName = "toReportWaivers"
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

  CalculateQualityGateRequestWaiversInner toRequestWaiver(Waiver waiver);

  ListQualityGateReports200ResponseInnerCalculationRequestWaiversInner toReportWaiver(
    Waiver waiver
  );

  /**
   * A calculation that was given no waivers answers no {@code waivers} field at all, rather than an
   * empty array: an empty array reads as a document that was submitted and happened to be empty,
   * which is a different thing from never having been asked.
   */
  @Named("toRequestWaivers")
  default @Nullable List<CalculateQualityGateRequestWaiversInner> toRequestWaivers(
    List<Waiver> waivers
  ) {
    return waivers.isEmpty()
      ? null
      : waivers.stream().map(this::toRequestWaiver).toList();
  }

  /**
   * The same absent-rather-than-empty projection as {@link #toRequestWaivers(List)}, for the read
   * shape the generator emitted separately.
   */
  @Named("toReportWaivers")
  default @Nullable List<ListQualityGateReports200ResponseInnerCalculationRequestWaiversInner> toReportWaivers(
    List<Waiver> waivers
  ) {
    return waivers.isEmpty()
      ? null
      : waivers.stream().map(this::toReportWaiver).toList();
  }

  default OffsetDateTime map(Instant instant) {
    return OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
  }
}
