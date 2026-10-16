/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka.stream;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka.stream.processor.TracingProcessor.newTracingProcessor;
import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.springframework.util.CollectionUtils.isEmpty;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka.serialization.QualityGateCalculationEventSerdes;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenApiCoverageCalculationService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.OpenApiNotIndexedException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.TelemetryBackendUnavailableException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.UnmatchedWaiverException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.UnparseableOpenApiException;
import io.opentelemetry.api.OpenTelemetry;
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

  private final OpenTelemetry openTelemetry;
  private final OpenApiCoverageStreamProperties openApiCoverageStreamProperties;
  private final OpenApiCoverageCalculationService openApiCoverageCalculationService;

  @Bean
  @RealizesSw(SwTraceables.SW_008_KAFKA_AS_ASYNC_CALCULATION_DRIVER)
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
          openTelemetry,
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
    } catch (TelemetryBackendUnavailableException exception) {
      logger.error(
        "Telemetry backend unavailable while processing OpenAPI coverage for message: {}",
        exception.getMessage(),
        exception
      );

      return failureEvent(
        qualityGateCalculationRequestEventFixedKeyRecord,
        exception.getMessage()
      );
    } catch (UnmatchedWaiverException exception) {
      logger.error(
        "Refusing to publish coverage for a calculation holding a waiver that matched nothing: {}",
        exception.getMessage()
      );

      // Caught by name rather than left to the handler below, because what this branch guarantees
      // is the point of the failure and not an incidental consequence of it: the report carries the
      // reason its author has to act on, and carries no ratio that would read as a verdict on an
      // input the calculation did not fully apply. No stack trace - nothing went wrong in the code,
      // only in the request.
      return failureEvent(
        qualityGateCalculationRequestEventFixedKeyRecord,
        exception.getMessage()
      );
    } catch (Exception exception) {
      var rootCause = getRootCause(exception);

      logger.error(
        "Failed to process OpenAPI coverage for message: {}",
        rootCause.getMessage(),
        exception
      );

      return failureEvent(
        qualityGateCalculationRequestEventFixedKeyRecord,
        rootCause.getMessage() != null
          ? rootCause.getMessage()
          : rootCause.getClass().getSimpleName()
      );
    }
  }

  /**
   * Every failure answers the request the same way: the API it was about, and the reason, with no
   * results. The branches above differ in what they log and in nothing else, and this keeps that the
   * only thing they have to decide.
   */
  private static @NonNull OpenApiCoverageResponseEvent failureEvent(
    FixedKeyRecord<
      String,
      QualityGateCalculationRequestEvent
    > qualityGateCalculationRequestEventFixedKeyRecord,
    String errorMessage
  ) {
    return new OpenApiCoverageResponseEvent(
      qualityGateCalculationRequestEventFixedKeyRecord
        .value()
        .getApiInformation(),
      errorMessage
    );
  }

  private @NonNull OpenApiCoverageResponseEvent processOpenApiCoverageRequest(
    FixedKeyRecord<
      String,
      QualityGateCalculationRequestEvent
    > qualityGateCalculationRequestEventFixedKeyRecord,
    Long timestamp
  )
    throws OpenApiNotIndexedException, UnparseableOpenApiException, TelemetryBackendUnavailableException, UnmatchedWaiverException {
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
              .attributeFilters()
              .stream()
              .sorted(comparing(AttributeFilter::key))
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
