package io.github.bbortt.snow.white.toolkit.spring.aop.aspect;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation;
import io.github.bbortt.snow.white.toolkit.spring.aop.config.SnowWhiteSpringAopProperties;
import io.opentelemetry.api.trace.Span;
import java.lang.reflect.Method;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class RestControllerAspectTest {

  private static final String API_NAME = "test-api";
  private static final String API_VERSION = "v1.0.0";
  private static final String SERVICE_NAME = "test-service";

  private static final String API_NAME_PROPERTY = "custom.api.name";
  private static final String API_VERSION_PROPERTY = "custom.api.version";
  private static final String SERVICE_NAME_PROPERTY = "custom.service.name";

  @Mock
  private JoinPoint joinPointMock;

  @Mock
  private MethodSignature methodSignatureMock;

  @Mock
  private Method methodMock;

  @Mock
  private Span spanMock;

  @Mock
  private SpanProvider spanProviderMock;

  @Mock
  private SnowWhiteSpringAopProperties propertiesMock;

  private RestControllerAspect fixture;

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
  void setUp() {
    fixture = new RestControllerAspect(propertiesMock);
    setField(fixture, "spanProvider", spanProviderMock, SpanProvider.class);
  }

  @Test
  void invocationWithNoSpanDoesNothing() {
    doReturn(null).when(spanProviderMock).getCurrentSpan();

    fixture.beforeRestMethod(joinPointMock);

    verify(joinPointMock, never()).getSignature();
  }

  @Test
  void invocationWithNonMethodSignatureDoesNothing() {
    doReturn(spanMock).when(spanProviderMock).getCurrentSpan();
    doReturn(null).when(joinPointMock).getSignature();

    fixture.beforeRestMethod(joinPointMock);

    verify(spanMock, never()).setAttribute(API_NAME_PROPERTY, API_NAME);
  }

  @Test
  void invocationWithNoAnnotationDoesNothing() {
    doReturn(spanMock).when(spanProviderMock).getCurrentSpan();
    doReturn(methodSignatureMock).when(joinPointMock).getSignature();
    doReturn(methodMock).when(methodSignatureMock).getMethod();
    doReturn(false)
      .when(methodMock)
      .isAnnotationPresent(SnowWhiteInformation.class);

    fixture.beforeRestMethod(joinPointMock);

    verify(spanMock, never()).setAttribute(API_NAME_PROPERTY, API_NAME);
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
    doReturn(methodSignatureMock).when(joinPointMock).getSignature();
    doReturn(methodMock).when(methodSignatureMock).getMethod();
    doReturn(true)
      .when(methodMock)
      .isAnnotationPresent(SnowWhiteInformation.class);
    doReturn(annotation)
      .when(methodMock)
      .getAnnotation(SnowWhiteInformation.class);

    fixture.beforeRestMethod(joinPointMock);

    verify(spanMock).setAttribute(API_NAME_PROPERTY, API_NAME);
    verify(spanMock).setAttribute(API_VERSION_PROPERTY, API_VERSION);
    verify(spanMock).setAttribute(SERVICE_NAME_PROPERTY, SERVICE_NAME);
  }
}
