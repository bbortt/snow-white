/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.kafka.stream;

import static java.util.Objects.nonNull;

import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config.KafkaEventFilterProperties;
import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.service.OtelInformationFilteringService;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;

@Slf4j
public abstract class AbstractExportTraceServiceRequestEventProcessor {

  private final OtelInformationFilteringService otelInformationFilteringService;

  private final String inboundTopicName;
  private final String outboundTopicName;

  protected AbstractExportTraceServiceRequestEventProcessor(
    OtelInformationFilteringService otelInformationFilteringService,
    KafkaEventFilterProperties kafkaEventFilterProperties
  ) {
    this.otelInformationFilteringService = otelInformationFilteringService;

    this.inboundTopicName = kafkaEventFilterProperties.getInboundTopicName();
    this.outboundTopicName = kafkaEventFilterProperties.getOutboundTopicName();
  }

  protected KStream<String, ExportTraceServiceRequest> resourceSpansStream(
    StreamsBuilder streamsBuilder
  ) {
    var stream = createStream(streamsBuilder, inboundTopicName);

    stream
      .peek((key, value) -> logger.debug("Handling message id '{}'", key))
      .mapValues((key, value) -> {
        try {
          return otelInformationFilteringService.filterUnknownSpecifications(
            value
          );
        } catch (Exception e) {
          logger.error(
            "Failed to process message with key {}: {}",
            key,
            e.getMessage(),
            e
          );

          return null;
        }
      })
      .filter((key, exportTraceServiceRequest) ->
        nonNull(exportTraceServiceRequest)
      )
      .filter((key, exportTraceServiceRequest) ->
        (exportTraceServiceRequest.getResourceSpansCount() > 0 &&
          exportTraceServiceRequest
            .getResourceSpansList()
            .stream()
            .anyMatch(
              resourceSpans ->
                resourceSpans.getScopeSpansCount() > 0 &&
                resourceSpans
                  .getScopeSpansList()
                  .stream()
                  .anyMatch(scopeSpans -> scopeSpans.getSpansCount() > 0)
            ))
      )
      .peek((key, value) -> logger.trace("Message '{}' passed processing", key))
      .to(
        outboundTopicName,
        Produced.with(Serdes.String(), outboundValueSerde())
      );

    return stream;
  }

  protected abstract KStream<String, ExportTraceServiceRequest> createStream(
    StreamsBuilder streamsBuilder,
    String inboundTopicName
  );

  protected abstract Serde<ExportTraceServiceRequest> outboundValueSerde();
}
