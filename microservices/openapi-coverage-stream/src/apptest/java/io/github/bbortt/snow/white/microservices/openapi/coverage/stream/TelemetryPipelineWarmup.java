/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.OtlpTraceFixtures.Span.span;
import static lombok.AccessLevel.PRIVATE;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.NoArgsConstructor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * The very first trace published onto {@code snow-white_outbound} in a JVM pays a one-time cold start tax that has nothing to do with any individual test:
 * the topic has to be auto-created,
 * the collector's kafka receiver has to subscribe,
 * and its {@code otlp_grpc/grafana} exporter has to establish its first (slow) gRPC connection to the bundled Tempo.
 * Whichever apptest class happens to run first ends up carrying that cost inside its own {@link AbstractOpenApiCoverageStreamAppTest#TELEMETRY_INGESTION_MARGIN},
 * making it flaky unless that margin is inflated for every test to cover the worst case.
 * <p>
 * Paying this cost once, upfront, outside of any single test's budget -
 * before any assertions start - removes that variance instead of guessing at an ever-larger fixed margin.
 */
@NoArgsConstructor(access = PRIVATE)
final class TelemetryPipelineWarmup {

  private static final AtomicBoolean STARTED = new AtomicBoolean(false);

  static void ensureWarm(String bootstrapServers, String topic) {
    if (!STARTED.compareAndSet(false, true)) {
      return;
    }

    var producerProperties = new Properties();
    producerProperties.put(
      ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
      bootstrapServers
    );
    producerProperties.put(
      ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
      StringSerializer.class
    );
    producerProperties.put(
      ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
      StringSerializer.class
    );

    try (var producer = new KafkaProducer<String, String>(producerProperties)) {
      var payload = OtlpTraceFixtures.traceRequestJson(
        span("telemetry-pipeline-warmup", "warmup")
      );
      producer
        .send(new ProducerRecord<>(topic, payload))
        .get(30, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new IllegalStateException(
        "Failed to warm up the telemetry pipeline",
        e
      );
    }

    try {
      Thread.sleep(Duration.ofSeconds(60).toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
