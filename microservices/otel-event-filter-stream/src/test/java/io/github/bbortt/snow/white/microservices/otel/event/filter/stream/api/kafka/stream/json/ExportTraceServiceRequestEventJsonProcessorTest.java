/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.kafka.stream.json;

import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.TestData.wrapResourceSpans;
import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.kafka.serialization.ExportTraceServiceRequestSerdes.JsonSerde;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.TestData;
import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config.KafkaEventFilterProperties;
import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config.KafkaStreamsConfig;
import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.service.OtelInformationFilteringService;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class ExportTraceServiceRequestEventJsonProcessorTest {

  private static final TestData TEST_DATA = TestData.builder().build();

  private final String inboundTopicName =
    getClass().getSimpleName() + ":inbound";
  private final String outboundTopicName =
    getClass().getSimpleName() + ":outbound";

  private Properties snowWhiteKafkaProperties;

  @Mock
  private OtelInformationFilteringService otelInformationFilteringServiceMock;

  private ExportTraceServiceRequestEventJsonProcessor fixture;

  @BeforeEach
  void beforeEachSetup() {
    var kafkaEventFilterProperties = new KafkaEventFilterProperties();
    kafkaEventFilterProperties.setSchemaRegistryUrl(
      "mock://" + getClass().getSimpleName()
    );
    kafkaEventFilterProperties.setInboundTopicName(inboundTopicName);
    kafkaEventFilterProperties.setOutboundTopicName(outboundTopicName);

    var kafkaStreamsConfig = new KafkaStreamsConfig();
    snowWhiteKafkaProperties = kafkaStreamsConfig.snowWhiteKafkaProperties(
      kafkaEventFilterProperties
    );

    fixture = new ExportTraceServiceRequestEventJsonProcessor(
      otelInformationFilteringServiceMock,
      kafkaEventFilterProperties
    );
  }

  @Test
  void constructor() {
    assertThat(fixture).hasNoNullFieldsOrProperties();
  }

  @Nested
  class ResourceSpansStream {

    public static Stream<ResourceSpans> validResourceSpans() {
      return Stream.of(
        TEST_DATA.resourceSpansWithResourceAttributes(),
        TEST_DATA.resourceSpansWithScopeAttributes(),
        TEST_DATA.resourceSpansWithSpanAttributes(),
        TEST_DATA.resourceSpansWithAttributesOnEachLevel()
      );
    }

    @ParameterizedTest
    @MethodSource("validResourceSpans")
    void streamShouldForwardValidTraceServiceRequest(
      ResourceSpans resourceSpans
    ) {
      var exportTraceServiceRequest = wrapResourceSpans(resourceSpans);
      doReturn(exportTraceServiceRequest)
        .when(otelInformationFilteringServiceMock)
        .filterUnknownSpecifications(any(ExportTraceServiceRequest.class));

      var requestId = "eb647b01-a002-4d8a-862b-6f687636fa80";

      sendEventsAndAssert(
        requestId,
        ExportTraceServiceRequest.getDefaultInstance(),
        outputTopic ->
          assertThat(outputTopic.readKeyValuesToList())
            .hasSize(1)
            .first()
            .satisfies(
              r -> assertThat(r.key).isEqualTo(requestId),
              r -> assertThat(r.value).isEqualTo(exportTraceServiceRequest)
            )
      );
    }

    @Test
    void streamShouldDiscardEmptyExportTraceServiceRequest() {
      var exportTraceServiceRequestMock = mock(ExportTraceServiceRequest.class);
      doReturn(0).when(exportTraceServiceRequestMock).getResourceSpansCount();
      doReturn(exportTraceServiceRequestMock)
        .when(otelInformationFilteringServiceMock)
        .filterUnknownSpecifications(any(ExportTraceServiceRequest.class));

      sendEventsAndAssert(
        "1e355574-46c2-4a17-a58e-55efc38f84a2",
        ExportTraceServiceRequest.getDefaultInstance(),
        outputTopic -> assertThat(outputTopic.readKeyValuesToList()).isEmpty()
      );
    }

    public static Stream<ResourceSpans> resourceSpansWithoutContent() {
      return Stream.of(
        TEST_DATA.resourceSpansWithoutScopeSpans(),
        TEST_DATA.resourceSpansWithoutSpans()
      );
    }

    @ParameterizedTest
    @MethodSource("resourceSpansWithoutContent")
    void streamShouldDiscardExportTraceServiceRequestWithoutContent(
      ResourceSpans resourceSpans
    ) {
      doReturn(wrapResourceSpans(resourceSpans))
        .when(otelInformationFilteringServiceMock)
        .filterUnknownSpecifications(any(ExportTraceServiceRequest.class));

      sendEventsAndAssert(
        "81446e59-8de1-4276-a970-a960323c5208",
        ExportTraceServiceRequest.getDefaultInstance(),
        outputTopic -> assertThat(outputTopic.readKeyValuesToList()).isEmpty()
      );
    }

    @Test
    void streamShouldBeResilientAgainstServiceFailure() {
      doThrow(IllegalArgumentException.class)
        .when(otelInformationFilteringServiceMock)
        .filterUnknownSpecifications(any(ExportTraceServiceRequest.class));

      sendEventsAndAssert(
        "a73e2be9-0600-4ff4-980a-9e465114aa21",
        ExportTraceServiceRequest.getDefaultInstance(),
        outputTopic -> assertThat(outputTopic.readKeyValuesToList()).isEmpty()
      );
    }

    private void sendEventsAndAssert(
      String requestId,
      ExportTraceServiceRequest exportTraceServiceRequest,
      Consumer<TestOutputTopic<String, ExportTraceServiceRequest>> eventAssert
    ) {
      var streamsBuilder = new StreamsBuilder();

      fixture.resourceSpansStream(streamsBuilder);
      var topology = streamsBuilder.build();

      try (
        var topologyTestDriver = new TopologyTestDriver(
          topology,
          snowWhiteKafkaProperties
        );
      ) {
        var inputTopic = topologyTestDriver.createInputTopic(
          inboundTopicName,
          new StringSerializer(),
          JsonSerde().serializer()
        );

        var outputTopic = topologyTestDriver.createOutputTopic(
          outboundTopicName,
          new StringDeserializer(),
          JsonSerde().deserializer()
        );

        inputTopic.pipeInput(requestId, exportTraceServiceRequest);

        eventAssert.accept(outputTopic);
      }
    }
  }

  @Nested
  class CreateStream {

    @Mock
    private StreamsBuilder streamsBuilderMock;

    @Test
    void isNotNull() {
      KStream<String, ExportTraceServiceRequest> kStreamMock = mock();
      doReturn(kStreamMock)
        .when(streamsBuilderMock)
        .stream(eq(inboundTopicName), any(Consumed.class));

      KStream<String, ExportTraceServiceRequest> kStream = fixture.createStream(
        streamsBuilderMock,
        inboundTopicName
      );

      assertThat(kStream).isEqualTo(kStreamMock);
    }
  }

  @Nested
  class OutboundValueSerde {

    @Test
    void isNotNull() {
      assertThat(fixture.outboundValueSerde()).isNotNull();
    }
  }
}
