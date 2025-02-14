package io.github.bbortt.snow.white.toolkit.spring.web.interceptor;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation;
import io.github.bbortt.snow.white.toolkit.spring.web.config.SpringWebInterceptorProperties;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
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

  private static final String API_NAME_PROPERTY = "custom.api.name";
  private static final String API_VERSION_PROPERTY = "custom.api.version";
  private static final String SERVICE_NAME_PROPERTY = "custom.service.name";

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

  private static SnowWhiteInformation createMockAnnotation() {
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
    };
  }

  @BeforeEach
  void beforeEachSetup() {
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
  void invocationWithNonMethodSignatureDoesNothing() {
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
    doReturn(API_NAME_PROPERTY).when(propertiesMock).getApiNameProperty();
    doReturn(API_VERSION_PROPERTY).when(propertiesMock).getApiVersionProperty();
    doReturn(SERVICE_NAME_PROPERTY)
      .when(propertiesMock)
      .getOtelServiceNameProperty();

    var annotation = createMockAnnotation();
    doReturn(spanMock).when(spanProviderMock).getCurrentSpan();
    doReturn(methodMock).when(handlerMethodMock).getMethod();
    doReturn(true)
      .when(methodMock)
      .isAnnotationPresent(SnowWhiteInformation.class);
    doReturn(annotation)
      .when(methodMock)
      .getAnnotation(SnowWhiteInformation.class);

    fixture.preHandle(
      httpServletRequestMock,
      httpServletResponseMock,
      handlerMethodMock
    );

    verify(spanMock).setAttribute(API_NAME_PROPERTY, API_NAME);
    verify(spanMock).setAttribute(API_VERSION_PROPERTY, API_VERSION);
    verify(spanMock).setAttribute(SERVICE_NAME_PROPERTY, SERVICE_NAME);
  }

  public static Stream<
    String
  > invocationWithAnnotationButNoServiceNamePropertySetsSpanAttributes() {
    return Stream.of(null, "");
  }

  @MethodSource
  @ParameterizedTest
  void invocationWithAnnotationButNoServiceNamePropertySetsSpanAttributes(
    @Nullable String serviceNameProperty
  ) {
    doReturn(API_NAME_PROPERTY).when(propertiesMock).getApiNameProperty();
    doReturn(API_VERSION_PROPERTY).when(propertiesMock).getApiVersionProperty();
    doReturn(serviceNameProperty)
      .when(propertiesMock)
      .getOtelServiceNameProperty();

    var annotation = createMockAnnotation();
    doReturn(spanMock).when(spanProviderMock).getCurrentSpan();
    doReturn(methodMock).when(handlerMethodMock).getMethod();
    doReturn(true)
      .when(methodMock)
      .isAnnotationPresent(SnowWhiteInformation.class);
    doReturn(annotation)
      .when(methodMock)
      .getAnnotation(SnowWhiteInformation.class);

    fixture.preHandle(
      httpServletRequestMock,
      httpServletResponseMock,
      handlerMethodMock
    );

    verify(spanMock).setAttribute(API_NAME_PROPERTY, API_NAME);
    verify(spanMock).setAttribute(API_VERSION_PROPERTY, API_VERSION);
    verifyNoMoreInteractions(spanMock);
  }
}
