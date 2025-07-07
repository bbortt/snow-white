/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.config;

import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration
@ImportRuntimeHints(
  {
    NativeRuntimeHintsConfiguration.ConfluentKafkaRuntimeHints.class,
    NativeRuntimeHintsConfiguration.OtelRuntimeHints.class,
  }
)
public class NativeRuntimeHintsConfiguration {

  static class ConfluentKafkaRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
      hints
        .reflection()
        .registerType(
          io.confluent.kafka.serializers.context.NullContextNameStrategy.class
        )
        .registerType(
          io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer.class
        )
        .registerType(
          io.confluent.kafka.serializers.protobuf
            .KafkaProtobufDeserializer.class
        )
        .registerType(
          io.confluent.kafka.streams.serdes.protobuf.KafkaProtobufSerde.class
        );

      try {
        hints
          .reflection()
          .registerType(
            Class.forName(
              "io.confluent.kafka.serializers.context.TopicNameStrategy"
            )
          );
        hints
          .reflection()
          .registerType(
            Class.forName(
              "io.confluent.kafka.serializers.context.TopicRecordNameStrategy"
            )
          );
        hints
          .reflection()
          .registerType(
            Class.forName(
              "io.confluent.kafka.serializers.context.RecordNameStrategy"
            )
          );
      } catch (ClassNotFoundException e) {
        // These classes might not be available in all versions
      }
    }
  }

  static class OtelRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
      hints
        .reflection()
        .registerType(ExportTraceServiceRequest.class, INVOKE_PUBLIC_METHODS)
        .registerType(
          ExportTraceServiceRequest.Builder.class,
          INVOKE_PUBLIC_METHODS
        )
        .registerType(ResourceSpans.class, INVOKE_PUBLIC_METHODS)
        .registerType(ResourceSpans.Builder.class, INVOKE_PUBLIC_METHODS)
        .registerType(ScopeSpans.class, INVOKE_PUBLIC_METHODS)
        .registerType(Span.class, INVOKE_PUBLIC_METHODS)
        .registerType(AnyValue.class, INVOKE_PUBLIC_METHODS)
        .registerType(KeyValue.class, INVOKE_PUBLIC_METHODS);
    }
  }
}
