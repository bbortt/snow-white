/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.spring.web.validation;

import static io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformationExtractor.resolveApiName;
import static io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformationExtractor.resolveApiVersion;
import static io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformationExtractor.resolveServiceName;
import static java.util.Objects.nonNull;
import static org.springframework.aop.support.AopUtils.getTargetClass;
import static org.springframework.core.annotation.AnnotatedElementUtils.hasAnnotation;
import static org.springframework.util.StringUtils.hasText;

import io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
public class SnowWhiteAnnotationValidator {

  @EventListener(ApplicationReadyEvent.class)
  public void validateOnStartup(ApplicationReadyEvent event) {
    event
      .getApplicationContext()
      .getBeansWithAnnotation(Controller.class)
      .values()
      .forEach(bean -> validateController(getTargetClass(bean)));
  }

  private void validateController(Class<?> controllerClass) {
    var classAnnotation = controllerClass.getAnnotation(
      SnowWhiteInformation.class
    );

    for (var method : controllerClass.getDeclaredMethods()) {
      if (
        !methodIsRequestMapping(method) ||
        !classOrMethodHasSnowWhiteInformation(classAnnotation, method)
      ) {
        continue;
      }

      var methodAnnotation = method.getAnnotation(SnowWhiteInformation.class);

      var serviceName = resolveServiceName(methodAnnotation, classAnnotation);
      var apiName = resolveApiName(methodAnnotation, classAnnotation);
      var apiVersion = resolveApiVersion(methodAnnotation, classAnnotation);

      if (!hasText(serviceName) || !hasText(apiName) || !hasText(apiVersion)) {
        throw new IllegalStateException(
          "Incomplete @SnowWhiteInformation on %s#%s: serviceName='%s', apiName='%s', apiVersion='%s'".formatted(
            controllerClass.getSimpleName(),
            method.getName(),
            serviceName,
            apiName,
            apiVersion
          )
        );
      }
    }
  }

  private boolean classOrMethodHasSnowWhiteInformation(
    @Nullable SnowWhiteInformation classAnnotation,
    @NonNull Method method
  ) {
    return (
      nonNull(classAnnotation) ||
      nonNull(method.getAnnotation(SnowWhiteInformation.class))
    );
  }

  private static boolean methodIsRequestMapping(@NonNull Method method) {
    return !method.isSynthetic() && hasAnnotation(method, RequestMapping.class);
  }
}
