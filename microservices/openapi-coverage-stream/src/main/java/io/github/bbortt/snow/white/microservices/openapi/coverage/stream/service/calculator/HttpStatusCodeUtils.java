/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static java.lang.Integer.parseInt;
import static lombok.AccessLevel.PRIVATE;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatusCode;

@NoArgsConstructor(access = PRIVATE)
final class HttpStatusCodeUtils {

  @RealizesSw(
    SwTraceables.SW_007_DEFAULT_RESPONSE_KEY_IS_THE_ERROR_FALLBACK_CASE
  )
  static boolean isErrorHttpStatusCode(String statusCode) {
    try {
      return HttpStatusCode.valueOf(parseInt(statusCode)).isError();
    } catch (NumberFormatException _) {
      // Handle patterns like "4XX", "5XX", "default"
      return (
        statusCode.startsWith("4") ||
        statusCode.startsWith("5") ||
        statusCode.equalsIgnoreCase("DEFAULT")
      );
    }
  }

  @RealizesSw(
    SwTraceables.SW_007_DEFAULT_RESPONSE_KEY_IS_THE_ERROR_FALLBACK_CASE
  )
  static boolean isPositiveHttpStatusCode(String statusCode) {
    try {
      int code = parseInt(statusCode);
      return code >= 100 && code <= 399;
    } catch (NumberFormatException _) {
      // Handle patterns like "1XX", "2XX", "3XX"
      return (
        statusCode.startsWith("1") ||
        statusCode.startsWith("2") ||
        statusCode.startsWith("3")
      );
    }
  }
}
