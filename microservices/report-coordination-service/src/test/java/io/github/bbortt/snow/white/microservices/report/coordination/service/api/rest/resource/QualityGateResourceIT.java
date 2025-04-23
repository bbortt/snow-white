/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.resource;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.kafka.test.utils.KafkaTestUtils.consumerProps;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.microservices.report.coordination.service.IntegrationTest;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.client.qualitygateapi.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.QualityGateCalculationRequest;
import io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.repository.QualityGateReportRepository;
import java.time.Duration;
import java.util.UUID;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.wiremock.spring.EnableWireMock;

@Testcontainers
@EnableWireMock
@IntegrationTest
@AutoConfigureMockMvc
class QualityGateResourceIT {

  private static final String ENTITY_API_URL = "/api/rest/v1/quality-gates";
  private static final String CALCULATION_REQUEST_API_URL =
    ENTITY_API_URL + "/{qualityGateName}/calculate";

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
  private ObjectMapper objectMapper;

  @Autowired
  private QualityGateReportRepository qualityGateReportRepository;

  @Autowired
  private ReportCoordinationServiceProperties reportCoordinationServiceProperties;

  @Autowired
  private MockMvc mockMvc;

  private KafkaConsumer<String, QualityGateCalculationRequestEvent> consumer;

  @BeforeEach
  void beforeEachSetup() {
    consumer = createQualityGateCalculationRequestEventConsumer();
    consumer.subscribe(
      singletonList(
        reportCoordinationServiceProperties.getCalculationRequestTopic()
      )
    );
  }

  private KafkaConsumer<
    String,
    QualityGateCalculationRequestEvent
  > createQualityGateCalculationRequestEventConsumer() {
    var consumerProps = consumerProps(
      KAFKA_CONTAINER.getBootstrapServers(),
      getClass().getSimpleName(),
      "true"
    );

    consumerProps.put(AUTO_OFFSET_RESET_CONFIG, "earliest");

    return new KafkaConsumer<>(
      consumerProps,
      new StringDeserializer(),
      new JsonDeserializer<>(QualityGateCalculationRequestEvent.class)
    );
  }

  @AfterEach
  void afterEachTeardown() {
    consumer.close();
  }

  @Test
  void postRequest_withAllParameters_shouldTriggerABasicCoverageCalculation()
    throws Exception {
    var qualityGateByNameEndpoint = createQualityGateApiWiremockStub();

    var qualityGateCalculationRequest = QualityGateCalculationRequest.builder()
      .serviceName("star-wars")
      .apiName("yoda")
      .apiVersion("1.0.0")
      .build();

    var contentAsString = mockMvc
      .perform(
        post(CALCULATION_REQUEST_API_URL, "basic-coverage")
          .contentType(APPLICATION_JSON)
          .content(
            objectMapper.writeValueAsString(qualityGateCalculationRequest)
          )
      )
      .andExpect(status().isAccepted())
      .andReturn()
      .getResponse()
      .getContentAsString();

    var responseJson = objectMapper.readTree(contentAsString);
    String calculationIdStr = responseJson.get("calculationId").asText();

    assertThat(calculationIdStr).isNotEmpty();
    var calculationId = assertDoesNotThrow(() ->
      UUID.fromString(calculationIdStr)
    );

    assertThat(qualityGateReportRepository.findById(calculationId)).isPresent();

    assertThatKafkaEventHasBeenPublished(
      calculationIdStr,
      qualityGateCalculationRequest,
      qualityGateCalculationRequest.getLookbackWindow()
    );

    verify(getRequestedFor(urlEqualTo(qualityGateByNameEndpoint)));
  }

  @Test
  void postRequest_withRequiredParameters_shouldTriggerABasicCoverageCalculation()
    throws Exception {
    var qualityGateByNameEndpoint = createQualityGateApiWiremockStub();

    var qualityGateCalculationRequest = QualityGateCalculationRequest.builder()
      .serviceName("star-wars")
      .apiName("yoda")
      .lookbackWindow(null)
      .build();

    var contentAsString = mockMvc
      .perform(
        post(CALCULATION_REQUEST_API_URL, "basic-coverage")
          .contentType(APPLICATION_JSON)
          .content(
            objectMapper.writeValueAsString(qualityGateCalculationRequest)
          )
      )
      .andExpect(status().isAccepted())
      .andReturn()
      .getResponse()
      .getContentAsString();

    var responseJson = objectMapper.readTree(contentAsString);
    String calculationIdStr = responseJson.get("calculationId").asText();

    assertThat(calculationIdStr).isNotEmpty();
    var calculationId = assertDoesNotThrow(() ->
      UUID.fromString(calculationIdStr)
    );

    assertThat(qualityGateReportRepository.findById(calculationId)).isPresent();

    assertThatKafkaEventHasBeenPublished(
      calculationIdStr,
      qualityGateCalculationRequest,
      "1h"
    );

    verify(getRequestedFor(urlEqualTo(qualityGateByNameEndpoint)));
  }

  private @NotNull String createQualityGateApiWiremockStub()
    throws JsonProcessingException {
    var qualityGateConfig = new QualityGateConfig().name("basic-coverage");

    var qualityGateByNameEndpoint = "/api/rest/v1/quality-gates/basic-coverage";
    stubFor(
      get(qualityGateByNameEndpoint).willReturn(
        okJson(objectMapper.writeValueAsString(qualityGateConfig))
      )
    );

    return qualityGateByNameEndpoint;
  }

  private void assertThatKafkaEventHasBeenPublished(
    String calculationIdStr,
    QualityGateCalculationRequest qualityGateCalculationRequest,
    String lookbackWindow
  ) {
    await()
      .atMost(5, SECONDS)
      .untilAsserted(
        () ->
          getSingleRecord(
            consumer,
            reportCoordinationServiceProperties.getCalculationRequestTopic(),
            Duration.ofSeconds(5)
          ),
        record ->
          assertThat(record).satisfies(
            r -> assertThat(r.key()).isEqualTo(calculationIdStr),
            r ->
              assertThat(r.value()).satisfies(
                event ->
                  assertThat(event.getServiceName()).isEqualTo(
                    qualityGateCalculationRequest.getServiceName()
                  ),
                event ->
                  assertThat(event.getApiName()).isEqualTo(
                    qualityGateCalculationRequest.getApiName()
                  ),
                event ->
                  assertThat(event.getApiVersion()).isEqualTo(
                    qualityGateCalculationRequest.getApiVersion()
                  ),
                event ->
                  assertThat(event.getLookbackWindow()).isEqualTo(
                    lookbackWindow
                  )
              )
          )
      );
  }
}
