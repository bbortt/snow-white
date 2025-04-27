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
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.PASSED;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Arrays.stream;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.report.coordination.service.IntegrationTest;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.client.qualitygateapi.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.OpenApiTestCriteria;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportParameters;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.repository.OpenApiCriterionRepository;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.repository.QualityGateReportRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
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
  private OpenApiCriterionRepository openApiCriterionRepository;

  @Autowired
  private QualityGateReportRepository qualityGateReportRepository;

  @Autowired
  private ReportCoordinationServiceProperties reportCoordinationServiceProperties;

  @BeforeEach
  void beforeEachSetup() {
    var missingOpenApiCriteria = stream(OpenApiCriteria.values())
      .map(openApiCriteria ->
        OpenApiTestCriteria.builder().name(openApiCriteria.name()).build()
      )
      .filter(openApiCriterion ->
        openApiCriterionRepository
          .findByName(openApiCriterion.getName())
          .isEmpty()
      )
      .toList();

    // This ensures that entity merging is done properly
    openApiCriterionRepository.saveAll(missingOpenApiCriteria);
  }

  @Test
  void kafkaEvent_withCoveredCriteria_shouldBePersisted()
    throws JsonProcessingException {
    var calculationId = UUID.fromString("6fa77498-a7aa-48d2-8f1d-dee93eb45780");

    var qualityGateConfigName = persistInitialQualityGateReport(calculationId);

    var openApiCriterion = PATH_COVERAGE;
    var qualityGateByNameEndpoint = createQualityGateApiWiremockStub(
      qualityGateConfigName,
      openApiCriterion
    );

    var duration = Duration.ofMillis(1234);
    kafkaTemplate.send(
      reportCoordinationServiceProperties
        .getOpenapiCalculationResponse()
        .getTopic(),
      calculationId.toString(),
      new OpenApiCoverageResponseEvent(
        Set.of(new OpenApiTestResult(openApiCriterion, ONE, duration))
      )
    );

    assertThatEntityHasBeenUpdated(
      calculationId,
      PASSED,
      ONE.setScale(2, HALF_UP),
      duration
    );

    verify(getRequestedFor(urlEqualTo(qualityGateByNameEndpoint)));
  }

  @Test
  void kafkaEvent_withUncoveredCriteria_shouldBePersisted()
    throws JsonProcessingException {
    var calculationId = UUID.fromString("dc296f73-8124-4bd3-bc09-5518bdb5be6e");

    var qualityGateConfigName = persistInitialQualityGateReport(calculationId);

    var openApiCriterion = PATH_COVERAGE;
    var qualityGateByNameEndpoint = createQualityGateApiWiremockStub(
      qualityGateConfigName,
      openApiCriterion
    );

    var duration = Duration.ofMillis(1234);
    kafkaTemplate.send(
      reportCoordinationServiceProperties
        .getOpenapiCalculationResponse()
        .getTopic(),
      calculationId.toString(),
      new OpenApiCoverageResponseEvent(
        Set.of(new OpenApiTestResult(openApiCriterion, ZERO, duration))
      )
    );

    assertThatEntityHasBeenUpdated(
      calculationId,
      FAILED,
      ZERO.setScale(2, HALF_UP),
      duration
    );

    verify(getRequestedFor(urlEqualTo(qualityGateByNameEndpoint)));
  }

  private @NotNull String persistInitialQualityGateReport(UUID calculationId) {
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
    return qualityGateConfigName;
  }

  private @NotNull String createQualityGateApiWiremockStub(
    String qualityGateConfigName,
    OpenApiCriteria openApiCriterion
  ) throws JsonProcessingException {
    var qualityGateConfig = new QualityGateConfig()
      .name(qualityGateConfigName)
      .addOpenapiCriteriaItem(openApiCriterion.name());

    var qualityGateByNameEndpoint =
      "/api/rest/v1/quality-gates/" + qualityGateConfigName;
    stubFor(
      get(qualityGateByNameEndpoint).willReturn(
        okJson(objectMapper.writeValueAsString(qualityGateConfig))
      )
    );

    return qualityGateByNameEndpoint;
  }

  private void assertThatEntityHasBeenUpdated(
    UUID calculationId,
    ReportStatus reportStatus,
    BigDecimal coverage,
    Duration duration
  ) {
    await()
      .atMost(5, SECONDS)
      .untilAsserted(
        () -> qualityGateReportRepository.findById(calculationId),
        qualityGateReport ->
          assertThat(qualityGateReport)
            .isPresent()
            .get()
            .satisfies(
              report ->
                assertThat(report.getReportStatus()).isEqualTo(reportStatus),
              report ->
                assertThat(report.getOpenApiTestResults())
                  .hasSize(1)
                  .first()
                  .satisfies(
                    openApiResult ->
                      assertThat(openApiResult.getOpenApiTestCriteria())
                        .isNotNull()
                        .extracting(OpenApiTestCriteria::getName)
                        .isEqualTo(PATH_COVERAGE.name()),
                    openApiResult ->
                      assertThat(openApiResult.getCoverage()).isEqualTo(
                        coverage
                      ),
                    openApiResult ->
                      assertThat(openApiResult.getIncludedInReport()).isTrue(),
                    openApiResult ->
                      assertThat(openApiResult.getDuration()).isEqualTo(
                        duration
                      ),
                    openApiResult ->
                      assertThat(
                        openApiResult.getAdditionalInformation()
                      ).isNull()
                  )
            )
      );
  }
}
