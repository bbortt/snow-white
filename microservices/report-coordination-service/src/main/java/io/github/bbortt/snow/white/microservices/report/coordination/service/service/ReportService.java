/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.service;

import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.IN_PROGRESS;

import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportParameters;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.repository.QualityGateReportRepository;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.exception.QualityGateNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

  private final String calculationRequestTopic;

  private final KafkaTemplate<
    String,
    QualityGateCalculationRequestEvent
  > kafkaTemplate;
  private final QualityGateReportRepository qualityGateReportRepository;
  private final QualityGateService qualityGateService;

  public ReportService(
    KafkaTemplate<String, QualityGateCalculationRequestEvent> kafkaTemplate,
    QualityGateReportRepository qualityGateReportRepository,
    ReportCoordinationServiceProperties reportCoordinationServiceProperties,
    QualityGateService qualityGateService
  ) {
    this.calculationRequestTopic =
      reportCoordinationServiceProperties.getCalculationRequestTopic();

    this.kafkaTemplate = kafkaTemplate;
    this.qualityGateReportRepository = qualityGateReportRepository;

    this.qualityGateService = qualityGateService;
  }

  public Optional<QualityGateReport> findReportByCalculationId(
    UUID calculationId
  ) {
    return qualityGateReportRepository.findById(calculationId);
  }

  public QualityGateReport initializeQualityGateCalculation(
    String qualityGateConfigName,
    ReportParameters reportParameters
  ) throws QualityGateNotFoundException {
    var qualityGateConfig = qualityGateService
      .findQualityGateConfigByName(qualityGateConfigName)
      .orElseThrow(() ->
        new QualityGateNotFoundException(qualityGateConfigName)
      );

    var qualityGateReport = createInitialReport(
      qualityGateConfig.getName(),
      reportParameters
    );

    return dispatchOpenApiCoverageCalculation(qualityGateReport);
  }

  private QualityGateReport createInitialReport(
    String qualityGateConfigName,
    ReportParameters reportParameters
  ) {
    return qualityGateReportRepository.save(
      QualityGateReport.builder()
        .qualityGateConfigName(qualityGateConfigName)
        .reportParameters(reportParameters)
        .build()
    );
  }

  private QualityGateReport dispatchOpenApiCoverageCalculation(
    QualityGateReport qualityGateReport
  ) {
    var reportParameters = qualityGateReport.getReportParameters();

    kafkaTemplate.send(
      calculationRequestTopic,
      qualityGateReport.getCalculationId().toString(),
      QualityGateCalculationRequestEvent.builder()
        .serviceName(reportParameters.getServiceName())
        .apiName(reportParameters.getApiName())
        .apiVersion(reportParameters.getApiVersion())
        .lookbackWindow(reportParameters.getLookbackWindow())
        .build()
    );

    return qualityGateReportRepository.save(
      qualityGateReport.withOpenApiCoverageStatus(IN_PROGRESS)
    );
  }

  public QualityGateReport update(QualityGateReport qualityGateReport) {
    return qualityGateReportRepository.save(qualityGateReport);
  }

  public Page<QualityGateReport> findAllReports(Pageable pageable) {
    return qualityGateReportRepository.findAll(pageable);
  }
}
