/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.spring.kafka.interceptor;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.springframework.util.StringUtils.hasText;

import io.github.bbortt.snow.white.toolkit.spring.kafka.config.SpringKafkaInterceptorProperties;
import io.github.bbortt.snow.white.toolkit.spring.kafka.listener.KafkaListenerInformationRegistry;
import io.github.bbortt.snow.white.toolkit.spring.kafka.listener.KafkaListenerInformationRegistry.SnowWhiteApiInformation;
import io.opentelemetry.api.trace.Span;
import java.util.stream.Stream;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class AsyncApiInformationEnhancerUnitTest {

  private static final String TOPIC = "test-topic";
  private static final String API_NAME = "test-api";
  private static final String API_VERSION = "v1.0.0";
  private static final String SERVICE_NAME = "test-service";
  private static final String OPERATION_ID = "receiveTestMessage";

  private String apiNameAttributeKey;
  private String apiVersionAttributeKey;
  private String serviceNameAttributeKey;
  private String operationIdAttributeKey;

  private ConsumerRecord<Object, Object> consumerRecord;

  @Mock
  private Consumer<Object, Object> consumerMock;

  @Mock
  private Span spanMock;

  @Mock
  private SpanProvider spanProviderMock;

  @Mock
  private KafkaListenerInformationRegistry registryMock;

  @Mock
  private SpringKafkaInterceptorProperties propertiesMock;

  @InjectMocks
  private AsyncApiInformationEnhancer fixture;

  public static Stream<String> nullAndEmptyStrings() {
    return Stream.of(null, "", " ");
  }

  @BeforeEach
  void beforeEachSetup() {
    apiNameAttributeKey = "custom.api.name" + randomUUID();
    apiVersionAttributeKey = "custom.api.version" + randomUUID();
    serviceNameAttributeKey = "custom.service.name" + randomUUID();
    operationIdAttributeKey = "custom.operation.id" + randomUUID();

    consumerRecord = new ConsumerRecord<>(TOPIC, 0, 0L, "key", "value");

    setField(fixture, "spanProvider", spanProviderMock, SpanProvider.class);
  }

  @AfterEach
  void theConsumerIsNotToBeTouched() {
    verifyNoInteractions(consumerMock);
  }

  @Test
  void invocationWithNoSpanDoesNothing() {
    doReturn(null).when(spanProviderMock).getCurrentSpan();

    assertThat(fixture.intercept(consumerRecord, consumerMock)).isSameAs(
      consumerRecord
    );

    verify(registryMock).findByTopic(TOPIC);
    verifyNoMoreInteractions(registryMock);
  }

  @Test
  void invocationWithUnknownTopicDoesNothing() {
    doReturn(spanMock).when(spanProviderMock).getCurrentSpan();
    doReturn(null).when(registryMock).findByTopic(TOPIC);

    assertThat(fixture.intercept(consumerRecord, consumerMock)).isSameAs(
      consumerRecord
    );

    verify(spanMock, never()).setAttribute(anyString(), anyString());
  }

  @Test
  void invocationWithKnownTopicSetsSpanAttributes() {
    configureMockProperties(serviceNameAttributeKey, operationIdAttributeKey);
    configureRegistry(OPERATION_ID);

    assertThat(fixture.intercept(consumerRecord, consumerMock)).isSameAs(
      consumerRecord
    );

    verify(spanMock).setAttribute(apiNameAttributeKey, API_NAME);
    verify(spanMock).setAttribute(apiVersionAttributeKey, API_VERSION);
    verify(spanMock).setAttribute(serviceNameAttributeKey, SERVICE_NAME);
    verify(spanMock).setAttribute(operationIdAttributeKey, OPERATION_ID);
    verifyNoMoreInteractions(spanMock);
  }

  @ParameterizedTest
  @MethodSource("nullAndEmptyStrings")
  void invocationWithoutServiceNamePropertySetsSpanAttributes(
    @Nullable String serviceNameAttributeKey
  ) {
    configureMockProperties(serviceNameAttributeKey, operationIdAttributeKey);
    configureRegistry(OPERATION_ID);

    fixture.intercept(consumerRecord, consumerMock);

    verify(spanMock).setAttribute(apiNameAttributeKey, API_NAME);
    verify(spanMock).setAttribute(apiVersionAttributeKey, API_VERSION);
    verify(spanMock).setAttribute(operationIdAttributeKey, OPERATION_ID);
    verifyNoMoreInteractions(spanMock);
  }

  @ParameterizedTest
  @MethodSource("nullAndEmptyStrings")
  void invocationWithoutOperationIdSetsSpanAttributes(
    @Nullable String operationId
  ) {
    configureMockProperties(null, null);
    configureRegistry(operationId);

    fixture.intercept(consumerRecord, consumerMock);

    verify(spanMock).setAttribute(apiNameAttributeKey, API_NAME);
    verify(spanMock).setAttribute(apiVersionAttributeKey, API_VERSION);
    verifyNoMoreInteractions(spanMock);
  }

  private void configureRegistry(@Nullable String operationId) {
    doReturn(spanMock).when(spanProviderMock).getCurrentSpan();
    doReturn(
      new SnowWhiteApiInformation(
        API_NAME,
        API_VERSION,
        SERVICE_NAME,
        operationId == null ? "" : operationId
      )
    )
      .when(registryMock)
      .findByTopic(TOPIC);
  }

  private void configureMockProperties(
    @Nullable String serviceNameAttributeKey,
    @Nullable String operationIdProperty
  ) {
    doReturn(apiNameAttributeKey).when(propertiesMock).getApiNameAttribute();
    doReturn(apiVersionAttributeKey)
      .when(propertiesMock)
      .getApiVersionAttribute();

    if (hasText(serviceNameAttributeKey)) {
      doReturn(serviceNameAttributeKey)
        .when(propertiesMock)
        .getOtelServiceNameAttribute();
    }

    if (hasText(operationIdProperty)) {
      doReturn(operationIdProperty)
        .when(propertiesMock)
        .getOperationIdAttribute();
    }
  }
}
