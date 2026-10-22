/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config;

import static io.github.bbortt.snow.white.commons.testing.ClassPathScanningUtils.scanPackageForClassesRecursively;
import static lombok.AccessLevel.PROTECTED;
import static org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Slf4j
@Configuration
@NoArgsConstructor(access = PROTECTED)
@ImportRuntimeHints({
  NativeRuntimeHintsConfiguration.ApiIndexApiDtoRuntimeHints.class,
  NativeRuntimeHintsConfiguration.ConfluentKafkaRuntimeHints.class,
  NativeRuntimeHintsConfiguration.KafkaStreamsRuntimeHints.class,
  NativeRuntimeHintsConfiguration.OtelRuntimeHints.class,
})
public class NativeRuntimeHintsConfiguration {

  @NullMarked
  static class ApiIndexApiDtoRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(
      RuntimeHints hints,
      @Nullable ClassLoader classLoader
    ) {
      scanPackageForClassesRecursively(
        "io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.client.apiindexapi.dto"
      ).forEach(clazz ->
        hints
          .reflection()
          .registerType(
            clazz,
            INVOKE_PUBLIC_CONSTRUCTORS,
            INVOKE_PUBLIC_METHODS
          )
      );
    }
  }

  @NullMarked
  static class ConfluentKafkaRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(
      RuntimeHints hints,
      @Nullable ClassLoader classLoader
    ) {
      hints
        .reflection()
        .registerType(
          io.confluent.kafka.serializers.context.NullContextNameStrategy.class
        )
        .registerType(
          io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer.class
        )
        .registerType(
          io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer.class
        )
        .registerType(
          io.confluent.kafka.streams.serdes.protobuf.KafkaProtobufSerde.class
        )
        .registerType(
          org.apache.kafka.streams.processor.internals.NoOpProcessorWrapper.class
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

  /**
   * The GraalVM reachability metadata for {@code kafka-streams:4.3.1} misses
   * types that Kafka Streams instantiates reflectively. The assignor is loaded
   * by class name through the consumer's {@code partition.assignment.strategy},
   * the process file is (de-)serialized by Jackson in the state directory.
   */
  @NullMarked
  static class KafkaStreamsRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(
      RuntimeHints hints,
      @Nullable ClassLoader classLoader
    ) {
      hints
        .reflection()
        .registerType(
          org.apache.kafka.streams.processor.internals.StreamsPartitionAssignor.class,
          INVOKE_PUBLIC_CONSTRUCTORS
        )
        .registerType(
          TypeReference.of(
            "org.apache.kafka.streams.processor.internals.StateDirectory$StateDirectoryProcessFile"
          ),
          INVOKE_DECLARED_CONSTRUCTORS,
          DECLARED_FIELDS
        );
    }
  }

  @NullMarked
  static class OtelRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(
      RuntimeHints hints,
      @Nullable ClassLoader classLoader
    ) {
      scanPackageForClassesRecursively("com.google.protobuf").forEach(clazz ->
        hints.reflection().registerType(clazz, INVOKE_PUBLIC_METHODS)
      );
      scanPackageForClassesRecursively("io.opentelemetry.proto").forEach(
        clazz -> hints.reflection().registerType(clazz, INVOKE_PUBLIC_METHODS)
      );
    }
  }
}
