/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.resource;

import static io.github.bbortt.snow.white.commons.web.PaginationUtils.generatePaginationHttpHeaders;
import static io.github.bbortt.snow.white.commons.web.PaginationUtils.toPageable;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.IN_PROGRESS;
import static java.lang.String.format;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
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

  /**
   * The only read that serves findings, and therefore the only one that fetches them: a drilldown
   * expands beside a coverage bar that is already on screen, so the second request the UI would
   * otherwise make is the one this endpoint exists to avoid.
   * <p>
   * The status decides before either report is read, and a still-running report answers the
   * findings-free shape. The projection and the read are two statements, so the report that is
   * about to be serialized settles the {@code 202} rather than the projection that selected the
   * branch: a report that completes between them answers the findings shape instead of an
   * {@code Accepted} whose body already says {@code PASSED}. This endpoint is also the
   * calculation's poll - {@code toolkit/cli} reads
   * nothing but {@code status} from it every two seconds - and a poll that dragged the evidence
   * table through a four-level join for a field it never reads is the cost the denormalized
   * {@code coverage} column exists to avoid. Deciding on the projected status rather than on a whole
   * report is what keeps that decision from loading the graph twice; see
   * {@code QualityGateReportRepository#findReportStatusByCalculationId(UUID)}.
   */
  @Override
  @RealizesSw(SwTraceables.SW_031_FINDINGS_SERVED_WITH_THE_REPORT)
  @RealizesSw(SwTraceables.SW_014_IN_PROGRESS_REPORT_ANSWERS_ACCEPTED)
  public ResponseEntity getReportByCalculationId(UUID calculationId) {
    var optionalStatus = reportService.findReportStatusByCalculationId(
      calculationId
    );

    if (optionalStatus.isEmpty()) {
      return reportNotFound(calculationId);
    }

    if (IN_PROGRESS.equals(optionalStatus.get())) {
      var optionalReport = reportService.findReportByCalculationId(
        calculationId
      );

      if (optionalReport.isEmpty()) {
        return reportNotFound(calculationId);
      }

      var report = optionalReport.get();

      if (IN_PROGRESS.equals(report.getReportStatus())) {
        return reportStillInProgress(report);
      }
    }

    var optionalReportWithFindings =
      reportService.findReportWithFindingsByCalculationId(calculationId);

    if (optionalReportWithFindings.isEmpty()) {
      return reportNotFound(calculationId);
    }

    return ResponseEntity.ok(
      qualityGateReportMapper.toReportDto(optionalReportWithFindings.get())
    );
  }

  /**
   * A still-running report answers {@code 202} with the partial report as JSON rather than XML,
   * because a half-populated JUnit document would read as a genuine passing test run.
   * <p>
   * It reads the report without its findings: the {@code 202} body is a courtesy for a CI consumer
   * waiting on XML, and no poll should pay for evidence nobody renders.
   */
  @Override
  @RealizesSw(SwTraceables.SW_014_IN_PROGRESS_REPORT_ANSWERS_ACCEPTED)
  public ResponseEntity getReportByCalculationIdAsJUnit(UUID calculationId) {
    var optionalReport = reportService.findReportByCalculationId(calculationId);

    if (optionalReport.isEmpty()) {
      return reportNotFound(calculationId);
    }

    var report = optionalReport.get();

    if (IN_PROGRESS.equals(report.getReportStatus())) {
      return reportStillInProgress(report);
    }

    var jUnitReport = jUnitReporter.transformToJUnitTestSuites(report);

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
  > listQualityGateReports(
    @Nullable Integer page,
    @Nullable Integer size,
    @Nullable String sort,
    @Nullable String serviceName,
    @Nullable String apiName,
    @Nullable String apiVersion
  ) {
    var qualityGateReports = reportService.findAllReports(
      serviceName,
      apiName,
      apiVersion,
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

  /**
   * The one {@code 202} both reads answer, so the two cannot drift: the findings-free shape as JSON,
   * whatever the endpoint's completed response would have been.
   */
  @RealizesSw(SwTraceables.SW_014_IN_PROGRESS_REPORT_ANSWERS_ACCEPTED)
  private ResponseEntity reportStillInProgress(QualityGateReport report) {
    return ResponseEntity.status(ACCEPTED)
      .contentType(APPLICATION_JSON)
      .body(qualityGateReportMapper.toListDto(report));
  }

  private ResponseEntity reportNotFound(UUID calculationId) {
    return ResponseEntity.status(NOT_FOUND)
      .contentType(APPLICATION_JSON)
      .body(
        ListQualityGateReports500Response.builder()
          .code(NOT_FOUND.getReasonPhrase())
          .message(format("No report by id '%s' exists!", calculationId))
          .build()
      );
  }
}
