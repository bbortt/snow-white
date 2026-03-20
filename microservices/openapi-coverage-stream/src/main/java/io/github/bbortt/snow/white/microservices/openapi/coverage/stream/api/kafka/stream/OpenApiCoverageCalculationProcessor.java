/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka.stream;

import static io.github.bbortt.snow.white.commons.logging.ExceptionConverter.extractStackTraceOrErrorMessage;
import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka.stream.processor.TracingProcessor.newTracingProcessor;
import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka.serialization.QualityGateCalculationEventSerdes;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenApiCoverageCalculationService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.OpenApiNotIndexedException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.UnparseableOpenApiException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.influxdb.FluxAttributeFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

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
      .filter((_, qualityGateCalculationRequestEvent) ->
        OPENAPI.equals(
          qualityGateCalculationRequestEvent.getApiInformation().getApiType()
        )
      )
      .peek((key, _) -> logger.debug("Handling message id '{}'", key))
      .processValues(
        newTracingProcessor(
          this::processOpenApiCoverageRequestAndHandleExceptions
        )
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

  private @NonNull OpenApiCoverageResponseEvent processOpenApiCoverageRequestAndHandleExceptions(
    FixedKeyRecord<
      String,
      QualityGateCalculationRequestEvent
    > qualityGateCalculationRequestEventFixedKeyRecord,
    Long timestamp
  ) {
    try {
      return processOpenApiCoverageRequest(
        qualityGateCalculationRequestEventFixedKeyRecord,
        timestamp
      );
    } catch (Exception exception) {
      var rootCause = getRootCause(exception);

      logger.error(
        "Failed to process OpenAPI coverage for message: {}",
        rootCause.getMessage(),
        exception
      );

      return new OpenApiCoverageResponseEvent(
        qualityGateCalculationRequestEventFixedKeyRecord
          .value()
          .getApiInformation(),
        extractStackTraceOrErrorMessage(rootCause)
      );
    }
  }

  private @NonNull OpenApiCoverageResponseEvent processOpenApiCoverageRequest(
    FixedKeyRecord<
      String,
      QualityGateCalculationRequestEvent
    > qualityGateCalculationRequestEventFixedKeyRecord,
    Long timestamp
  ) throws OpenApiNotIndexedException, UnparseableOpenApiException {
    var openApiTestContext =
      openApiCoverageCalculationService.fetchOpenApiSpecification(
        qualityGateCalculationRequestEventFixedKeyRecord.key(),
        qualityGateCalculationRequestEventFixedKeyRecord.value()
      );

    openApiTestContext =
      openApiCoverageCalculationService.enrichWithOpenTelemetryData(
        openApiTestContext,
        timestamp
      );

    if (isEmpty(openApiTestContext.openTelemetryData())) {
      return new OpenApiCoverageResponseEvent(
        openApiTestContext.apiInformation(),
        format(
          "Did not find any telemetry data with configured criteria: %s",
          JsonMapper.shared().writeValueAsString(
            openApiTestContext
              .fluxAttributeFilters()
              .stream()
              .sorted(comparing(FluxAttributeFilter::getKey))
              .map(FluxAttributeFilter::getBaseAttributeFilter)
              .toList()
          )
        )
      );
    }

    openApiTestContext = openApiCoverageCalculationService.calculateCoverage(
      openApiTestContext
    );

    return openApiCoverageCalculationService.buildResponseEvent(
      openApiTestContext
    );
  }
}
