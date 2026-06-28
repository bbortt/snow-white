/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.annotation;

import static java.util.Objects.nonNull;
import static lombok.AccessLevel.PRIVATE;

import java.util.function.Function;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@NoArgsConstructor(access = PRIVATE)
public class SnowWhiteInformationExtractor {

  public static String resolveServiceName(
    @Nullable SnowWhiteInformation methodAnnotation,
    @Nullable SnowWhiteInformation classAnnotation
  ) {
    return resolve(
      methodAnnotation,
      classAnnotation,
      SnowWhiteInformation::serviceName
    );
  }

  public static String resolveApiName(
    @Nullable SnowWhiteInformation methodAnnotation,
    @Nullable SnowWhiteInformation classAnnotation
  ) {
    return resolve(
      methodAnnotation,
      classAnnotation,
      SnowWhiteInformation::apiName
    );
  }

  public static String resolveApiVersion(
    @Nullable SnowWhiteInformation methodAnnotation,
    @Nullable SnowWhiteInformation classAnnotation
  ) {
    return resolve(
      methodAnnotation,
      classAnnotation,
      SnowWhiteInformation::apiVersion
    );
  }

  public static String resolveOperationId(
    @Nullable SnowWhiteInformation methodAnnotation,
    @Nullable SnowWhiteInformation classAnnotation
  ) {
    return resolve(
      methodAnnotation,
      classAnnotation,
      SnowWhiteInformation::operationId
    );
  }

  private static String resolve(
    @Nullable SnowWhiteInformation methodLevel,
    @Nullable SnowWhiteInformation classLevel,
    Function<SnowWhiteInformation, String> getter
  ) {
    if (methodLevel != null && hasText(getter.apply(methodLevel))) {
      return getter.apply(methodLevel);
    }

    if (classLevel != null) {
      return getter.apply(classLevel);
    }

    return "";
  }

  private static boolean hasText(@Nullable String str) {
    return nonNull(str) && !str.isBlank();
  }
}
