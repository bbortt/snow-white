/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka.stream;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka.stream.processor.TracingProcessor.newTracingProcessor;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka.serialization.QualityGateCalculationEventSerdes;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenApiCoverageCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Kafka Streams processor for OpenAPI coverage calculation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenApiCoverageCalculationProcessor {

  private final OpenApiCoverageStreamProperties openApiCoverageStreamProperties;
  private final OpenApiCoverageCalculationService openApiCoverageCalculationService;

  @Bean
  public KStream<
    String,
    QualityGateCalculationRequestEvent
  > openapiCoverageStream(StreamsBuilder streamsBuilder) {
    var stream = createStream(streamsBuilder);

    stream
      .peek((key, _) -> logger.debug("Handling message id '{}'", key))
      .processValues(
        newTracingProcessor(
          (qualityGateCalculationRequestEventFixedKeyRecord, timestamp) -> {
            var openApiTestContext =
              openApiCoverageCalculationService.fetchOpenApiSpecification(
                qualityGateCalculationRequestEventFixedKeyRecord.key(),
                qualityGateCalculationRequestEventFixedKeyRecord.value()
              );

            if (
              isNull(openApiTestContext) || isNull(openApiTestContext.openAPI())
            ) {
              return null;
            }

            openApiTestContext =
              openApiCoverageCalculationService.enrichWithOpenTelemetryData(
                openApiTestContext,
                timestamp
              );

            openApiTestContext =
              openApiCoverageCalculationService.calculateCoverage(
                openApiTestContext
              );

            return openApiCoverageCalculationService.buildResponseEvent(
              openApiTestContext
            );
          }
        )
      )
      .filter((key, openApiCoverageResponse) ->
        nonNull(openApiCoverageResponse)
      )
      .to(
        openApiCoverageStreamProperties.getOpenapiCalculationResponseTopic(),
        Produced.with(
          Serdes.String(),
          QualityGateCalculationEventSerdes.OpenApiCoverageResponseEvent()
        )
      );

    return stream;
  }

  private KStream<String, QualityGateCalculationRequestEvent> createStream(
    StreamsBuilder streamsBuilder
  ) {
    return streamsBuilder.stream(
      openApiCoverageStreamProperties.getCalculationRequestTopic(),
      Consumed.with(
        Serdes.String(),
        QualityGateCalculationEventSerdes.QualityGateCalculationRequestEvent()
      )
    );
  }
}
