/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;

@NoArgsConstructor(access = PRIVATE)
public final class OperationKeyCalculator {

  public static String toOperationKey(
    @NonNull String path,
    @NonNull String method
  ) {
    return method.toUpperCase() + "_" + path;
  }

  public static String toPath(@NonNull String operationKey) {
    return operationKey.substring(operationKey.indexOf("_") + 1);
  }
}
