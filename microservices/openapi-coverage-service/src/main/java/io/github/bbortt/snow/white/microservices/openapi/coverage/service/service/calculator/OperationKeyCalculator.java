/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.calculator;

import static lombok.AccessLevel.PRIVATE;

import jakarta.annotation.Nonnull;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class OperationKeyCalculator {

  public static String toOperationKey(
    @Nonnull String path,
    @Nonnull String method
  ) {
    return method.toUpperCase() + "_" + path;
  }

  public static String toPath(@Nonnull String operationKey) {
    return operationKey.substring(operationKey.indexOf("_") + 1);
  }
}
