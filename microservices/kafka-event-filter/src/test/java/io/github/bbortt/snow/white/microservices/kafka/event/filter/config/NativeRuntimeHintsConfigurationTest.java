/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;

import org.junit.jupiter.api.BeforeEach;
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

    verify(reflectionHintsMock, times(697)).registerType(
      any(Class.class),
      eq(INVOKE_PUBLIC_METHODS)
    );
    verifyNoInteractions(classLoaderMock);
  }
}
