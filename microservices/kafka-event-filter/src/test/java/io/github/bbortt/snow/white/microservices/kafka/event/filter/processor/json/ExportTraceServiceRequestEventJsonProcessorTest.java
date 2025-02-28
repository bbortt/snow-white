package io.github.bbortt.snow.white.microservices.kafka.event.filter.processor.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties;
import io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaStreamsConfig;
import io.github.bbortt.snow.white.microservices.kafka.event.filter.filter.ExportTraceServiceRequestFilter;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class ExportTraceServiceRequestEventJsonProcessorTest {

  private final String inboundTopicName =
    getClass().getSimpleName() + ":inbound";
  private final String outboundTopicName =
    getClass().getSimpleName() + ":outbound";

  private Serde<ExportTraceServiceRequest> jsonSerde;

  @Mock
  private ExportTraceServiceRequestFilter exportTraceServiceRequestFilterMock;

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
    jsonSerde = kafkaStreamsConfig.jsonSerde();

    fixture = new ExportTraceServiceRequestEventJsonProcessor(
      exportTraceServiceRequestFilterMock,
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

    @Test
    void isNotNull() {
      assertThat(fixture.resourceSpansStream(new StreamsBuilder())).isNotNull();
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
