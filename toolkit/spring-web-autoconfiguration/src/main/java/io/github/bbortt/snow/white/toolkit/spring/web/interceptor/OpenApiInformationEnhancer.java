/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.spring.web.interceptor;

import static io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformationExtractor.resolveApiName;
import static io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformationExtractor.resolveApiVersion;
import static io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformationExtractor.resolveOperationId;
import static io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformationExtractor.resolveServiceName;
import static java.util.Objects.isNull;
import static org.springframework.util.StringUtils.hasText;

import io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation;
import io.github.bbortt.snow.white.toolkit.spring.web.config.SpringWebInterceptorProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@NullMarked
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
      return true;
    }

    var methodAnnotation = handlerMethod
      .getMethod()
      .getAnnotation(SnowWhiteInformation.class);
    var classAnnotation = handlerMethod
      .getBeanType()
      .getAnnotation(SnowWhiteInformation.class);

    if (isNull(classAnnotation) && isNull(methodAnnotation)) {
      return true;
    }

    logger.trace(
      "Enhancing span: method=[{}], class=[{}]",
      methodAnnotation,
      classAnnotation
    );

    currentSpan.setAttribute(
      springWebInterceptorProperties.getApiNameAttribute(),
      resolveApiName(methodAnnotation, classAnnotation)
    );
    currentSpan.setAttribute(
      springWebInterceptorProperties.getApiVersionAttribute(),
      resolveApiVersion(methodAnnotation, classAnnotation)
    );

    if (hasText(springWebInterceptorProperties.getOtelServiceNameAttribute())) {
      currentSpan.setAttribute(
        springWebInterceptorProperties.getOtelServiceNameAttribute(),
        resolveServiceName(methodAnnotation, classAnnotation)
      );
    }

    var operationId = resolveOperationId(methodAnnotation, classAnnotation);
    if (hasText(operationId)) {
      currentSpan.setAttribute(
        springWebInterceptorProperties.getOperationIdAttribute(),
        operationId
      );
    }

    return true;
  }
}
