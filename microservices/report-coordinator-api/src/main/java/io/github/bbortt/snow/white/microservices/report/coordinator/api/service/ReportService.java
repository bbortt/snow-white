/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static io.github.bbortt.snow.white.commons.event.dto.AttributeFilterOperator.STRING_EQUALS;
import static java.util.stream.Collectors.toSet;

import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.ApiTestRepository;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.QualityGateReportRepository;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception.QualityGateNotFoundException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

  private final String calculationRequestTopic;

  private final KafkaTemplate<
    String,
    QualityGateCalculationRequestEvent
  > kafkaTemplate;

  private final QualityGateService qualityGateService;

  private final ApiTestRepository apiTestRepository;
  private final QualityGateReportRepository qualityGateReportRepository;

  public ReportService(
    KafkaTemplate<String, QualityGateCalculationRequestEvent> kafkaTemplate,
    QualityGateService qualityGateService,
    ApiTestRepository apiTestRepository,
    QualityGateReportRepository qualityGateReportRepository,
    ReportCoordinationServiceProperties reportCoordinationServiceProperties
  ) {
    this.calculationRequestTopic =
      reportCoordinationServiceProperties.getCalculationRequestTopic();

    this.kafkaTemplate = kafkaTemplate;

    this.qualityGateService = qualityGateService;

    this.apiTestRepository = apiTestRepository;
    this.qualityGateReportRepository = qualityGateReportRepository;
  }

  public Optional<QualityGateReport> findReportByCalculationId(
    UUID calculationId
  ) {
    return qualityGateReportRepository.findById(calculationId);
  }

  @Transactional
  public QualityGateReport initializeQualityGateCalculation(
    String qualityGateConfigName,
    Set<ApiTest> apiTests,
    ReportParameter reportParameter
  ) throws QualityGateNotFoundException {
    var qualityGateConfig = qualityGateService.findQualityGateConfigByName(
      qualityGateConfigName
    );

    var qualityGateReport = persistInitialQualityGateReport(
      qualityGateConfig.getName(),
      apiTests,
      reportParameter
    );

    qualityGateReport
      .getApiTests()
      .forEach(apiTest ->
        dispatchQualityGateCalculationRequest(
          qualityGateReport.getCalculationId(),
          qualityGateReport.getReportParameter(),
          apiTest
        )
      );

    return qualityGateReport;
  }

  private QualityGateReport persistInitialQualityGateReport(
    String qualityGateConfigName,
    Set<ApiTest> apiTests,
    ReportParameter reportParameter
  ) {
    var calculationId = UUID.randomUUID();

    var qualityGateReport = QualityGateReport.builder()
      .calculationId(calculationId)
      .qualityGateConfigName(qualityGateConfigName)
      .reportParameter(reportParameter.withCalculationId(calculationId))
      .build();

    final var persistedQualityGateReport = qualityGateReportRepository.save(
      qualityGateReport
    );

    var persistedApiTests = apiTests
      .stream()
      .map(apiTest -> apiTest.withQualityGateReport(persistedQualityGateReport))
      .map(apiTestRepository::save)
      .collect(toSet());

    return persistedQualityGateReport.withApiTests(persistedApiTests);
  }

  private void dispatchQualityGateCalculationRequest(
    UUID calculationId,
    ReportParameter reportParameter,
    ApiTest apiTest
  ) {
    kafkaTemplate.send(
      calculationRequestTopic,
      calculationId.toString(),
      QualityGateCalculationRequestEvent.builder()
        .apiInformation(
          ApiInformation.builder()
            .serviceName(apiTest.getServiceName())
            .apiName(apiTest.getApiName())
            .apiVersion(apiTest.getApiVersion())
            .build()
        )
        .lookbackWindow(reportParameter.getLookbackWindow())
        .attributeFilters(
          parseReportParameterAttributeFiltersToDto(reportParameter)
        )
        .build()
    );
  }

  private static Set<AttributeFilter> parseReportParameterAttributeFiltersToDto(
    ReportParameter reportParameter
  ) {
    return reportParameter
      .getAttributeFilters()
      .entrySet()
      .stream()
      .map(attributeFilter ->
        new AttributeFilter(
          attributeFilter.getKey(),
          STRING_EQUALS,
          attributeFilter.getValue()
        )
      )
      .collect(toSet());
  }

  public QualityGateReport update(QualityGateReport qualityGateReport) {
    return qualityGateReportRepository.save(qualityGateReport);
  }

  public Page<QualityGateReport> findAllReports(Pageable pageable) {
    return qualityGateReportRepository.findAll(pageable);
  }
}
