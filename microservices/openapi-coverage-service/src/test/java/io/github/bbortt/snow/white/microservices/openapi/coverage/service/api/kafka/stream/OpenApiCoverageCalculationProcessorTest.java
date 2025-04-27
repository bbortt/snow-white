/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.api.kafka.stream;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiCriterionResult;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.config.KafkaStreamsConfig;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.config.OpenApiCoverageServiceProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.OpenApiCoverageService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.OpenApiService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.exception.OpenApiNotIndexedException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.exception.UnparseableOpenApiException;
import io.swagger.v3.oas.models.OpenAPI;
import java.math.BigDecimal;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiCoverageCalculationProcessorTest {

  public static final String SERVICE_NAME = "serviceName";
  public static final String API_NAME = "apiName";
  public static final String API_VERSION = "apiVersion";
  private final String requestTopicName =
    getClass().getSimpleName() + ":request";
  private final String responseTopicName =
    getClass().getSimpleName() + ":response";

  private final Properties snowWhiteKafkaProperties = new Properties();

  @Mock
  private OpenApiCoverageService openApiCoverageServiceMock;

  @Mock
  private OpenApiService openApiServiceMock;

  private OpenApiCoverageServiceProperties openApiCoverageServiceProperties;

  private OpenApiCoverageCalculationProcessor fixture;

  @BeforeEach
  void beforeEachSetup() {
    openApiCoverageServiceProperties = new OpenApiCoverageServiceProperties();
    openApiCoverageServiceProperties.setCalculationRequestTopic(
      requestTopicName
    );
    openApiCoverageServiceProperties.setOpenapiCalculationResponseTopic(
      responseTopicName
    );

    fixture = new OpenApiCoverageCalculationProcessor(
      openApiCoverageServiceMock,
      openApiCoverageServiceProperties,
      openApiServiceMock
    );
  }

  @Nested
  class OpenapiCoverageStream {

    @Test
    void shouldReturnCoverage()
      throws OpenApiNotIndexedException, UnparseableOpenApiException {
      var openAPIMock = mock(OpenAPI.class);
      doReturn(openAPIMock)
        .when(openApiServiceMock)
        .findAndParseOpenApi(any(OpenApiService.OpenApiIdentifier.class));

      Set<OpenApiCriterionResult> openApiCriterionResults = Set.of(
        new OpenApiCriterionResult(PATH_COVERAGE, BigDecimal.ONE, null)
      );
      doReturn(openApiCriterionResults)
        .when(openApiCoverageServiceMock)
        .gatherDataAndCalculateCoverage(
          any(OpenApiService.OpenApiCoverageRequest.class)
        );

      var calculationId = "685fff79-964e-4ee8-b4d5-a4fb20465cf3";
      var lookbackWindow = "1h";

      sendEventsAndAssert(
        calculationId,
        getQualityGateCalculationRequestEvent(lookbackWindow),
        outputTopic ->
          assertThat(outputTopic.readKeyValuesToList())
            .hasSize(1)
            .first()
            .satisfies(
              r -> assertThat(r.key).isEqualTo(calculationId),
              r ->
                assertThat(r.value)
                  .isInstanceOf(OpenApiCoverageResponseEvent.class)
                  .satisfies(c ->
                    assertThat(c.openApiCriteria()).isEqualTo(
                      openApiCriterionResults
                    )
                  )
            )
      );

      verifyCoverageCalculationInvocation(openAPIMock, lookbackWindow);
    }

    @Test
    void shouldReturnEmptyCoverage()
      throws OpenApiNotIndexedException, UnparseableOpenApiException {
      var openAPIMock = mock(OpenAPI.class);
      doReturn(openAPIMock)
        .when(openApiServiceMock)
        .findAndParseOpenApi(any(OpenApiService.OpenApiIdentifier.class));

      Set<OpenApiCriterionResult> openApiCriterionResults = emptySet();
      doReturn(openApiCriterionResults)
        .when(openApiCoverageServiceMock)
        .gatherDataAndCalculateCoverage(
          any(OpenApiService.OpenApiCoverageRequest.class)
        );

      var calculationId = "31a196cd-b918-49a8-9ae0-dcf82a1f39fe";
      var lookbackWindow = "1h";

      sendEventsAndAssert(
        calculationId,
        getQualityGateCalculationRequestEvent(lookbackWindow),
        outputTopic ->
          assertThat(outputTopic.readKeyValuesToList())
            .hasSize(1)
            .first()
            .satisfies(
              r -> assertThat(r.key).isEqualTo(calculationId),
              r ->
                assertThat(r.value)
                  .isInstanceOf(OpenApiCoverageResponseEvent.class)
                  .satisfies(c -> assertThat(c.openApiCriteria()).isEmpty())
            )
      );

      verifyCoverageCalculationInvocation(openAPIMock, lookbackWindow);
    }

    @Test
    void shouldReturnEmptyStream_IfOpenapiIsNull()
      throws OpenApiNotIndexedException, UnparseableOpenApiException {
      ArgumentCaptor<
        OpenApiService.OpenApiIdentifier
      > openApiIdentifierArgumentCaptor = captor();

      doReturn(null)
        .when(openApiServiceMock)
        .findAndParseOpenApi(openApiIdentifierArgumentCaptor.capture());

      sendValidRequestAndExpectEmptyResponse(openApiIdentifierArgumentCaptor);
    }

    @Test
    void shouldReturnEmptyStream_IfOpenApiIsNotIndexed()
      throws OpenApiNotIndexedException, UnparseableOpenApiException {
      ArgumentCaptor<
        OpenApiService.OpenApiIdentifier
      > openApiIdentifierArgumentCaptor = captor();

      doThrow(OpenApiNotIndexedException.class)
        .when(openApiServiceMock)
        .findAndParseOpenApi(openApiIdentifierArgumentCaptor.capture());

      sendValidRequestAndExpectEmptyResponse(openApiIdentifierArgumentCaptor);
    }

    @Test
    void shouldReturnEmptyStream_IfOpenApiIsUnparseable()
      throws OpenApiNotIndexedException, UnparseableOpenApiException {
      ArgumentCaptor<
        OpenApiService.OpenApiIdentifier
      > openApiIdentifierArgumentCaptor = captor();

      doThrow(UnparseableOpenApiException.class)
        .when(openApiServiceMock)
        .findAndParseOpenApi(openApiIdentifierArgumentCaptor.capture());

      sendValidRequestAndExpectEmptyResponse(openApiIdentifierArgumentCaptor);
    }

    private void sendValidRequestAndExpectEmptyResponse(
      ArgumentCaptor<
        OpenApiService.OpenApiIdentifier
      > openApiIdentifierArgumentCaptor
    ) throws OpenApiNotIndexedException, UnparseableOpenApiException {
      sendEventsAndAssert(
        "981900ba-bce2-4147-99c0-c52d12ec9575",
        getQualityGateCalculationRequestEvent("1h"),
        outputTopic -> assertThat(outputTopic.readKeyValuesToList()).isEmpty()
      );

      assertThat(openApiIdentifierArgumentCaptor.getValue())
        .isNotNull()
        .satisfies(
          i -> assertThat(i.otelServiceName()).isEqualTo(SERVICE_NAME),
          i -> assertThat(i.apiName()).isEqualTo(API_NAME),
          i -> assertThat(i.apiVersion()).isEqualTo(API_VERSION)
        );

      verifyNoInteractions(openApiCoverageServiceMock);
    }

    private void sendEventsAndAssert(
      String calculationId,
      QualityGateCalculationRequestEvent qualityGateCalculationRequestEvent,
      Consumer<
        TestOutputTopic<String, OpenApiCoverageResponseEvent>
      > eventAssert
    ) {
      var streamsBuilder = new StreamsBuilder();

      fixture.openapiCoverageStream(streamsBuilder);
      var topology = streamsBuilder.build();

      try (
        var topologyTestDriver = new TopologyTestDriver(
          topology,
          snowWhiteKafkaProperties
        )
      ) {
        var inputTopic = topologyTestDriver.createInputTopic(
          requestTopicName,
          new StringSerializer(),
          KafkaStreamsConfig.QualityGateCalculationRequestEvent().serializer()
        );

        var outputTopic = topologyTestDriver.createOutputTopic(
          responseTopicName,
          new StringDeserializer(),
          KafkaStreamsConfig.OpenApiCoverageResponseEvent().deserializer()
        );

        inputTopic.pipeInput(calculationId, qualityGateCalculationRequestEvent);

        eventAssert.accept(outputTopic);
      }
    }

    private void verifyCoverageCalculationInvocation(
      OpenAPI openAPIMock,
      String lookbackWindow
    ) {
      ArgumentCaptor<
        OpenApiService.OpenApiCoverageRequest
      > openApiCoverageRequestArgumentCaptor = captor();
      verify(openApiCoverageServiceMock).gatherDataAndCalculateCoverage(
        openApiCoverageRequestArgumentCaptor.capture()
      );
      assertThat(openApiCoverageRequestArgumentCaptor.getValue())
        .isNotNull()
        .satisfies(
          r -> assertThat(r.openAPI()).isEqualTo(openAPIMock),
          r ->
            assertThat(r.openApiIdentifier())
              .isNotNull()
              .satisfies(
                i -> assertThat(i.otelServiceName()).isEqualTo(SERVICE_NAME),
                i -> assertThat(i.apiName()).isEqualTo(API_NAME),
                i -> assertThat(i.apiVersion()).isEqualTo(API_VERSION)
              ),
          r -> assertThat(r.lookbackWindow()).isEqualTo(lookbackWindow)
        );
    }
  }

  private static QualityGateCalculationRequestEvent getQualityGateCalculationRequestEvent(
    String lookbackWindow
  ) {
    return QualityGateCalculationRequestEvent.builder()
      .serviceName(SERVICE_NAME)
      .apiName(API_NAME)
      .apiVersion(API_VERSION)
      .lookbackWindow(lookbackWindow)
      .build();
  }
}
