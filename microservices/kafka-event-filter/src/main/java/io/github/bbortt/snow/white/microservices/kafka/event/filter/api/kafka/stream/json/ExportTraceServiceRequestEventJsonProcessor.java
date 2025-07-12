/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka.stream.json;

import static io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaStreamsConfig.JsonSerde;

import io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka.stream.AbstractExportTraceServiceRequestEventProtobufProcessor;
import io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties;
import io.github.bbortt.snow.white.microservices.kafka.event.filter.service.OtelInformationFilteringService;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.context.annotation.Bean;

@Slf4j
public class ExportTraceServiceRequestEventJsonProcessor
  extends AbstractExportTraceServiceRequestEventProtobufProcessor {

  public ExportTraceServiceRequestEventJsonProcessor(
    OtelInformationFilteringService otelInformationFilteringService,
    KafkaEventFilterProperties kafkaEventFilterProperties
  ) {
    super(otelInformationFilteringService, kafkaEventFilterProperties);
    logger.info("Enabled JSON processing mode");
  }

  @Bean
  @Override
  public KStream<String, ExportTraceServiceRequest> resourceSpansStream(
    StreamsBuilder streamsBuilder
  ) {
    return super.resourceSpansStream(streamsBuilder);
  }

  @Override
  protected KStream<String, ExportTraceServiceRequest> createStream(
    StreamsBuilder streamsBuilder,
    String inboundTopicName
  ) {
    return streamsBuilder.stream(
      inboundTopicName,
      Consumed.with(Serdes.String(), JsonSerde())
    );
  }

  @Override
  protected Serde<ExportTraceServiceRequest> outboundValueSerde() {
    return JsonSerde();
  }
}
