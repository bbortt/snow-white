/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

    @Test
    void shouldThrowException_whenPropertyValueIsEmpty() {
      Map<String, String> properties = new HashMap<>();
      properties.put("property1", "value1");
      properties.put("property2", "");

      assertThatThrownBy(() ->
        PropertyUtils.assertRequiredProperties(properties)
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("All properties must be configured")
        .hasMessageContaining("property2");
    }

    @Test
    void shouldThrowException_whenPropertyValueContainsOnlyWhitespace() {
      Map<String, String> properties = new HashMap<>();
      properties.put("property1", "value1");
      properties.put("property2", "   ");

      assertThatThrownBy(() ->
        PropertyUtils.assertRequiredProperties(properties)
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("All properties must be configured")
        .hasMessageContaining("property2");
    }

    @Test
    void shouldThrowException_withAllMissingPropertiesInMessage() {
      Map<String, String> properties = new HashMap<>();
      properties.put("property1", "");
      properties.put("property2", null);
      properties.put("property3", "  ");
      properties.put("property4", "value4");

      assertThatThrownBy(() ->
        PropertyUtils.assertRequiredProperties(properties)
      )
        .isInstanceOf(IllegalArgumentException.class)
        .message()
        .satisfies(
          message -> assertThat(message).contains("property1"),
          message -> assertThat(message).contains("property2"),
          message -> assertThat(message).contains("property3"),
          message -> assertThat(message).doesNotContain("property4")
        );
    }
  }
}
