package io.github.bbortt.snow.white.microservices.report.coordination.service.service;

import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportParameters;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.repository.QualityGateReportRepository;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.exception.QualityGateNotFoundException;
import java.util.Optional;
import java.util.UUID;
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
      .orElseThrow(() -> new QualityGateNotFoundException(qualityGateConfigName)
      );

    var qualityGateReport = createInitialReport(
      qualityGateConfig.getName(),
      reportParameters
    );

    dispatchOpenApiCoverageCalculation(qualityGateReport);

    return qualityGateReport;
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

  private void dispatchOpenApiCoverageCalculation(
    QualityGateReport qualityGateReport
  ) {
    kafkaTemplate.send(
      calculationRequestTopic,
      qualityGateReport.getCalculationId().toString(),
      new QualityGateCalculationRequestEvent(
        qualityGateReport.getReportParameters().getServiceName(),
        qualityGateReport.getReportParameters().getApiName(),
        qualityGateReport.getReportParameters().getApiVersion(),
        qualityGateReport.getReportParameters().getLookbackWindow()
      )
    );
  }

  public QualityGateReport update(QualityGateReport qualityGateReport) {
    return qualityGateReportRepository.save(qualityGateReport);
  }
}
