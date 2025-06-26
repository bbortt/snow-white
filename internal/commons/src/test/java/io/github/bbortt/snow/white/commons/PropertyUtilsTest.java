/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PropertyUtilsTest {

  @Nested
  class AssertRequiredProperties {

    @Test
    void shouldNotThrowException_whenAllPropertiesArePresent() {
      Map<String, String> properties = new HashMap<>();
      properties.put("property1", "value1");
      properties.put("property2", "value2");

      assertThatNoException()
        .isThrownBy(() -> PropertyUtils.assertRequiredProperties(properties));
    }

    @Test
    void shouldThrowException_whenPropertyValueIsNull() {
      Map<String, String> properties = new HashMap<>();
      properties.put("property1", "value1");
      properties.put("property2", null);

      assertThatThrownBy(() ->
        PropertyUtils.assertRequiredProperties(properties)
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("All properties must be configured")
        .hasMessageContaining("property2");
    }

    public static Stream<
      String
    > shouldThrowException_whenPropertyValueIsNullOrEmpty() {
      return Stream.of(null, "", " ");
    }

    @MethodSource
    @ParameterizedTest
    void shouldThrowException_whenPropertyValueIsNullOrEmpty(
      String propertyValue
    ) {
      Map<String, String> properties = new HashMap<>();
      properties.put("property1", "value1");
      properties.put("property2", propertyValue);

      assertThatThrownBy(() ->
        PropertyUtils.assertRequiredProperties(properties)
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("All properties must be configured")
        .hasMessageContaining("property2");
    }
  }
}
