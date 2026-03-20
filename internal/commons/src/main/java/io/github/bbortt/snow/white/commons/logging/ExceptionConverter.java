/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.logging;

import static lombok.AccessLevel.PRIVATE;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class ExceptionConverter {

  public static @NonNull String extractStackTraceOrErrorMessage(
    @NonNull Throwable rootCause
  ) {
    try (
      var stringWriter = new StringWriter();
      var printWriter = new PrintWriter(stringWriter)
    ) {
      rootCause.printStackTrace(printWriter);
      return stringWriter.toString();
    } catch (IOException e) {
      logger.warn("Failed to print stacktrace of exception!", e);
    }

    return rootCause.getMessage();
  }
}
