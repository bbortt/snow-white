/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ExceptionConverterTest {

  @Nested
  class ExtractStackTraceOrErrorMessageTest {

    @Test
    void returnsNonBlankStringForPlainException() {
      var exception = new RuntimeException("something went wrong");

      String result = ExceptionConverter.extractStackTraceOrErrorMessage(
        exception
      );

      assertThat(result).isNotNull().isNotBlank();
    }

    @Test
    void includesExceptionClassName() {
      var exception = new IllegalArgumentException("bad arg");

      String result = ExceptionConverter.extractStackTraceOrErrorMessage(
        exception
      );

      assertThat(result).contains(IllegalArgumentException.class.getName());
    }

    @Test
    void includesExceptionMessage() {
      String message = "unique-error-message-12345";
      var exception = new RuntimeException(message);

      String result = ExceptionConverter.extractStackTraceOrErrorMessage(
        exception
      );

      assertThat(result).contains(message);
    }

    @Test
    void includesStackFrames() {
      var exception = new RuntimeException("with frames");

      String result = ExceptionConverter.extractStackTraceOrErrorMessage(
        exception
      );

      assertThat(result).contains("\tat ");
    }

    @Test
    void includesCallerClass() {
      var exception = new RuntimeException("caller check");

      String result = ExceptionConverter.extractStackTraceOrErrorMessage(
        exception
      );

      assertThat(result).contains(getClass().getName());
    }

    @Test
    void handlesNullMessageGracefully() {
      // NullPointerException has a null detail message by default
      var exception = new NullPointerException();

      String result = ExceptionConverter.extractStackTraceOrErrorMessage(
        exception
      );

      // The stack trace itself is still non-blank even when getMessage() == null
      assertThat(result).isNotNull().isNotBlank();
    }

    @Test
    void includesCauseInformation() {
      var cause = new IllegalStateException("root cause");
      var exception = new RuntimeException("wrapper", cause);

      String result = ExceptionConverter.extractStackTraceOrErrorMessage(
        exception
      );

      assertThat(result)
        .contains(IllegalStateException.class.getName())
        .contains("root cause");
    }

    @Test
    void includesCausedBySection() {
      var cause = new RuntimeException("cause");
      var exception = new RuntimeException("wrapper", cause);

      String result = ExceptionConverter.extractStackTraceOrErrorMessage(
        exception
      );

      assertThat(result).contains("Caused by:");
    }

    @Test
    void handlesDeeplyNestedCauseChain() {
      Throwable current = new RuntimeException("level-0");
      for (int i = 1; i <= 10; i++) {
        current = new RuntimeException("level-" + i, current);
      }

      String result = ExceptionConverter.extractStackTraceOrErrorMessage(
        current
      );

      assertThat(result)
        .isNotNull()
        .isNotBlank()
        .contains("level-0")
        .contains("level-10");
    }

    @Test
    void handlesCustomThrowableSubclass() {
      class CustomError extends Error {

        CustomError(String msg) {
          super(msg);
        }
      }

      var error = new CustomError("custom-error-message");

      String result = ExceptionConverter.extractStackTraceOrErrorMessage(error);

      assertThat(result).contains("custom-error-message");
    }
  }
}
