/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka.stream;

import static io.github.bbortt.snow.white.commons.event.dto.AttributeFilter.attributeFilters;
import static io.github.bbortt.snow.white.commons.event.dto.AttributeFilterOperator.STRING_EQUALS;
import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.TestData.API_NAME;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.TestData.API_VERSION;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.TestData.LOOKBACK_WINDOW;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.TestData.SERVICE_NAME;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.TestData.qualityGateCalculationRequestEvent;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka.serialization.QualityGateCalculationEventSerdes;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenApiCoverageService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenApiService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenTelemetryService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenApiTestContext;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.OpenApiNotIndexedException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.UnparseableOpenApiException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.influxdb.FluxAttributeFilter;
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
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiCoverageCalculationProcessorTest {

  private final String requestTopicName =
    getClass().getSimpleName() + ":request";
  private final String responseTopicName =
    getClass().getSimpleName() + ":response";

  private final Properties snowWhiteKafkaProperties = new Properties();

  @Mock
  private OpenApiCoverageService openApiCoverageServiceMock;

  @Mock
  private OpenApiService openApiServiceMock;

  @Mock
  private OpenTelemetryService openTelemetryService;

  private OpenApiCoverageCalculationProcessor fixture;

  @BeforeEach
  void beforeEachSetup() {
    var openApiCoverageServiceProperties =
      new OpenApiCoverageStreamProperties();
    openApiCoverageServiceProperties.setCalculationRequestTopic(
      requestTopicName
    );
    openApiCoverageServiceProperties.setOpenapiCalculationResponseTopic(
      responseTopicName
    );

    fixture = new OpenApiCoverageCalculationProcessor(
      openApiCoverageServiceMock,
      openApiCoverageServiceProperties,
      openApiServiceMock,
      openTelemetryService
    );
  }

  @Nested
  class OpenapiCoverageStream {

    @Test
    void shouldReturnCoverage_withUnmappedAttributeFilters()
      throws OpenApiNotIndexedException, UnparseableOpenApiException {
      var openAPIMock = getOpenAPIMock();
      var openTelemetryData = getMockedOpenTelemetryData();
      var openApiTestResults = getOpenApiTestResults(
        Set.of(new OpenApiTestResult(PATH_COVERAGE, BigDecimal.ONE, null))
      );

      var calculationId = "685fff79-964e-4ee8-b4d5-a4fb20465cf3";

      sendEventsAndAssert(
        calculationId,
        qualityGateCalculationRequestEvent(),
        outputTopic ->
          assertThat(outputTopic.readKeyValuesToList())
            .hasSize(1)
            .first()
            .satisfies(
              r -> assertThat(r.key).isEqualTo(calculationId),
              r ->
                assertThat(r.value)
                  .isInstanceOf(OpenApiCoverageResponseEvent.class)
                  .satisfies(
                    responseEvent ->
                      assertThat(responseEvent.getApiType()).isEqualTo(OPENAPI),
                    responseEvent ->
                      assertThat(responseEvent.apiInformation()).satisfies(
                        openApiInformation ->
                          assertThat(
                            openApiInformation.getServiceName()
                          ).isEqualTo(SERVICE_NAME),
                        openApiInformation ->
                          assertThat(openApiInformation.getApiName()).isEqualTo(
                            API_NAME
                          ),
                        openApiInformation ->
                          assertThat(
                            openApiInformation.getApiVersion()
                          ).isEqualTo(API_VERSION)
                      ),
                    responseEvent ->
                      assertThat(responseEvent.openApiCriteria()).isEqualTo(
                        openApiTestResults
                      )
                  )
            )
      );

      assertThatOpenApiHasBeenTestedWithTelemetryData(
        openAPIMock,
        openTelemetryData
      );
    }

    @Test
    void shouldReturnCoverage_withMappedAttributeFilters()
      throws OpenApiNotIndexedException, UnparseableOpenApiException {
      var calculationId = "bc1eed1a-251f-4c3b-98b7-5f1c5a3ee45c";

      var attributeFilter = new AttributeFilter("key", STRING_EQUALS, "value");
      var attributeFilters = attributeFilters().with(attributeFilter).build();

      var openAPIMock = getOpenAPIMock();
      var openTelemetryData = getMockedOpenTelemetryData();
      var openApiTestResults = getOpenApiTestResults(
        Set.of(new OpenApiTestResult(PATH_COVERAGE, BigDecimal.ONE, null))
      );

      sendEventsAndAssert(
        calculationId,
        qualityGateCalculationRequestEvent().withAttributeFilters(
          attributeFilters
        ),
        outputTopic ->
          assertThat(outputTopic.readKeyValuesToList())
            .hasSize(1)
            .first()
            .satisfies(
              r -> assertThat(r.key).isEqualTo(calculationId),
              r ->
                assertThat(r.value)
                  .isInstanceOf(OpenApiCoverageResponseEvent.class)
                  .satisfies(
                    responseEvent ->
                      assertThat(responseEvent.getApiType()).isEqualTo(OPENAPI),
                    responseEvent ->
                      assertThat(responseEvent.apiInformation()).satisfies(
                        openApiInformation ->
                          assertThat(
                            openApiInformation.getServiceName()
                          ).isEqualTo(SERVICE_NAME),
                        openApiInformation ->
                          assertThat(openApiInformation.getApiName()).isEqualTo(
                            API_NAME
                          ),
                        openApiInformation ->
                          assertThat(
                            openApiInformation.getApiVersion()
                          ).isEqualTo(API_VERSION)
                      ),
                    responseEvent ->
                      assertThat(responseEvent.openApiCriteria()).isEqualTo(
                        openApiTestResults
                      )
                  )
            )
      );

      var fluxAttributeFiltersArgumentCaptor =
        assertThatOpenApiHasBeenTestedWithTelemetryData(
          openAPIMock,
          openTelemetryData
        );

      assertThat(fluxAttributeFiltersArgumentCaptor.getValue())
        .hasSize(1)
        .first()
        .extracting("baseAttributeFilter")
        .isEqualTo(attributeFilter);
    }

    @Test
    void shouldReturnEmptyCoverage()
      throws OpenApiNotIndexedException, UnparseableOpenApiException {
      var openAPIMock = getOpenAPIMock();
      Set<OpenTelemetryData> openTelemetryData = getMockedOpenTelemetryData();
      getOpenApiTestResults(emptySet());

      var calculationId = "31a196cd-b918-49a8-9ae0-dcf82a1f39fe";

      sendEventsAndAssert(
        calculationId,
        qualityGateCalculationRequestEvent(),
        outputTopic ->
          assertThat(outputTopic.readKeyValuesToList())
            .hasSize(1)
            .first()
            .satisfies(
              r -> assertThat(r.key).isEqualTo(calculationId),
              r ->
                assertThat(r.value)
                  .isInstanceOf(OpenApiCoverageResponseEvent.class)
                  .satisfies(responseEvent ->
                    assertThat(responseEvent.openApiCriteria()).isEmpty()
                  )
            )
      );

      assertThatOpenApiHasBeenTestedWithTelemetryData(
        openAPIMock,
        openTelemetryData
      );
    }

    @Test
    void shouldReturnEmptyStream_IfOpenapiIsNull()
      throws OpenApiNotIndexedException, UnparseableOpenApiException {
      ArgumentCaptor<ApiInformation> apiInformationArgumentCaptor = captor();

      doReturn(null)
        .when(openApiServiceMock)
        .findAndParseOpenApi(apiInformationArgumentCaptor.capture());

      sendValidRequestAndExpectEmptyResponse(apiInformationArgumentCaptor);
    }

    @Test
    void shouldReturnEmptyStream_IfOpenApiIsNotIndexed()
      throws OpenApiNotIndexedException, UnparseableOpenApiException {
      ArgumentCaptor<ApiInformation> apiInformationArgumentCaptor = captor();

      doThrow(OpenApiNotIndexedException.class)
        .when(openApiServiceMock)
        .findAndParseOpenApi(apiInformationArgumentCaptor.capture());

      sendValidRequestAndExpectEmptyResponse(apiInformationArgumentCaptor);
    }

    @Test
    void shouldReturnEmptyStream_IfOpenApiIsUnparseable()
      throws OpenApiNotIndexedException, UnparseableOpenApiException {
      ArgumentCaptor<ApiInformation> apiInformationArgumentCaptor = captor();

      doThrow(UnparseableOpenApiException.class)
        .when(openApiServiceMock)
        .findAndParseOpenApi(apiInformationArgumentCaptor.capture());

      sendValidRequestAndExpectEmptyResponse(apiInformationArgumentCaptor);
    }

    private void sendValidRequestAndExpectEmptyResponse(
      ArgumentCaptor<ApiInformation> apiInformationArgumentCaptor
    ) {
      sendEventsAndAssert(
        "981900ba-bce2-4147-99c0-c52d12ec9575",
        qualityGateCalculationRequestEvent(),
        outputTopic -> assertThat(outputTopic.readKeyValuesToList()).isEmpty()
      );

      assertThat(apiInformationArgumentCaptor.getValue())
        .isNotNull()
        .satisfies(
          i -> assertThat(i.getServiceName()).isEqualTo(SERVICE_NAME),
          i -> assertThat(i.getApiName()).isEqualTo(API_NAME),
          i -> assertThat(i.getApiVersion()).isEqualTo(API_VERSION)
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
        );
      ) {
        var inputTopic = topologyTestDriver.createInputTopic(
          requestTopicName,
          new StringSerializer(),
          QualityGateCalculationEventSerdes.QualityGateCalculationRequestEvent().serializer()
        );

        var outputTopic = topologyTestDriver.createOutputTopic(
          responseTopicName,
          new StringDeserializer(),
          QualityGateCalculationEventSerdes.OpenApiCoverageResponseEvent().deserializer()
        );

        inputTopic.pipeInput(calculationId, qualityGateCalculationRequestEvent);

        eventAssert.accept(outputTopic);
      }
    }

    private ArgumentCaptor<
      Set<FluxAttributeFilter>
    > assertThatOpenApiHasBeenTestedWithTelemetryData(
      OpenAPI openAPIMock,
      Set<OpenTelemetryData> openTelemetryData
    ) {
      ArgumentCaptor<OpenApiTestContext> openApiTestContextArgumentCaptor =
        captor();

      verify(openApiCoverageServiceMock).testOpenApi(
        openApiTestContextArgumentCaptor.capture()
      );

      assertThat(openApiTestContextArgumentCaptor.getValue())
        .isNotNull()
        .satisfies(
          r -> assertThat(r.openAPI()).isEqualTo(openAPIMock),
          r -> assertThat(r.openTelemetryData()).isEqualTo(openTelemetryData)
        );

      ArgumentCaptor<ApiInformation> apiInformationArgumentCaptor = captor();
      ArgumentCaptor<
        Set<FluxAttributeFilter>
      > fluxAttributeFiltersArgumentCaptor = captor();

      verify(openTelemetryService).findOpenTelemetryTracingData(
        apiInformationArgumentCaptor.capture(),
        anyLong(),
        eq(LOOKBACK_WINDOW),
        fluxAttributeFiltersArgumentCaptor.capture()
      );

      assertThat(apiInformationArgumentCaptor.getValue())
        .isNotNull()
        .satisfies(
          apiInformation ->
            assertThat(apiInformation.getServiceName()).isEqualTo(SERVICE_NAME),
          apiInformation ->
            assertThat(apiInformation.getApiName()).isEqualTo(API_NAME),
          apiInformation ->
            assertThat(apiInformation.getApiVersion()).isEqualTo(API_VERSION)
        );

      return fluxAttributeFiltersArgumentCaptor;
    }
  }

  private @NonNull Set<OpenApiTestResult> getOpenApiTestResults(
    Set<OpenApiTestResult> openApiTestResults
  ) {
    doReturn(openApiTestResults)
      .when(openApiCoverageServiceMock)
      .testOpenApi(any(OpenApiTestContext.class));
    return openApiTestResults;
  }

  private @NonNull OpenAPI getOpenAPIMock()
    throws OpenApiNotIndexedException, UnparseableOpenApiException {
    var openAPIMock = mock(OpenAPI.class);
    doReturn(openAPIMock)
      .when(openApiServiceMock)
      .findAndParseOpenApi(any(ApiInformation.class));
    return openAPIMock;
  }

  private @NonNull Set<OpenTelemetryData> getMockedOpenTelemetryData() {
    Set<OpenTelemetryData> openTelemetryData = Set.of(
      new OpenTelemetryData("spanId", "traceId", null)
    );
    doReturn(openTelemetryData)
      .when(openTelemetryService)
      .findOpenTelemetryTracingData(
        any(ApiInformation.class),
        anyLong(),
        eq(LOOKBACK_WINDOW),
        anySet()
      );
    return openTelemetryData;
  }
}
