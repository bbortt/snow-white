/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.config;

import static io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties.CONSUMER_MODE_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import io.github.bbortt.snow.white.microservices.kafka.event.filter.service.OtelInformationFilteringService;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.Environment;

@ExtendWith({ MockitoExtension.class })
class ProtobufProcessorConfigTest {

  @Mock
  private Environment environmentMock;

  @Mock
  private OtelInformationFilteringService otelInformationFilteringServiceMock;

  @Mock
  private KafkaEventFilterProperties kafkaEventFilterPropertiesMock;

  private final StreamsBuilder streamsBuilder = new StreamsBuilder();

  private ProtobufProcessorConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ProtobufProcessorConfig(environmentMock);
  }

  @Nested
  class JsonProcessor {

    @Test
    void shouldBePresent_whenConsumerModeIsJson() {
      var contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(ProtobufProcessorConfig.class);

      doReturn("inboundTopicName")
        .when(kafkaEventFilterPropertiesMock)
        .getInboundTopicName();
      doReturn("outboundTopicName")
        .when(kafkaEventFilterPropertiesMock)
        .getOutboundTopicName();

      contextRunner
        .withBean(OtelInformationFilteringService.class, () ->
          otelInformationFilteringServiceMock
        )
        .withBean(KafkaEventFilterProperties.class, () ->
          kafkaEventFilterPropertiesMock
        )
        .withBean(StreamsBuilder.class, () -> streamsBuilder)
        .withPropertyValues(CONSUMER_MODE_PROPERTY_NAME + "=json")
        .run(context ->
          assertThat(context)
            .getBean("exportTraceServiceRequestJsonStream", KStream.class)
            .isNotNull()
        );
    }

    @Test
    void shouldReturnNullBean_whenConsumerModeIsProtobuf() {
      doReturn("protobuf")
        .when(environmentMock)
        .getProperty(CONSUMER_MODE_PROPERTY_NAME, "json");

      assertThat(
        fixture.exportTraceServiceRequestJsonStream(
          otelInformationFilteringServiceMock,
          kafkaEventFilterPropertiesMock,
          streamsBuilder
        )
      ).isNull();
    }
  }

  @Nested
  class ProtobufProcessor {

    @Mock
    private Serde<ExportTraceServiceRequest> protobufSerdeMock;

    @Test
    void shouldBePresent_whenConsumerModeIsJson() {
      var contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(ProtobufProcessorConfig.class);

      doReturn("inboundTopicName")
        .when(kafkaEventFilterPropertiesMock)
        .getInboundTopicName();
      doReturn("outboundTopicName")
        .when(kafkaEventFilterPropertiesMock)
        .getOutboundTopicName();

      contextRunner
        .withBean(OtelInformationFilteringService.class, () ->
          otelInformationFilteringServiceMock
        )
        .withBean(KafkaEventFilterProperties.class, () ->
          kafkaEventFilterPropertiesMock
        )
        .withBean(Serde.class, () -> protobufSerdeMock)
        .withBean(StreamsBuilder.class, () -> streamsBuilder)
        .withPropertyValues(CONSUMER_MODE_PROPERTY_NAME + "=protobuf")
        .run(context ->
          assertThat(context)
            .getBean("exportTraceServiceRequestProtobufStream", KStream.class)
            .isNotNull()
        );
    }

    @Test
    void shouldReturnNullBean_whenConsumerModeIsJson() {
      doReturn("json")
        .when(environmentMock)
        .getProperty(CONSUMER_MODE_PROPERTY_NAME, "json");

      assertThat(
        fixture.exportTraceServiceRequestProtobufStream(
          otelInformationFilteringServiceMock,
          kafkaEventFilterPropertiesMock,
          protobufSerdeMock,
          streamsBuilder
        )
      ).isNull();
    }

    @Test
    void shouldThrowException_whenNullSerdeIsSupplied() {
      doReturn("protobuf")
        .when(environmentMock)
        .getProperty(CONSUMER_MODE_PROPERTY_NAME, "json");

      assertThatThrownBy(() ->
        fixture.exportTraceServiceRequestProtobufStream(
          otelInformationFilteringServiceMock,
          kafkaEventFilterPropertiesMock,
          null,
          streamsBuilder
        )
      ).isInstanceOf(NullPointerException.class);
    }
  }
}
