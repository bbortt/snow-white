package io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka.stream.json;

import static io.github.bbortt.snow.white.microservices.kafka.event.filter.TestData.RESOURCE_SPANS_WITHOUT_SCOPE_SPANS;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.TestData.RESOURCE_SPANS_WITHOUT_SPANS;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.TestData.RESOURCE_SPANS_WITH_ATTRIBUTES_ON_EACH_LEVEL;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.TestData.RESOURCE_SPANS_WITH_RESOURCE_ATTRIBUTES;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.TestData.RESOURCE_SPANS_WITH_SCOPE_ATTRIBUTES;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.TestData.RESOURCE_SPANS_WITH_SPAN_ATTRIBUTES;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.TestData.wrapResourceSpans;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.github.bbortt.snow.white.microservices.kafka.event.filter.TestData;
import io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties;
import io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaStreamsConfig;
import io.github.bbortt.snow.white.microservices.kafka.event.filter.service.OtelInformationFilteringService;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.kafka.common.serialization.Serde;
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

  private final String inboundTopicName =
    getClass().getSimpleName() + ":inbound";
  private final String outboundTopicName =
    getClass().getSimpleName() + ":outbound";

  private Properties snowWhiteKafkaProperties;
  private Serde<ExportTraceServiceRequest> jsonSerde;

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
    jsonSerde = kafkaStreamsConfig.jsonSerde();

    fixture = new ExportTraceServiceRequestEventJsonProcessor(
      otelInformationFilteringServiceMock,
      kafkaEventFilterProperties,
      jsonSerde
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
        RESOURCE_SPANS_WITH_RESOURCE_ATTRIBUTES,
        RESOURCE_SPANS_WITH_SCOPE_ATTRIBUTES,
        RESOURCE_SPANS_WITH_SPAN_ATTRIBUTES,
        RESOURCE_SPANS_WITH_ATTRIBUTES_ON_EACH_LEVEL
      );
    }

    @ParameterizedTest
    @MethodSource("validResourceSpans")
    void validTraceServiceRequestIsBeingForwarded(ResourceSpans resourceSpans) {
      doReturn(wrapResourceSpans(resourceSpans))
        .when(otelInformationFilteringServiceMock)
        .filterUnknownSpecifications(any(ExportTraceServiceRequest.class));

      sendEventsAndAssert(
        ExportTraceServiceRequest.getDefaultInstance(),
        outputTopic -> assertThat(outputTopic.readKeyValuesToList()).hasSize(1)
      );
    }

    @Test
    void emptyExportTraceServiceRequestIsBeingDiscarded() {
      var exportTraceServiceRequestMock = mock(ExportTraceServiceRequest.class);
      doReturn(0).when(exportTraceServiceRequestMock).getResourceSpansCount();
      doReturn(exportTraceServiceRequestMock)
        .when(otelInformationFilteringServiceMock)
        .filterUnknownSpecifications(any(ExportTraceServiceRequest.class));

      sendEventsAndAssert(
        ExportTraceServiceRequest.getDefaultInstance(),
        outputTopic -> assertThat(outputTopic.readKeyValuesToList()).isEmpty()
      );
    }

    public static Stream<ResourceSpans> resourceSpansWithoutContent() {
      return Stream.of(
        RESOURCE_SPANS_WITHOUT_SCOPE_SPANS,
        RESOURCE_SPANS_WITHOUT_SPANS
      );
    }

    @ParameterizedTest
    @MethodSource("resourceSpansWithoutContent")
    void exportTraceServiceRequestWithoutContentIsBeingDiscarded(
      ResourceSpans resourceSpans
    ) {
      doReturn(wrapResourceSpans(resourceSpans))
        .when(otelInformationFilteringServiceMock)
        .filterUnknownSpecifications(any(ExportTraceServiceRequest.class));

      sendEventsAndAssert(
        ExportTraceServiceRequest.getDefaultInstance(),
        outputTopic -> assertThat(outputTopic.readKeyValuesToList()).isEmpty()
      );
    }

    private void sendEventsAndAssert(
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
        )
      ) {
        var inputTopic = topologyTestDriver.createInputTopic(
          inboundTopicName,
          new StringSerializer(),
          jsonSerde.serializer()
        );

        var outputTopic = topologyTestDriver.createOutputTopic(
          outboundTopicName,
          new StringDeserializer(),
          jsonSerde.deserializer()
        );

        inputTopic.pipeInput(
          "eb647b01-a002-4d8a-862b-6f687636fa80",
          exportTraceServiceRequest
        );

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
      var inboundTopicName = "inboundTopicName";

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
