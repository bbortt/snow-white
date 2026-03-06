/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka.stream.processor;

import static io.github.bbortt.snow.white.commons.kafka.OtelPropagators.KAFKA_HEADERS_GETTER;
import static io.github.bbortt.snow.white.commons.kafka.OtelPropagators.KAFKA_HEADERS_SETTER;
import static io.opentelemetry.api.GlobalOpenTelemetry.getPropagators;

import io.opentelemetry.context.Context;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.processor.api.ContextualFixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorSupplier;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.jspecify.annotations.NonNull;

/**
 * A {@link FixedKeyProcessor} that handles OpenTelemetry tracing context propagation.
 *
 * @param <K> the type of the key.
 * @param <V> the type of the value.
 * @param <R> the type of the resulting value.
 */
@RequiredArgsConstructor
public class TracingProcessor<
  K,
  V,
  R
> extends ContextualFixedKeyProcessor<K, V, R> {

  private final BiFunction<FixedKeyRecord<K, V>, Long, R> processorFunction;

  /**
   * Creates a {@link FixedKeyProcessorSupplier} for a {@link TracingProcessor}.
   *
   * @param processorFunction the function to process the record.
   * @param <K> the type of the key.
   * @param <V> the type of the value.
   * @param <R> the type of the resulting value.
   * @return the supplier.
   */
  public static <K, V, R> FixedKeyProcessorSupplier<
    K,
    V,
    R
  > newTracingProcessor(
    BiFunction<FixedKeyRecord<K, V>, Long, R> processorFunction
  ) {
    return () -> new TracingProcessor<>(processorFunction);
  }

  private static <K, V> Context extractTraceContextFromIncomingHeaders(
    FixedKeyRecord<K, V> kafkaEventRecord
  ) {
    return getPropagators()
      .getTextMapPropagator()
      .extract(
        Context.current(),
        kafkaEventRecord.headers(),
        KAFKA_HEADERS_GETTER
      );
  }

  private static <K, R> void injectTraceContext(
    @NonNull FixedKeyRecord<K, R> outgoingRecord
  ) {
    getPropagators()
      .getTextMapPropagator()
      .inject(
        Context.current(),
        outgoingRecord.headers(),
        KAFKA_HEADERS_SETTER
      );
  }

  @Override
  public void process(FixedKeyRecord<K, V> kafkaEventRecord) {
    var extractedContext = extractTraceContextFromIncomingHeaders(
      kafkaEventRecord
    );

    try (var _ = extractedContext.makeCurrent()) {
      var result = processorFunction.apply(
        kafkaEventRecord,
        kafkaEventRecord.timestamp()
      );

      var outgoingRecord = kafkaEventRecord.withValue(result);

      injectTraceContext(outgoingRecord);

      context().forward(outgoingRecord);
    }
  }
}
