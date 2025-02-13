package io.github.bbortt.snow.white.kafka.microservices.event.filter.processor.json;

import static io.github.bbortt.snow.white.kafka.microservices.event.filter.config.KafkaEventFilterProperties.CONSUMER_MODE_PROPERTY_NAME;

import io.github.bbortt.snow.white.kafka.microservices.event.filter.config.KafkaEventFilterProperties;
import io.github.bbortt.snow.white.kafka.microservices.event.filter.filter.ExportTraceServiceRequestFilter;
import io.github.bbortt.snow.white.kafka.microservices.event.filter.processor.AbstractExportTraceServiceRequestEventProtobufProcessor;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = CONSUMER_MODE_PROPERTY_NAME, havingValue = "json")
public class ExportTraceServiceRequestEventJsonProcessor
  extends AbstractExportTraceServiceRequestEventProtobufProcessor {

  public final Serde<ExportTraceServiceRequest> jsonSerde;

  public ExportTraceServiceRequestEventJsonProcessor(
    ExportTraceServiceRequestFilter exportTraceServiceRequestFilter,
    KafkaEventFilterProperties kafkaEventFilterProperties,
    Serde<ExportTraceServiceRequest> jsonSerde
  ) {
    super(exportTraceServiceRequestFilter, kafkaEventFilterProperties);
    this.jsonSerde = jsonSerde;

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
      Consumed.with(Serdes.String(), jsonSerde)
    );
  }

  @Override
  protected Serde<ExportTraceServiceRequest> outboundValueSerde() {
    return jsonSerde;
  }
}
