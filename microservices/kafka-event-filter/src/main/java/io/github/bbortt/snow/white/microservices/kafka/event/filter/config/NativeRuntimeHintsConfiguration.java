/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.config;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration
@ImportRuntimeHints(
  NativeRuntimeHintsConfiguration.ConfluentKafkaRuntimeHints.class
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
}
