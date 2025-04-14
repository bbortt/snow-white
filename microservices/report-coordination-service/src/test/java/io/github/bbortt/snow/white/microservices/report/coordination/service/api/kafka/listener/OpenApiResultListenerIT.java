/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.kafka.listener;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.PASSED;
import static java.math.BigDecimal.ONE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiCriterionResult;
import io.github.bbortt.snow.white.microservices.report.coordination.service.IntegrationTest;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.client.qualitygateapi.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportParameters;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.repository.QualityGateReportRepository;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.wiremock.spring.EnableWireMock;

@Testcontainers
@EnableWireMock
@IntegrationTest
class OpenApiResultListenerIT {

  @Container
  static final ConfluentKafkaContainer KAFKA_CONTAINER =
    new ConfluentKafkaContainer("confluentinc/cp-kafka:7.8.2").withExposedPorts(
      9092
    );

  @DynamicPropertySource
  static void kafkaProperties(DynamicPropertyRegistry registry) {
    registry.add(
      "spring.kafka.bootstrap-servers",
      KAFKA_CONTAINER::getBootstrapServers
    );
  }

  @Autowired
  private KafkaTemplate<String, OpenApiCoverageResponseEvent> kafkaTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private QualityGateReportRepository qualityGateReportRepository;

  @Autowired
  private ReportCoordinationServiceProperties reportCoordinationServiceProperties;

  @Test
  void calculateAndPersistQualityGateReport() throws JsonProcessingException {
    var calculationId = UUID.fromString("6fa77498-a7aa-48d2-8f1d-dee93eb45780");

    var qualityGateConfigName = "minimal";
    qualityGateReportRepository.save(
      QualityGateReport.builder()
        .calculationId(calculationId)
        .qualityGateConfigName(qualityGateConfigName)
        .reportParameters(
          ReportParameters.builder()
            .serviceName("throne-of-glass")
            .apiName("morath")
            .apiVersion("1.0.0")
            .lookbackWindow("1h")
            .build()
        )
        .build()
    );

    var qualityGateByNameEndpoint = createQualityGateApiWiremockStub(
      qualityGateConfigName
    );

    kafkaTemplate.send(
      reportCoordinationServiceProperties
        .getOpenapiCalculationResponse()
        .getTopic(),
      calculationId.toString(),
      new OpenApiCoverageResponseEvent(
        Set.of(new OpenApiCriterionResult(PATH_COVERAGE, ONE))
      )
    );

    await()
      .atMost(5, SECONDS)
      .untilAsserted(
        () -> qualityGateReportRepository.findById(calculationId),
        qualityGateReport ->
          assertThat(qualityGateReport)
            .isPresent()
            .get()
            .extracting(QualityGateReport::getReportStatus)
            .isEqualTo(PASSED)
      );

    verify(getRequestedFor(urlEqualTo(qualityGateByNameEndpoint)));
  }

  private @NotNull String createQualityGateApiWiremockStub(
    String qualityGateConfigName
  ) throws JsonProcessingException {
    var qualityGateConfig = new QualityGateConfig().name(qualityGateConfigName);

    var qualityGateByNameEndpoint =
      "/api/rest/v1/quality-gates/" + qualityGateConfigName;
    stubFor(
      get(qualityGateByNameEndpoint).willReturn(
        okJson(objectMapper.writeValueAsString(qualityGateConfig))
      )
    );

    return qualityGateByNameEndpoint;
  }
}
