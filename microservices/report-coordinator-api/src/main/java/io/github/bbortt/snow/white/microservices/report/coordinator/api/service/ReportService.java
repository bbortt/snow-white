/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.FINISHED_EXCEPTIONALLY;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper.ApiTestResultMapper;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.ApiTestRepository;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.QualityGateReportRepository;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception.QualityGateNotFoundException;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

  private final QualityGateService qualityGateService;

  private final ApiTestRepository apiTestRepository;
  private final QualityGateReportRepository qualityGateReportRepository;

  private final ApiTestResultMapper apiTestResultMapper;
  private final QualityGateReportApiTestsFilter qualityGateReportApiTestsFilter;
  private final ApiTestResultLinker apiTestResultLinker;
  private final QualityGateStatusCalculator qualityGateStatusCalculator;

  private final QualityGateCalculationRequestDispatcher dispatcher;

  @WithSpan
  public Optional<QualityGateReport> findReportByCalculationId(
    UUID calculationId
  ) {
    return qualityGateReportRepository.findById(calculationId);
  }

  @WithSpan
  @Transactional
  public void updateReportWithOpenApiCoverageResults(
    UUID calculationId,
    OpenApiCoverageResponseEvent event
  ) {
    var report = findReportByCalculationId(calculationId).orElseGet(() -> {
      logger.warn(
        "Received OpenAPI coverage response for unknown calculation ID: {}",
        calculationId
      );
      return null;
    });

    if (isNull(report)) {
      return;
    }

    if (nonNull(event.errorMessage())) {
      handleExceptionalResponse(report, event.errorMessage());
      return;
    }

    handleSuccessfulResponse(report, event);
  }

  private void handleExceptionalResponse(
    QualityGateReport report,
    String errorMessage
  ) {
    var updated = report
      .withStackTrace(errorMessage)
      .withReportStatus(FINISHED_EXCEPTIONALLY);
    update(updated);
  }

  private void handleSuccessfulResponse(
    QualityGateReport report,
    OpenApiCoverageResponseEvent event
  ) {
    var qualityGateConfigName = report.getQualityGateConfigName();

    final QualityGateReport updatedReport;
    try {
      var qualityGateConfig = qualityGateService.findQualityGateConfigByName(
        qualityGateConfigName
      );

      var apiTest =
        qualityGateReportApiTestsFilter.findApiTestMatchingApiInformationInQualityGateReport(
          report,
          event.apiInformation()
        );

      apiTestResultLinker.addApiTestResultsToApiTest(
        apiTestResultMapper.fromDtos(
          requireNonNull(event.openApiTestResults()),
          apiTest
        ),
        apiTest,
        qualityGateConfig.getOpenApiCriteria()
      );

      updatedReport = qualityGateStatusCalculator.withUpdatedReportStatus(
        report
      );
    } catch (QualityGateNotFoundException e) {
      logger.warn(
        "Cannot find quality-gate config: {}",
        qualityGateConfigName,
        e
      );
      return;
    }

    update(updatedReport);
  }

  @WithSpan
  @Transactional
  public QualityGateReport initializeQualityGateCalculation(
    String qualityGateConfigName,
    Set<ApiTest> apiTests,
    ReportParameter reportParameter
  ) throws QualityGateNotFoundException {
    var qualityGateConfig = qualityGateService.findQualityGateConfigByName(
      qualityGateConfigName
    );

    var report = persistInitialQualityGateReport(
      qualityGateConfig.getName(),
      apiTests,
      reportParameter
    );

    dispatcher.dispatch(
      report.getCalculationId(),
      report.getReportParameter(),
      report.getApiTests()
    );

    return report;
  }

  private QualityGateReport persistInitialQualityGateReport(
    String qualityGateConfigName,
    Set<ApiTest> apiTests,
    ReportParameter reportParameter
  ) {
    var report = QualityGateReport.builder()
      .calculationId(reportParameter.getCalculationId())
      .qualityGateConfigName(qualityGateConfigName)
      .reportParameter(reportParameter)
      .build();

    var persistedReport = qualityGateReportRepository.save(report);

    var persistedApiTests = apiTests
      .stream()
      .map(apiTest -> apiTest.withQualityGateReport(persistedReport))
      .map(apiTestRepository::save)
      .collect(toSet());

    return persistedReport.withApiTests(persistedApiTests);
  }

  @WithSpan
  public QualityGateReport update(QualityGateReport qualityGateReport) {
    return qualityGateReportRepository.save(qualityGateReport);
  }

  @WithSpan
  public Page<@NonNull QualityGateReport> findAllReports(Pageable pageable) {
    return qualityGateReportRepository.findAll(pageable);
  }
}
