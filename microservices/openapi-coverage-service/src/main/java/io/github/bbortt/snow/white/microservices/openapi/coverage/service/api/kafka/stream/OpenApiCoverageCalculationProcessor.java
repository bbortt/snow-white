package io.github.bbortt.snow.white.microservices.openapi.coverage.service.api.kafka.stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.config.KafkaStreamsConfig;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.config.OpenApiCoverageServiceProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.OpenApiCoverageService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.OpenApiService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.exception.OpenApiNotIndexedException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.exception.UnparseableOpenApiException;
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

  private final OpenApiCoverageService openApiCoverageService;
  private final OpenApiCoverageServiceProperties openApiCoverageServiceProperties;
  private final OpenApiService openApiService;

  @Bean
  public KStream<
    String,
    QualityGateCalculationRequestEvent
  > openapiCoverageStream(StreamsBuilder streamsBuilder) {
    var stream = createStream(streamsBuilder);

    stream
      .peek((key, calculationRequestEvent) ->
        logger.debug("Handling message id '{}'", key)
      )
      .mapValues((key, calculationRequestEvent) -> {
        var openApiIdentifier = new OpenApiService.OpenApiIdentifier(
          calculationRequestEvent.getServiceName(),
          calculationRequestEvent.getApiName(),
          calculationRequestEvent.getApiVersion()
        );

        try {
          return new OpenApiService.OpenApiCoverageRequest(
            openApiIdentifier,
            openApiService.findAndParseOpenApi(openApiIdentifier),
            calculationRequestEvent.getLookbackWindow()
          );
        } catch (OpenApiNotIndexedException | UnparseableOpenApiException e) {
          logger.error(
            "Failed to process message with key {}: {}",
            key,
            e.getMessage(),
            e
          );

          return null;
        }
      })
      .filter(
        (key, openApiCoverageRequest) ->
          nonNull(openApiCoverageRequest) &&
          nonNull(openApiCoverageRequest.openAPI())
      )
      .mapValues((key, openApiCoverageRequest) ->
        openApiCoverageService.gatherDataAndCalculateCoverage(
          openApiCoverageRequest
        )
      )
      .flatMapValues((key, openApiCriteriaResult) ->
        singletonList(new OpenApiCoverageResponseEvent(openApiCriteriaResult))
      )
      .to(
        openApiCoverageServiceProperties.getOpenapiCalculationResponseTopic(),
        Produced.with(
          Serdes.String(),
          KafkaStreamsConfig.OpenApiCoverageResponseEvent()
        )
      );

    // TODO: Error should not go unnoticed, but be sent to "dead letter queue"

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
