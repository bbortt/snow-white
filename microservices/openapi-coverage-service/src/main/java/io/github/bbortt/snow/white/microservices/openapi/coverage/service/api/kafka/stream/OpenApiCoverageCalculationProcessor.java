package io.github.bbortt.snow.white.microservices.openapi.coverage.service.api.kafka.stream;

import static org.apache.kafka.streams.KeyValue.pair;

import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.config.KafkaStreamsConfig;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.config.OpenApiCoverageServiceProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.OpenApiCoverageService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto.mapper.OpenApiCoverageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenApiCoverageCalculationProcessor {

  private final OpenApiCoverageMapper openApiCoverageMapper;
  private final OpenApiCoverageService openApiCoverageService;
  private final OpenApiCoverageServiceProperties openApiCoverageServiceProperties;

  @Bean
  public KStream<
    String,
    QualityGateCalculationRequestEvent
  > resourceSpansStream(StreamsBuilder streamsBuilder) {
    var stream = createStream(streamsBuilder);

    stream
      .peek((key, value) -> logger.debug("Handling message id '{}'", key))
      .map((key, value) ->
        pair(
          key,
          openApiCoverageService.gatherDataAndCalculateCoverage(
            value.getServiceName(),
            value.getApiName(),
            value.getApiVersion()
          )
        )
      )
      .map((key, value) ->
        pair(key, openApiCoverageMapper.toResponseEvent(value))
      )
      .to(
        openApiCoverageServiceProperties.getOpenapiCalculationResponseTopic(),
        Produced.with(
          Serdes.String(),
          KafkaStreamsConfig.OpenApiCoverageResponseEvent()
        )
      );

    return stream;
  }

  private KStream<String, QualityGateCalculationRequestEvent> createStream(
    StreamsBuilder streamsBuilder
  ) {
    return streamsBuilder.stream(
      openApiCoverageServiceProperties.getCalculationRequestTopic(),
      Consumed.with(
        Serdes.String(),
        KafkaStreamsConfig.QualityGateCalculationRequestEvent()
      )
    );
  }
}
