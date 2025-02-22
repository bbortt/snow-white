package io.github.bbortt.snow.white.toolkit.spring.web.interceptor;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.util.StringUtils.hasText;

import io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation;
import io.github.bbortt.snow.white.toolkit.spring.web.config.SpringWebInterceptorProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class OpenApiInformationEnhancer implements HandlerInterceptor {

  private final SpanProvider spanProvider = new SpanProvider();

  private final SpringWebInterceptorProperties springWebInterceptorProperties;

  public OpenApiInformationEnhancer(
    SpringWebInterceptorProperties springWebInterceptorProperties
  ) {
    this.springWebInterceptorProperties = springWebInterceptorProperties;
  }

  public boolean preHandle(
    HttpServletRequest request,
    HttpServletResponse response,
    Object handler
  ) {
    var currentSpan = spanProvider.getCurrentSpan();
    if (
      !(handler instanceof HandlerMethod handlerMethod) || isNull(currentSpan)
    ) {
      return true; // No active span, nothing to enhance
    }

    var method = handlerMethod.getMethod();
    if (
      nonNull(method) && method.isAnnotationPresent(SnowWhiteInformation.class)
    ) {
      var snowWhiteInformation = method.getAnnotation(
        SnowWhiteInformation.class
      );

      logger.trace("Enhancing span: [{}]", snowWhiteInformation);

      currentSpan.setAttribute(
        springWebInterceptorProperties.getApiNameProperty(),
        snowWhiteInformation.apiName()
      );
      currentSpan.setAttribute(
        springWebInterceptorProperties.getApiVersionProperty(),
        snowWhiteInformation.apiVersion()
      );

      if (
        hasText(springWebInterceptorProperties.getOtelServiceNameProperty())
      ) currentSpan.setAttribute(
        springWebInterceptorProperties.getOtelServiceNameProperty(),
        snowWhiteInformation.serviceName()
      );

      if (hasText(snowWhiteInformation.operationId())) currentSpan.setAttribute(
        springWebInterceptorProperties.getOperationIdProperty(),
        snowWhiteInformation.operationId()
      );
    }

    return true;
  }
}
