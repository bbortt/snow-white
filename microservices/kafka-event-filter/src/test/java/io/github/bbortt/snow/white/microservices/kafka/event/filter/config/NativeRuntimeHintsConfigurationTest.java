/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.config;

import static io.github.bbortt.snow.white.microservices.kafka.event.filter.config.NativeRuntimeHintsConfiguration.OtelRuntimeHints.scanPackageForClassesRecursively;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NativeRuntimeHintsConfigurationTest {

  @Nested
  class ScanPackageForClassesRecursively {

    @Test
    void shouldScanAllClasses_includingStaticNestedClasses() {
      var configClasses = scanPackageForClassesRecursively(
        "io.github.bbortt.snow.white.microservices.kafka.event.filter.config"
      );

      assertThat(configClasses)
        .hasSizeGreaterThanOrEqualTo(28)
        .contains(
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config.NativeRuntimeHintsConfiguration
            .OtelRuntimeHints.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config.ProtobufProcessorConfigTest
            .ProtobufProcessor.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties
            .ConsumerMode.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config
            .KafkaEventFilterPropertiesIT.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config
            .ProtobufProcessorConfigTest.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config.NativeRuntimeHintsConfiguration
            .ConfluentKafkaRuntimeHints.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config
            .KafkaStreamsProtobufConfigIT.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config.PropertyUtilsTest
            .PropertiesToMap.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config.ProtobufProcessorConfigTest
            .JsonProcessor.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterPropertiesTest
            .AfterPropertiesSet.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config
            .PropertyUtilsTest.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaStreamsConfigTest
            .SnowWhiteKafkaProperties.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config
            .NativeRuntimeHintsConfigurationTest.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaStreamsConfigTest
            .JsonSerde.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config
            .KafkaEventFilterPropertiesTest.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config
            .KafkaStreamsConfig.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config
            .RedisConfig.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config
            .ProtobufProcessorConfig.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config
            .NativeRuntimeHintsConfiguration.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterPropertiesTest
            .consumerMode.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties
            .Filtering.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config
            .KafkaEventFilterProperties.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config
            .KafkaStreamsConfigTest.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config
            .RedisConfigTest.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config
            .PropertyUtils.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config.NativeRuntimeHintsConfigurationTest
            .ScanPackageForClassesRecursively.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config
            .RedisConfigIT.class,
          io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterPropertiesTest
            .SemanticConvention.class
        );
    }
  }
}
