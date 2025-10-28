/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config;

import static io.github.bbortt.snow.white.commons.testing.ClassPathScanningUtils.scanPackageForClassesRecursively;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Slf4j
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
        )
        .registerType(
          org.apache.kafka.streams.processor.internals
            .NoOpProcessorWrapper.class
        );

      registerTypeIgnoringClassNotFoundException(
        hints,
        "io.confluent.kafka.serializers.subject.TopicNameStrategy"
      );
      registerTypeIgnoringClassNotFoundException(
        hints,
        "io.confluent.kafka.serializers.subject.TopicRecordNameStrategy"
      );
      registerTypeIgnoringClassNotFoundException(
        hints,
        "io.confluent.kafka.serializers.subject.RecordNameStrategy"
      );
    }

    private static void registerTypeIgnoringClassNotFoundException(
      RuntimeHints hints,
      String className
    ) {
      try {
        hints.reflection().registerType(Class.forName(className));
      } catch (ClassNotFoundException e) {
        logger.warn("Failed to register runtime hint class '%{}", className, e);
      }
    }
  }

  static class OtelRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
      scanPackageForClassesRecursively("com.google.protobuf").forEach(clazz ->
        hints.reflection().registerType(clazz, INVOKE_PUBLIC_METHODS)
      );
      scanPackageForClassesRecursively("io.opentelemetry.proto").forEach(
        clazz -> hints.reflection().registerType(clazz, INVOKE_PUBLIC_METHODS)
      );
    }
  }
}
