/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
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
import org.junit.jupiter.params.provider.ValueSource;

class PropertyUtilsTest {

  @Nested
  class AssertRequiredPropertiesTest {

    @Test
    void shouldNotThrowException_whenAllPropertiesArePresent() {
      Map<String, String> properties = new HashMap<>();
      properties.put("property1", "value1");
      properties.put("property2", "value2");

      assertThatNoException().isThrownBy(() ->
        PropertyUtils.assertRequiredProperties(properties)
      );
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

    public static Stream<String> shouldThrowException_whenPropertyValueIsNullOrEmpty() {
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

  @Nested
  class AssertPositivePropertyTest {

    @ValueSource(ints = { 1, 2, Integer.MAX_VALUE })
    @ParameterizedTest
    void shouldNotThrowException_whenValueIsPositive(int value) {
      assertThatNoException().isThrownBy(() ->
        PropertyUtils.assertPositiveProperty("property1", value)
      );
    }

    @ValueSource(ints = { 0, -1, Integer.MIN_VALUE })
    @ParameterizedTest
    void shouldThrowException_whenValueIsNotPositive(int value) {
      assertThatThrownBy(() ->
        PropertyUtils.assertPositiveProperty("property1", value)
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "Property 'property1' must be greater than 0, but was: %s!".formatted(
            value
          )
        );
    }
  }
}
