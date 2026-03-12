/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka.stream;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.TestData.API_NAME;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.TestData.API_VERSION;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.TestData.SERVICE_NAME;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.TestData.defaultApiInformation;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.TestData.qualityGateCalculationRequestEvent;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.quality.gate.ApiType;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka.serialization.QualityGateCalculationEventSerdes;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenApiCoverageCalculationService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenApiTestContext;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.OpenApiNotIndexedException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.UnparseableOpenApiException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.test.TestRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
  private OpenApiCoverageCalculationService openApiCoverageCalculationServiceMock;

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
      openApiCoverageServiceProperties,
      openApiCoverageCalculationServiceMock
    );
  }

  @Nested
  class OpenapiCoverageStreamTest {

    public static final byte[] TRACEPARENT_HEADER_BYTES =
      "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01".getBytes(
        StandardCharsets.UTF_8
      );

    @Test
    void shouldProcessCoverageRequest()
      throws OpenApiNotIndexedException, UnparseableOpenApiException {
      var calculationId = "685fff79-964e-4ee8-b4d5-a4fb20465cf3";
      var requestEvent = qualityGateCalculationRequestEvent();

      var enrichedContext = mock(OpenApiTestContext.class);
      var calculatedContext = mock(OpenApiTestContext.class);
      var responseEvent = new OpenApiCoverageResponseEvent(
        ApiInformation.builder()
          .serviceName(SERVICE_NAME)
          .apiName(API_NAME)
          .apiVersion(API_VERSION)
          .apiType(OPENAPI)
          .build(),
        emptySet()
      );

      var openApiTestContext = mock(OpenApiTestContext.class);
      doReturn(openApiTestContext)
        .when(openApiCoverageCalculationServiceMock)
        .fetchOpenApiSpecification(calculationId, requestEvent);
      doReturn(enrichedContext)
        .when(openApiCoverageCalculationServiceMock)
        .enrichWithOpenTelemetryData(eq(openApiTestContext), anyLong());
      doReturn(calculatedContext)
        .when(openApiCoverageCalculationServiceMock)
        .calculateCoverage(enrichedContext);
      doReturn(responseEvent)
        .when(openApiCoverageCalculationServiceMock)
        .buildResponseEvent(calculatedContext);

      sendEventsAndAssert(calculationId, requestEvent, outputTopic ->
        assertThat(outputTopic.readRecordsToList())
          .hasSize(1)
          .first()
          .satisfies(
            r ->
              assertThat(r.getHeaders()).containsExactly(
                new RecordHeader("traceparent", TRACEPARENT_HEADER_BYTES)
              ),
            r -> assertThat(r.getKey()).isEqualTo(calculationId),
            r -> assertThat(r.value()).isEqualTo(responseEvent)
          )
      );

      verify(openApiCoverageCalculationServiceMock).fetchOpenApiSpecification(
        calculationId,
        requestEvent
      );
      verify(openApiCoverageCalculationServiceMock).enrichWithOpenTelemetryData(
        eq(openApiTestContext),
        anyLong()
      );
      verify(openApiCoverageCalculationServiceMock).calculateCoverage(
        enrichedContext
      );
      verify(openApiCoverageCalculationServiceMock).buildResponseEvent(
        calculatedContext
      );
    }

    @ParameterizedTest
    @EnumSource(value = ApiType.class, names = { "OPENAPI" }, mode = EXCLUDE)
    void shouldFilterOutEventsThatDontCoverOpenapi(ApiType apiType) {
      var calculationId = "bc1eed1a-251f-4c3b-98b7-5f1c5a3ee45c";
      var requestEvent = qualityGateCalculationRequestEvent(apiType);

      sendEventsAndAssert(calculationId, requestEvent, outputTopic ->
        assertThat(outputTopic.readKeyValuesToList()).isEmpty()
      );

      verifyNoMoreInteractions(openApiCoverageCalculationServiceMock);
    }

    @Test
    void shouldRespondWithOpenApiNotIndexedException()
      throws OpenApiNotIndexedException, UnparseableOpenApiException {
      var cause = new OpenApiNotIndexedException(defaultApiInformation());
      assertThatEventIsBeingRespondedWithException(() -> cause, cause);
    }

    @Test
    void shouldRespondWithUnparseableOpenApiException()
      throws OpenApiNotIndexedException, UnparseableOpenApiException {
      var cause = new UnparseableOpenApiException(
        singletonList("Error message")
      );
      assertThatEventIsBeingRespondedWithException(() -> cause, cause);
    }

    @Test
    void shouldRespondWithRootCauseOnAnyException()
      throws OpenApiNotIndexedException, UnparseableOpenApiException {
      var cause = new IllegalArgumentException("Something nasty happened");

      assertThatEventIsBeingRespondedWithException(
        () -> new IllegalArgumentException(cause),
        cause
      );
    }

    private void assertThatEventIsBeingRespondedWithException(
      Supplier<Exception> exceptionSupplier,
      Exception cause
    ) throws OpenApiNotIndexedException, UnparseableOpenApiException {
      var calculationId = "e872b8e9-bca6-406f-9a7e-728017171138";
      var requestEvent = qualityGateCalculationRequestEvent();

      doThrow(exceptionSupplier.get())
        .when(openApiCoverageCalculationServiceMock)
        .fetchOpenApiSpecification(calculationId, requestEvent);

      sendEventsAndAssert(calculationId, requestEvent, outputTopic ->
        assertThat(outputTopic.readRecordsToList())
          .hasSize(1)
          .first()
          .satisfies(
            r ->
              assertThat(r.getHeaders()).containsExactly(
                new RecordHeader("traceparent", TRACEPARENT_HEADER_BYTES)
              ),
            r -> assertThat(r.getKey()).isEqualTo(calculationId),
            r -> assertThat(r.value()).isNotNull(),
            r ->
              assertThat(r.value().exception()).satisfies(
                e -> assertThat(e).isNotNull(),
                e -> assertThat(e).message().isEqualTo(cause.getMessage())
              )
          )
      );

      verify(openApiCoverageCalculationServiceMock).fetchOpenApiSpecification(
        calculationId,
        requestEvent
      );
      verifyNoMoreInteractions(openApiCoverageCalculationServiceMock);
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
          QualityGateCalculationEventSerdes.QualityGateCalculationRequestEvent().serializer()
        );

        var outputTopic = topologyTestDriver.createOutputTopic(
          responseTopicName,
          new StringDeserializer(),
          QualityGateCalculationEventSerdes.OpenApiCoverageResponseEvent().deserializer()
        );

        var testRecord = new TestRecord<>(
          calculationId,
          qualityGateCalculationRequestEvent
        );
        testRecord.getHeaders().add("traceparent", TRACEPARENT_HEADER_BYTES);

        inputTopic.pipeInput(testRecord);

        eventAssert.accept(outputTopic);
      }
    }
  }
}
