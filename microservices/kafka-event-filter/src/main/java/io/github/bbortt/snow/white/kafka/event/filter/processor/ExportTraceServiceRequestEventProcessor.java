package io.github.bbortt.snow.white.kafka.event.filter.processor;

import static org.apache.kafka.streams.KeyValue.pair;

import io.github.bbortt.snow.white.kafka.event.filter.config.KafkaEventFilterProperties;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ExportTraceServiceRequestEventProcessor {

  private final ExportTraceServiceRequestFilter exportTraceServiceRequestFilter;

  private final String inboundTopicName;
  private final String outboundTopicName;

  public ExportTraceServiceRequestEventProcessor(
    ExportTraceServiceRequestFilter exportTraceServiceRequestFilter,
    KafkaEventFilterProperties kafkaEventFilterProperties
  ) {
    this.exportTraceServiceRequestFilter = exportTraceServiceRequestFilter;

    this.inboundTopicName = kafkaEventFilterProperties.getInboundTopicName();
    this.outboundTopicName = kafkaEventFilterProperties.getOutboundTopicName();
  }

  @Bean
  public KStream<String, ExportTraceServiceRequest> resourceSpansStream(
    StreamsBuilder streamsBuilder,
    Serde<ExportTraceServiceRequest> exportTraceServiceRequestSerde
  ) {
    KStream<String, ExportTraceServiceRequest> stream = streamsBuilder.stream(
      inboundTopicName,
      Consumed.with(Serdes.String(), exportTraceServiceRequestSerde)
    );

    stream
      .peek((key, value) -> logger.trace("Handling message id '{}'", key))
      .map((key, value) ->
        pair(
          key,
          exportTraceServiceRequestFilter.filterUnknownSpecifications(value)
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
      .peek((key, value) -> logger.trace("Message '{}' passed processed", key))
      .to(
        outboundTopicName,
        Produced.with(Serdes.String(), exportTraceServiceRequestSerde)
      );

    return stream;
  }
}
