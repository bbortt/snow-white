package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OperationKeyCalculatorTest {

  @Nested
  class ToOperationKey {

    @Test
    void shouldConstructOperationKey() {
      String result = OperationKeyCalculator.toOperationKey("path", "method");

      assertThat(result).isEqualTo("METHOD_path");
    }
  }

  @Nested
  class ToPath {

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
}
