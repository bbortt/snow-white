/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config;

import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config.KafkaEventFilterProperties.CONSUMER_MODE_PROPERTY_NAME;
import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config.KafkaEventFilterProperties.ConsumerMode.JSON;
import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config.KafkaEventFilterProperties.ConsumerMode.PROTOBUF;
import static java.util.Objects.requireNonNull;

import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.kafka.stream.json.ExportTraceServiceRequestEventJsonProcessor;
import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.kafka.stream.protobuf.ExportTraceServiceRequestEventProtobufProcessor;
import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.service.OtelInformationFilteringService;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@RequiredArgsConstructor
public class ProtobufProcessorConfig {

  private final Environment environment;

  @Bean
  public KStream<
    String,
    ExportTraceServiceRequest
  > exportTraceServiceRequestJsonStream(
    OtelInformationFilteringService otelInformationFilteringService,
    KafkaEventFilterProperties kafkaEventFilterProperties,
    StreamsBuilder streamsBuilder
  ) {
    if (
      !JSON.equals(
        KafkaEventFilterProperties.ConsumerMode.valueOf(
          environment
            .getProperty(CONSUMER_MODE_PROPERTY_NAME, "json")
            .toUpperCase()
        )
      )
    ) {
      return null;
    }

    return new ExportTraceServiceRequestEventJsonProcessor(
      otelInformationFilteringService,
      kafkaEventFilterProperties
    ).resourceSpansStream(streamsBuilder);
  }

  @Bean
  public KStream<
    String,
    ExportTraceServiceRequest
  > exportTraceServiceRequestProtobufStream(
    OtelInformationFilteringService otelInformationFilteringService,
    KafkaEventFilterProperties kafkaEventFilterProperties,
    @Autowired(required = false) @Nullable Serde<
      ExportTraceServiceRequest
    > protobufSerde,
    StreamsBuilder streamsBuilder
  ) {
    if (
      !PROTOBUF.equals(
        KafkaEventFilterProperties.ConsumerMode.valueOf(
          environment
            .getProperty(CONSUMER_MODE_PROPERTY_NAME, "json")
            .toUpperCase()
        )
      )
    ) {
      return null;
    }

    requireNonNull(protobufSerde);

    return new ExportTraceServiceRequestEventProtobufProcessor(
      otelInformationFilteringService,
      kafkaEventFilterProperties,
      protobufSerde
    ).resourceSpansStream(streamsBuilder);
  }
}
