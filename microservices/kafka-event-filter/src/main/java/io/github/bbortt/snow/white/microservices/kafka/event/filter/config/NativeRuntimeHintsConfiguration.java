/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.config;

import static java.util.Collections.addAll;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;

import com.google.common.annotations.VisibleForTesting;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

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
      scanPackageForClassesRecursively("com.google.protobuf").forEach(clazz ->
        hints.reflection().registerType(clazz, INVOKE_PUBLIC_METHODS)
      );
      scanPackageForClassesRecursively("io.opentelemetry.proto").forEach(
        clazz -> hints.reflection().registerType(clazz, INVOKE_PUBLIC_METHODS)
      );
    }

    @VisibleForTesting
    static Set<Class<?>> scanPackageForClassesRecursively(String packageName) {
      var scanner = new ClassPathScanningCandidateComponentProvider(false);

      scanner.addIncludeFilter(
        new RegexPatternTypeFilter(Pattern.compile(".*"))
      );

      Set<Class<?>> classes = new HashSet<>();

      scanner
        .findCandidateComponents(packageName)
        .forEach(beanDefinition -> {
          try {
            Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
            classes.add(clazz);

            // Add nested classes
            Class<?>[] nestedClasses = clazz.getDeclaredClasses();
            addAll(classes, nestedClasses);
          } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
          }
        });

      logger.debug("Scanned classes: {}", classes);

      return classes;
    }
  }
}
