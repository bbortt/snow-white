/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static java.util.Locale.ROOT;
import static lombok.AccessLevel.PRIVATE;

import java.util.regex.Pattern;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;

@NoArgsConstructor(access = PRIVATE)
public final class OperationKeyCalculator {

  private static final Pattern PARAMETER_SEGMENT = Pattern.compile("\\{[^/]+}");

  public static String toOperationKey(
    @NonNull String path,
    @NonNull String method
  ) {
    return method.toUpperCase(ROOT) + "_" + path;
  }

  public static String toPath(@NonNull String operationKey) {
    return operationKey.substring(operationKey.indexOf("_") + 1);
  }

  public static String toMethod(@NonNull String operationKey) {
    return operationKey.substring(0, operationKey.indexOf("_"));
  }

  /**
   * Converts an operation key that may contain path-parameter templates (e.g. {@code "GET_/pung/{message}"}) into a {@link Pattern} that matches concrete operation keys with resolved values (e.g. {@code "GET_/pung/hello"}).
   * The literal segments between placeholders are quoted rather than spliced into the regex
   * verbatim, so a path carrying a regex metacharacter (e.g. {@code "GET_/reports/{id}.json"})
   * matches only that literal character, never "any character".
   */
  public static Pattern toOperationKeyPattern(
    @NonNull String templateOperationKey
  ) {
    var matcher = PARAMETER_SEGMENT.matcher(templateOperationKey);
    var regex = new StringBuilder("^");
    var lastEnd = 0;

    while (matcher.find()) {
      regex.append(
        Pattern.quote(templateOperationKey.substring(lastEnd, matcher.start()))
      );
      regex.append("[^/]+");
      lastEnd = matcher.end();
    }
    regex.append(Pattern.quote(templateOperationKey.substring(lastEnd)));
    regex.append("$");

    return Pattern.compile(regex.toString());
  }
}
