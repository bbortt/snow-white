/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.resource;

import static io.github.bbortt.snow.white.commons.web.PaginationUtils.generatePaginationHttpHeaders;
import static io.github.bbortt.snow.white.commons.web.PaginationUtils.toPageable;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.resource.ReportResource.ReportOrErrorResponse.errorResponse;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.resource.ReportResource.ReportOrErrorResponse.qualityGateReport;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.IN_PROGRESS;
import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;

import io.github.bbortt.snow.white.commons.testing.VisibleForTesting;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper.QualityGateReportMapper;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.ReportApi;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.ListQualityGateReports200ResponseInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.ListQualityGateReports500Response;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.JUnitReporter;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.ReportService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReportResource implements ReportApi {

  @VisibleForTesting
  static final String JUNIT_XML_FILENAME = "snow-white-junit.xml";

  private final JUnitReporter jUnitReporter;
  private final ReportService reportService;
  private final QualityGateReportMapper qualityGateReportMapper;

  @Override
  public ResponseEntity getReportByCalculationId(UUID calculationId) {
    var reportOrError = getReportByCalculationIdOrErrorResponse(calculationId);
    if (nonNull(reportOrError.errorResponse())) {
      return reportOrError.errorResponse();
    }

    return ResponseEntity.ok(
      qualityGateReportMapper.toListDto(reportOrError.qualityGateReport())
    );
  }

  @Override
  public ResponseEntity getReportByCalculationIdAsJUnit(UUID calculationId) {
    var reportOrError = getReportByCalculationIdOrErrorResponse(calculationId);
    if (nonNull(reportOrError.errorResponse())) {
      return reportOrError.errorResponse();
    }

    var jUnitReport = jUnitReporter.transformToJUnitTestSuites(
      reportOrError.qualityGateReport()
    );

    return ResponseEntity.ok()
      .header(
        CONTENT_DISPOSITION,
        format("attachment; filename=\"%s\"", JUNIT_XML_FILENAME)
      )
      .contentType(APPLICATION_XML)
      .body(jUnitReport);
  }

  @Override
  public ResponseEntity<
    @NonNull List<ListQualityGateReports200ResponseInner>
  > listQualityGateReports(Integer page, Integer size, String sort) {
    var qualityGateReports = reportService.findAllReports(
      toPageable(page, size, sort)
    );

    return ResponseEntity.ok()
      .headers(generatePaginationHttpHeaders(qualityGateReports))
      .body(
        qualityGateReports
          .stream()
          .map(qualityGateReportMapper::toListDto)
          .toList()
      );
  }

  private ReportOrErrorResponse getReportByCalculationIdOrErrorResponse(
    UUID calculationId
  ) {
    var optionalReport = reportService.findReportByCalculationId(calculationId);

    if (optionalReport.isEmpty()) {
      return errorResponse(
        ResponseEntity.status(NOT_FOUND)
          .contentType(APPLICATION_JSON)
          .body(
            ListQualityGateReports500Response.builder()
              .code(NOT_FOUND.getReasonPhrase())
              .message(format("No report by id '%s' exists!", calculationId))
              .build()
          )
      );
    }

    var report = optionalReport.get();

    if (IN_PROGRESS.equals(report.getReportStatus())) {
      return errorResponse(
        ResponseEntity.status(ACCEPTED)
          .contentType(APPLICATION_JSON)
          .body(qualityGateReportMapper.toListDto(report))
      );
    }

    return qualityGateReport(report);
  }

  record ReportOrErrorResponse(
    @Nullable QualityGateReport qualityGateReport,
    @Nullable ResponseEntity errorResponse
  ) {
    static ReportOrErrorResponse errorResponse(ResponseEntity errorResponse) {
      return new ReportOrErrorResponse(null, errorResponse);
    }

    static ReportOrErrorResponse qualityGateReport(
      QualityGateReport qualityGateReport
    ) {
      return new ReportOrErrorResponse(qualityGateReport, null);
    }
  }
}
