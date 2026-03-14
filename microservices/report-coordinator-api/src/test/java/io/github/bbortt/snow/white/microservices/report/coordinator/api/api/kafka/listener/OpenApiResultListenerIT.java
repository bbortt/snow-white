/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.kafka.listener;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.HTTP_METHOD_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.TestData.defaultApiInformation;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.TestData.defaultApiTest;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.FINISHED_EXCEPTIONALLY;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.PASSED;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.SET;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.awaitility.Awaitility.await;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.AbstractReportCoordinationServiceIT;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.qualitygateapi.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.ApiTestRepository;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.QualityGateReportRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.json.JsonMapper;

class OpenApiResultListenerIT extends AbstractReportCoordinationServiceIT {

  @Autowired
  private KafkaTemplate<
    @NonNull String,
    @NonNull OpenApiCoverageResponseEvent
  > kafkaTemplate;

  @Autowired
  private JsonMapper jsonMapper;

  @Autowired
  private ApiTestRepository apiTestRepository;

  @Autowired
  private QualityGateReportRepository qualityGateReportRepository;

  @Autowired
  private ReportCoordinationServiceProperties reportCoordinationServiceProperties;

  @BeforeEach
  void beforeEachSetup() {
    reset();
  }

  @Test
  void kafkaEvent_withCoveredCriteria_shouldBePersisted() {
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
        defaultApiInformation(),
        Set.of(new OpenApiTestResult(openApiCriterion, ONE, duration))
      )
    );

    assertThatEntityHasBeenUpdated(
      calculationId,
      openApiCriterion,
      PASSED,
      ONE.setScale(2, HALF_UP),
      duration
    );

    verify(getRequestedFor(urlEqualTo(qualityGateByNameEndpoint)));
  }

  @Test
  void kafkaEvent_withUncoveredCriteria_shouldBePersisted() {
    var calculationId = UUID.fromString("dc296f73-8124-4bd3-bc09-5518bdb5be6e");
    var qualityGateConfigName = persistInitialQualityGateReport(calculationId);

    var openApiCriterion = HTTP_METHOD_COVERAGE;
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
        defaultApiInformation(),
        Set.of(new OpenApiTestResult(openApiCriterion, ZERO, duration))
      )
    );

    assertThatEntityHasBeenUpdated(
      calculationId,
      openApiCriterion,
      FAILED,
      ZERO.setScale(2, HALF_UP),
      duration
    );

    verify(getRequestedFor(urlEqualTo(qualityGateByNameEndpoint)));
  }

  @Test
  void kafkaEvent_withException_shouldBePersisted() {
    var calculationId = UUID.fromString("0946c831-cc38-4707-b24c-46fedc7665af");
    var qualityGateConfigName = persistInitialQualityGateReport(calculationId);

    kafkaTemplate.send(
      reportCoordinationServiceProperties
        .getOpenapiCalculationResponse()
        .getTopic(),
      calculationId.toString(),
      new OpenApiCoverageResponseEvent(
        defaultApiInformation(),
        new IllegalArgumentException("Exception that should be persisted")
      )
    );

    await()
      .atMost(1, MINUTES)
      .untilAsserted(
        () -> qualityGateReportRepository.findById(calculationId),
        qualityGateReport ->
          assertThat(qualityGateReport)
            .isPresent()
            .get()
            .satisfies(
              report ->
                assertThat(report.getReportStatus()).isEqualTo(
                  FINISHED_EXCEPTIONALLY
                ),
              report ->
                assertThat(report.getApiTests())
                  .hasSize(1)
                  .first()
                  .satisfies(apiTest ->
                    assertThat(apiTest.getApiTestResults()).isEmpty()
                  )
            )
      );

    verify(
      0,
      getRequestedFor(
        urlMatching("/api/rest/v1/quality-gates/" + qualityGateConfigName)
      )
    );
  }

  private @NonNull String persistInitialQualityGateReport(UUID calculationId) {
    var qualityGateConfigName = "minimal";
    var qualityGateReport = qualityGateReportRepository.save(
      QualityGateReport.builder()
        .calculationId(calculationId)
        .qualityGateConfigName(qualityGateConfigName)
        .reportParameter(
          ReportParameter.builder()
            .calculationId(calculationId)
            .lookbackWindow("1h")
            .build()
        )
        .build()
    );

    apiTestRepository.save(
      defaultApiTest().withQualityGateReport(qualityGateReport)
    );

    return qualityGateConfigName;
  }

  private @NonNull String createQualityGateApiWiremockStub(
    String qualityGateConfigName,
    OpenApiCriteria openApiCriterion
  ) {
    var qualityGateConfig = new QualityGateConfig()
      .name(qualityGateConfigName)
      .addOpenApiCriteriaItem(openApiCriterion.name());

    var qualityGateByNameEndpoint =
      "/api/rest/v1/quality-gates/" + qualityGateConfigName;
    stubFor(
      get(qualityGateByNameEndpoint).willReturn(
        okJson(jsonMapper.writeValueAsString(qualityGateConfig))
      )
    );

    return qualityGateByNameEndpoint;
  }

  private void assertThatEntityHasBeenUpdated(
    UUID calculationId,
    OpenApiCriteria openApiCriterion,
    ReportStatus reportStatus,
    BigDecimal coverage,
    Duration duration
  ) {
    await()
      .atMost(1, MINUTES)
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
                assertThat(report.getApiTests())
                  .hasSize(1)
                  .first()
                  .extracting(ApiTest::getApiTestResults)
                  .asInstanceOf(SET)
                  .hasSize(1)
                  .first()
                  .asInstanceOf(type(ApiTestResult.class))
                  .satisfies(
                    openApiResult ->
                      assertThat(openApiResult.getApiTestCriteria())
                        .isNotNull()
                        .isEqualTo(openApiCriterion.name()),
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
