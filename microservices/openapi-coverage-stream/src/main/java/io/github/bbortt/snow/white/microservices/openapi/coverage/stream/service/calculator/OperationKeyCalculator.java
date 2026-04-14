/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static lombok.AccessLevel.PRIVATE;

import java.util.regex.Pattern;
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

  /**
   * Converts an operation key that may contain path-parameter templates (e.g. {@code "GET_/pung/{message}"}) into a {@link Pattern} that matches concrete operation keys with resolved values (e.g. {@code "GET_/pung/hello"}).
   */
  public static Pattern toOperationKeyPattern(
    @NonNull String templateOperationKey
  ) {
    String regex = templateOperationKey.replaceAll("\\{[^/]+}", "[^/]+");
    return Pattern.compile("^" + regex + "$");
  }
}
