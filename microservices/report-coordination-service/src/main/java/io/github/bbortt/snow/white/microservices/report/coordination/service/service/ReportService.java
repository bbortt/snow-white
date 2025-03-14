package io.github.bbortt.snow.white.microservices.report.coordination.service.service;

import io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordination.service.rest.dto.QualityGateRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

  private String calculationRequestTopic;

  private final KafkaTemplate<String, String> kafkaTemplate;

  public ReportService(
    ReportCoordinationServiceProperties reportCoordinationServiceProperties,
    KafkaTemplate<String, String> kafkaTemplate
  ) {
    this.calculationRequestTopic =
      reportCoordinationServiceProperties.getCalculationRequestTopic();
    this.kafkaTemplate = kafkaTemplate;
  }

  public QualityGateReport createInitialReport(
    String qualityGateName,
    QualityGateRequest qualityGateRequest
  ) {
    return null;
  }

  public void dispatchCalculations(QualityGateReport qualityGateReport) {
    // TODO: https://docs.spring.io/spring-kafka/reference/kafka/serdes.html#json-serde
    kafkaTemplate.send(calculationRequestTopic, "");
  }
}
