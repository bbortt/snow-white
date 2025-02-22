package io.github.bbortt.snow.white.toolkit.spring.web.interceptor;

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
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;

@ExtendWith({ MockitoExtension.class })
class OpenApiInformationEnhancerTest {

  private static final String API_NAME = "test-api";
  private static final String API_VERSION = "v1.0.0";
  private static final String SERVICE_NAME = "test-service";

  private String apiNameProperty;
  private String apiVersionProperty;
  private String serviceNameProperty;
  private String operationIdProperty;

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
    apiNameProperty = "custom.api.name" + UUID.randomUUID();
    apiVersionProperty = "custom.api.version" + UUID.randomUUID();
    serviceNameProperty = "custom.service.name" + UUID.randomUUID();
    operationIdProperty = "custom.operation.id" + UUID.randomUUID();

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
  void invocationWithNoMethodSignatureDoesNothing() {
    doReturn(spanMock).when(spanProviderMock).getCurrentSpan();
    doReturn(null).when(handlerMethodMock).getMethod();

    fixture.preHandle(
      httpServletRequestMock,
      httpServletResponseMock,
      handlerMethodMock
    );

    verify(spanMock, never()).setAttribute(anyString(), anyString());
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
    configureMockProperties(serviceNameProperty, operationIdProperty);

    var operationId = "operationId";
    var annotation = createMockAnnotation(operationId);

    fullMockConfiguration(annotation);

    fixture.preHandle(
      httpServletRequestMock,
      httpServletResponseMock,
      handlerMethodMock
    );

    verify(spanMock).setAttribute(apiNameProperty, API_NAME);
    verify(spanMock).setAttribute(apiVersionProperty, API_VERSION);
    verify(spanMock).setAttribute(serviceNameProperty, SERVICE_NAME);
    verify(spanMock).setAttribute(operationIdProperty, operationId);
  }

  public static Stream<String> nullAndEmptyStrings() {
    return Stream.of(null, "", " ");
  }

  @ParameterizedTest
  @MethodSource("nullAndEmptyStrings")
  void invocationWithAnnotationButNoServiceNamePropertySetsSpanAttributes(
    @Nullable String serviceNameProperty
  ) {
    configureMockProperties(serviceNameProperty, null);

    var annotation = createMockAnnotation(null);
    fullMockConfiguration(annotation);

    fixture.preHandle(
      httpServletRequestMock,
      httpServletResponseMock,
      handlerMethodMock
    );

    verify(spanMock).setAttribute(apiNameProperty, API_NAME);
    verify(spanMock).setAttribute(apiVersionProperty, API_VERSION);
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

    verify(spanMock).setAttribute(apiNameProperty, API_NAME);
    verify(spanMock).setAttribute(apiVersionProperty, API_VERSION);
    verifyNoMoreInteractions(spanMock);
  }

  private void configureMockProperties(
    String serviceNameProperty,
    String operationIdProperty
  ) {
    doReturn(apiNameProperty).when(propertiesMock).getApiNameProperty();
    doReturn(apiVersionProperty).when(propertiesMock).getApiVersionProperty();

    if (hasText(serviceNameProperty)) {
      doReturn(serviceNameProperty)
        .when(propertiesMock)
        .getOtelServiceNameProperty();
    }

    if (hasText(operationIdProperty)) {
      doReturn(operationIdProperty)
        .when(propertiesMock)
        .getOperationIdProperty();
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
