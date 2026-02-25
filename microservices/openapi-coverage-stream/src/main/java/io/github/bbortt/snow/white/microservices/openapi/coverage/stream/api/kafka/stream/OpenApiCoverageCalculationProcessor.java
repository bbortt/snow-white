/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka.stream;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka.serialization.QualityGateCalculationEventSerdes;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenApiCoverageService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenApiService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenTelemetryService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenApiTestContext;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.OpenApiNotIndexedException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.UnparseableOpenApiException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.influxdb.FluxAttributeFilter;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.api.ContextualFixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorSupplier;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenApiCoverageCalculationProcessor {

  private static @NonNull Set<FluxAttributeFilter> mapToFluxAttributeFilters(
    Set<AttributeFilter> attributeFilters
  ) {
    return Optional.ofNullable(attributeFilters)
      .orElseGet(Collections::emptySet)
      .stream()
      .map(FluxAttributeFilter::new)
      .collect(toSet());
  }

  private final OpenApiCoverageService openApiCoverageService;
  private final OpenApiCoverageStreamProperties openApiCoverageStreamProperties;
  private final OpenApiService openApiService;
  private final OpenTelemetryService openTelemetryService;

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
      .mapValues(this::fetchOpenApiSpecification)
      .processValues(
        (FixedKeyProcessorSupplier<
          String,
          OpenApiTestContext,
          OpenApiTestContext
        >) () ->
          new ContextualFixedKeyProcessor<>() {
            @Override
            public void process(
              FixedKeyRecord<String, OpenApiTestContext> kafkaEventRecord
            ) {
              var openApiTestContext = kafkaEventRecord.value();

              if (nonNull(openApiTestContext.e())) {
                context().forward(kafkaEventRecord);
                return;
              }

              context().forward(
                kafkaEventRecord.withValue(
                  openApiTestContext.withOpenTelemetryData(
                    openTelemetryService.findOpenTelemetryTracingData(
                      openApiTestContext.apiInformation(),
                      kafkaEventRecord.timestamp(),
                      openApiTestContext.lookbackWindow(),
                      openApiTestContext.fluxAttributeFilters()
                    )
                  )
                )
              );
            }
          }
      )
      .mapValues((key, openApiTestContext) -> {
        if (nonNull(openApiTestContext.e())) {
          return openApiTestContext;
        }

        return openApiTestContext.withOpenApiTestResults(
          openApiCoverageService.testOpenApi(requireNonNull(openApiTestContext))
        );
      })
      .flatMapValues((key, openApiTestContext) -> {
        if (nonNull(openApiTestContext.e())) {
          return singletonList(
            new OpenApiCoverageResponseEvent(
              OPENAPI,
              openApiTestContext.apiInformation(),
              null,
              openApiTestContext.e()
            )
          );
        }

        return singletonList(
          new OpenApiCoverageResponseEvent(
            OPENAPI,
            openApiTestContext.apiInformation(),
            openApiTestContext.openApiTestResults(),
            null
          )
        );
      })
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

  private OpenApiTestContext fetchOpenApiSpecification(
    String key,
    QualityGateCalculationRequestEvent calculationRequestEvent
  ) {
    var apiInformation = calculationRequestEvent.getApiInformation();
    var lookbackWindow = calculationRequestEvent.getLookbackWindow();
    var attributeFilters = calculationRequestEvent.getAttributeFilters();

    try {
      return new OpenApiTestContext(
        apiInformation,
        openApiService.findAndParseOpenApi(apiInformation),
        lookbackWindow,
        mapToFluxAttributeFilters(attributeFilters)
      );
    } catch (OpenApiNotIndexedException | UnparseableOpenApiException e) {
      logger.error(
        "Failed to process message with key {}: {}",
        key,
        e.getMessage(),
        e
      );

      return new OpenApiTestContext(
        apiInformation,
        null,
        lookbackWindow,
        mapToFluxAttributeFilters(attributeFilters),
        null,
        null,
        e.getMessage()
      );
    }
  }
}
