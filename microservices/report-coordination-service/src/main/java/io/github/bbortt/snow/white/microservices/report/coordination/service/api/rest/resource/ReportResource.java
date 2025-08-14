/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.resource;

import static io.github.bbortt.snow.white.commons.web.PaginationUtils.generatePaginationHttpHeaders;
import static io.github.bbortt.snow.white.commons.web.PaginationUtils.toPageable;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.resource.ReportResource.ReportOrErrorResponse.errorResponse;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.resource.ReportResource.ReportOrErrorResponse.qualityGateReport;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.IN_PROGRESS;
import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.ReportApi;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.ListQualityGateReports200ResponseInner;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.ListQualityGateReports500Response;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper.QualityGateReportMapper;
import io.github.bbortt.snow.white.microservices.report.coordination.service.junit.JUnitReportCreationException;
import io.github.bbortt.snow.white.microservices.report.coordination.service.junit.JUnitReporter;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.ReportService;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReportResource implements ReportApi {

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

    try {
      var jUnitReport = jUnitReporter.transformToJUnitReport(
        reportOrError.qualityGateReport()
      );
      return ResponseEntity.ok()
        .header(
          CONTENT_DISPOSITION,
          format("attachment; filename=\"%s\"", jUnitReport.getFilename())
        )
        .body(jUnitReport);
    } catch (JUnitReportCreationException e) {
      return ResponseEntity.internalServerError().body(
        ListQualityGateReports500Response.builder()
          .code(INTERNAL_SERVER_ERROR.getReasonPhrase())
          .message(e.getMessage())
          .build()
      );
    }
  }

  @Override
  public ResponseEntity<
    List<ListQualityGateReports200ResponseInner>
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
        ResponseEntity.status(NOT_FOUND).body(
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
        ResponseEntity.status(ACCEPTED).body(
          qualityGateReportMapper.toListDto(report)
        )
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
