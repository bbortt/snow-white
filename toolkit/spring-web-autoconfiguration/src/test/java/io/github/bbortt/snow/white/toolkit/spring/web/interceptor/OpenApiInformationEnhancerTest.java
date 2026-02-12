/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.spring.web.interceptor;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.springframework.util.StringUtils.hasText;

import io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation;
import io.github.bbortt.snow.white.toolkit.spring.web.config.SpringWebInterceptorProperties;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;

@ExtendWith({ MockitoExtension.class })
class OpenApiInformationEnhancerTest {

  private static final String API_NAME = "test-api";
  private static final String API_VERSION = "v1.0.0";
  private static final String SERVICE_NAME = "test-service";

  private String apiNameAttributeKey;
  private String apiVersionAttributeKey;
  private String serviceNameAttributeKey;
  private String operationIdAttributeKey;

  @Mock
  private HttpServletRequest httpServletRequestMock;

  @Mock
  private HttpServletResponse httpServletResponseMock;

  @Mock
  private HandlerMethod handlerMethodMock;

  @Mock
  private Method methodMock;

  @Mock
  private Span spanMock;

  @Mock
  private SpanProvider spanProviderMock;

  @Mock
  private SpringWebInterceptorProperties propertiesMock;

  private OpenApiInformationEnhancer fixture;

  private static SnowWhiteInformation createMockAnnotation(
    @Nullable String operationId
  ) {
    return new SnowWhiteInformation() {
      @Override
      public Class<SnowWhiteInformation> annotationType() {
        return SnowWhiteInformation.class;
      }

      @Override
      public String serviceName() {
        return SERVICE_NAME;
      }

      @Override
      public String apiName() {
        return API_NAME;
      }

      @Override
      public String apiVersion() {
        return API_VERSION;
      }

      @Override
      public String operationId() {
        return operationId;
      }
    };
  }

  @BeforeEach
  void beforeEachSetup() {
    apiNameAttributeKey = "custom.api.name" + randomUUID();
    apiVersionAttributeKey = "custom.api.version" + randomUUID();
    serviceNameAttributeKey = "custom.service.name" + randomUUID();
    operationIdAttributeKey = "custom.operation.id" + randomUUID();

    fixture = new OpenApiInformationEnhancer(propertiesMock);

    setField(fixture, "spanProvider", spanProviderMock, SpanProvider.class);
  }

  @AfterEach
  void httpServletObjectsAreNotToBeTouched() {
    verifyNoInteractions(httpServletRequestMock, httpServletResponseMock);
  }

  @Test
  void invocationWithNoSpanDoesNothing() {
    doReturn(null).when(spanProviderMock).getCurrentSpan();

    fixture.preHandle(
      httpServletRequestMock,
      httpServletResponseMock,
      handlerMethodMock
    );

    verify(handlerMethodMock, never()).getMethod();
  }

  @Test
  void invocationWithNoAnnotationDoesNothing() {
    doReturn(spanMock).when(spanProviderMock).getCurrentSpan();
    doReturn(methodMock).when(handlerMethodMock).getMethod();
    doReturn(false)
      .when(methodMock)
      .isAnnotationPresent(SnowWhiteInformation.class);

    fixture.preHandle(
      httpServletRequestMock,
      httpServletResponseMock,
      handlerMethodMock
    );

    verify(spanMock, never()).setAttribute(anyString(), anyString());
  }

  @Test
  void invocationWithAnnotationSetsSpanAttributes() {
    configureMockProperties(serviceNameAttributeKey, operationIdAttributeKey);

    var operationId = "operationId";
    var annotation = createMockAnnotation(operationId);

    fullMockConfiguration(annotation);

    fixture.preHandle(
      httpServletRequestMock,
      httpServletResponseMock,
      handlerMethodMock
    );

    verify(spanMock).setAttribute(apiNameAttributeKey, API_NAME);
    verify(spanMock).setAttribute(apiVersionAttributeKey, API_VERSION);
    verify(spanMock).setAttribute(serviceNameAttributeKey, SERVICE_NAME);
    verify(spanMock).setAttribute(operationIdAttributeKey, operationId);
  }

  public static Stream<String> nullAndEmptyStrings() {
    return Stream.of(null, "", " ");
  }

  @ParameterizedTest
  @MethodSource("nullAndEmptyStrings")
  void invocationWithAnnotationButNoServiceNamePropertySetsSpanAttributes(
    @Nullable String serviceNameAttributeKey
  ) {
    configureMockProperties(serviceNameAttributeKey, null);

    var annotation = createMockAnnotation(null);
    fullMockConfiguration(annotation);

    fixture.preHandle(
      httpServletRequestMock,
      httpServletResponseMock,
      handlerMethodMock
    );

    verify(spanMock).setAttribute(apiNameAttributeKey, API_NAME);
    verify(spanMock).setAttribute(apiVersionAttributeKey, API_VERSION);
    verifyNoMoreInteractions(spanMock);
  }

  @ParameterizedTest
  @MethodSource("nullAndEmptyStrings")
  void invocationWithAnnotationButNoOperationIdSetsSpanAttributes(
    @Nullable String operationId
  ) {
    configureMockProperties(null, operationId);

    var annotation = createMockAnnotation(null);
    fullMockConfiguration(annotation);

    fixture.preHandle(
      httpServletRequestMock,
      httpServletResponseMock,
      handlerMethodMock
    );

    verify(spanMock).setAttribute(apiNameAttributeKey, API_NAME);
    verify(spanMock).setAttribute(apiVersionAttributeKey, API_VERSION);
    verifyNoMoreInteractions(spanMock);
  }

  private void configureMockProperties(
    String serviceNameAttributeKey,
    String operationIdProperty
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

  private void fullMockConfiguration(SnowWhiteInformation annotation) {
    doReturn(spanMock).when(spanProviderMock).getCurrentSpan();
    doReturn(methodMock).when(handlerMethodMock).getMethod();
    doReturn(true)
      .when(methodMock)
      .isAnnotationPresent(SnowWhiteInformation.class);
    doReturn(annotation)
      .when(methodMock)
      .getAnnotation(SnowWhiteInformation.class);
  }
}
