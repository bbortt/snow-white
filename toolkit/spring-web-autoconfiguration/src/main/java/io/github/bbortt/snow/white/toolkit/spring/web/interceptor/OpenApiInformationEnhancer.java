/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.spring.web.interceptor;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.util.StringUtils.hasText;

import io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation;
import io.github.bbortt.snow.white.toolkit.spring.web.config.SpringWebInterceptorProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenApiInformationEnhancer implements HandlerInterceptor {

  private final SpanProvider spanProvider = new SpanProvider();

  private final SpringWebInterceptorProperties springWebInterceptorProperties;

  @Override
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
    if (method.isAnnotationPresent(SnowWhiteInformation.class)) {
      var snowWhiteInformation = method.getAnnotation(
        SnowWhiteInformation.class
      );

      logger.trace("Enhancing span: [{}]", snowWhiteInformation);

      currentSpan.setAttribute(
        springWebInterceptorProperties.getApiNameAttribute(),
        snowWhiteInformation.apiName()
      );
      currentSpan.setAttribute(
        springWebInterceptorProperties.getApiVersionAttribute(),
        snowWhiteInformation.apiVersion()
      );

      if (
        hasText(springWebInterceptorProperties.getOtelServiceNameAttribute())
      ) currentSpan.setAttribute(
        springWebInterceptorProperties.getOtelServiceNameAttribute(),
        snowWhiteInformation.serviceName()
      );

      if (hasText(snowWhiteInformation.operationId())) currentSpan.setAttribute(
        springWebInterceptorProperties.getOperationIdAttribute(),
        snowWhiteInformation.operationId()
      );
    }

    return true;
  }
}
