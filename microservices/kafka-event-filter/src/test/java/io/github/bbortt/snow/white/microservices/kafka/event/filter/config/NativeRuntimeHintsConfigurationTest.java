/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.config;

import static io.github.bbortt.snow.white.microservices.kafka.event.filter.config.NativeRuntimeHintsConfiguration.OtelRuntimeHints.scanPackageForClassesRecursively;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;

@ExtendWith({ MockitoExtension.class })
class NativeRuntimeHintsConfigurationTest {

  @Mock
  private RuntimeHints runtimeHintsMock;

  @Mock
  private ReflectionHints reflectionHintsMock;

  @Mock
  private ClassLoader classLoaderMock;

  private NativeRuntimeHintsConfiguration.ConfluentKafkaRuntimeHints confluentKafkaRuntimeHints;
  private NativeRuntimeHintsConfiguration.OtelRuntimeHints otelRuntimeHints;

  @BeforeEach
  void beforeEachSetup() {
    confluentKafkaRuntimeHints =
      new NativeRuntimeHintsConfiguration.ConfluentKafkaRuntimeHints();
    otelRuntimeHints = new NativeRuntimeHintsConfiguration.OtelRuntimeHints();
  }

  @Test
  void shouldRegisterConfluentKafkaRuntimeHints() {
    doReturn(reflectionHintsMock).when(runtimeHintsMock).reflection();
    doReturn(reflectionHintsMock)
      .when(reflectionHintsMock)
      .registerType(any(Class.class));

    confluentKafkaRuntimeHints.registerHints(runtimeHintsMock, classLoaderMock);

    verify(reflectionHintsMock).registerType(
      io.confluent.kafka.serializers.context.NullContextNameStrategy.class
    );
    verify(reflectionHintsMock).registerType(
      io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer.class
    );
    verify(reflectionHintsMock).registerType(
      io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer.class
    );
    verify(reflectionHintsMock).registerType(
      io.confluent.kafka.streams.serdes.protobuf.KafkaProtobufSerde.class
    );

    verify(reflectionHintsMock).registerType(
      io.confluent.kafka.serializers.subject.TopicNameStrategy.class
    );
    verify(reflectionHintsMock).registerType(
      io.confluent.kafka.serializers.subject.TopicRecordNameStrategy.class
    );

    verifyNoInteractions(classLoaderMock);
  }

  @Test
  void shouldRegisterOtelKafkaRuntimeHints() {
    doReturn(reflectionHintsMock).when(runtimeHintsMock).reflection();
    doReturn(reflectionHintsMock)
      .when(reflectionHintsMock)
      .registerType(any(Class.class), eq(INVOKE_PUBLIC_METHODS));

    otelRuntimeHints.registerHints(runtimeHintsMock, classLoaderMock);

    verify(reflectionHintsMock, times(698)).registerType(
      any(Class.class),
      eq(INVOKE_PUBLIC_METHODS)
    );
    verifyNoInteractions(classLoaderMock);
  }

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
