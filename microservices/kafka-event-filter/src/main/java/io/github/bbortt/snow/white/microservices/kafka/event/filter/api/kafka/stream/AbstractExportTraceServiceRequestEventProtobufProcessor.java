package io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka.stream;

import static org.apache.kafka.streams.KeyValue.pair;

import io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties;
import io.github.bbortt.snow.white.microservices.kafka.event.filter.service.OtelInformationFilteringService;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;

@Slf4j
public abstract class AbstractExportTraceServiceRequestEventProtobufProcessor {

  private final OtelInformationFilteringService otelInformationFilteringService;

  private final String inboundTopicName;
  private final String outboundTopicName;

  protected AbstractExportTraceServiceRequestEventProtobufProcessor(
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
      .map((key, value) ->
        pair(
          key,
          otelInformationFilteringService.filterUnknownSpecifications(value)
        )
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
