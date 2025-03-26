package io.github.bbortt.snow.white.microservices.report.coordination.service.service;

import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.Report;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.ReportParameters;
import io.github.bbortt.snow.white.microservices.report.coordination.service.repository.ReportRepository;
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
  private final ReportRepository reportRepository;
  private final QualityGateService qualityGateService;

  public ReportService(
    KafkaTemplate<String, QualityGateCalculationRequestEvent> kafkaTemplate,
    ReportRepository reportRepository,
    ReportCoordinationServiceProperties reportCoordinationServiceProperties,
    QualityGateService qualityGateService
  ) {
    this.calculationRequestTopic =
      reportCoordinationServiceProperties.getCalculationRequestTopic();

    this.kafkaTemplate = kafkaTemplate;
    this.reportRepository = reportRepository;

    this.qualityGateService = qualityGateService;
  }

  public Optional<Report> findReportByCalculationId(UUID calculationId) {
    return reportRepository.findById(calculationId);
  }

  public Report initializeQualityGateCalculation(
    String qualityGateConfigName,
    ReportParameters reportParameters
  ) throws QualityGateNotFoundException {
    qualityGateService
      .findQualityGateConfigByName(qualityGateConfigName)
      .orElseThrow(() -> new QualityGateNotFoundException(qualityGateConfigName)
      );

    var qualityGateReport = createInitialReport(
      qualityGateConfigName,
      reportParameters
    );

    dispatchOpenApiCoverageCalculation(qualityGateReport);

    return qualityGateReport;
  }

  private Report createInitialReport(
    String qualityGateConfigName,
    ReportParameters reportParameters
  ) {
    return reportRepository.save(
      Report.builder()
        .qualityGateConfigName(qualityGateConfigName)
        .reportParameters(reportParameters)
        .build()
    );
  }

  private void dispatchOpenApiCoverageCalculation(Report report) {
    kafkaTemplate.send(
      calculationRequestTopic,
      report.getCalculationId().toString(),
      new QualityGateCalculationRequestEvent(
        report.getReportParameters().getServiceName(),
        report.getReportParameters().getApiName(),
        report.getReportParameters().getApiVersion(),
        report.getReportParameters().getLookbackWindow()
      )
    );
  }

  public Report update(Report report) {
    return reportRepository.save(report);
  }
}
