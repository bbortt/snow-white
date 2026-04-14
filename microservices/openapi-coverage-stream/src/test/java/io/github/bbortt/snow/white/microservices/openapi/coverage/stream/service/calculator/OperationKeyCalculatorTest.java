/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OperationKeyCalculatorTest {

  @Nested
  class ToOperationKeyTest {

    @Test
    void shouldConstructOperationKey() {
      String result = OperationKeyCalculator.toOperationKey("path", "method");

      assertThat(result).isEqualTo("METHOD_path");
    }
  }

  @Nested
  class ToPathTest {

    @Test
    void shouldConstructOperationKey() {
      String result = OperationKeyCalculator.toPath("method_path");

      assertThat(result).isEqualTo("path");
    }

    @Test
    void shouldConstructOperationKey_withoutPath() {
      String result = OperationKeyCalculator.toPath("method_");

      assertThat(result).isEmpty();
    }

    @Test
    void shouldConstructOperationKey_withMultipleUnderlines() {
      String result = OperationKeyCalculator.toPath(
        "method_path_with_underlines"
      );

      assertThat(result).isEqualTo("path_with_underlines");
    }
  }

  @Nested
  class ToOperationKeyPatternTest {

    @Test
    void shouldMatchConcretePathForTemplate() {
      Pattern pattern = OperationKeyCalculator.toOperationKeyPattern(
        "GET_/pung/{message}"
      );

      assertThat(pattern.matcher("GET_/pung/hello").matches()).isTrue();
    }

    @Test
    void shouldMatchConcretePathWithMultipleParams() {
      Pattern pattern = OperationKeyCalculator.toOperationKeyPattern(
        "GET_/api/v1/users/{userId}/orders/{orderId}"
      );

      assertThat(
        pattern.matcher("GET_/api/v1/users/42/orders/99").matches()
      ).isTrue();
    }

    @Test
    void shouldNotMatchWhenSegmentCountDiffers() {
      Pattern pattern = OperationKeyCalculator.toOperationKeyPattern(
        "GET_/pung/{message}"
      );

      assertThat(pattern.matcher("GET_/pung/hello/extra").matches()).isFalse();
    }

    @Test
    void shouldMatchExactPathWithNoParams() {
      Pattern pattern = OperationKeyCalculator.toOperationKeyPattern(
        "GET_/ping"
      );

      assertThat(pattern.matcher("GET_/ping").matches()).isTrue();
      assertThat(pattern.matcher("GET_/pong").matches()).isFalse();
    }
  }
}
