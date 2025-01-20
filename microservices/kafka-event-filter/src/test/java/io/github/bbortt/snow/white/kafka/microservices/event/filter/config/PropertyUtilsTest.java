package io.github.bbortt.snow.white.kafka.microservices.event.filter.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Properties;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PropertyUtilsTest {

  @Nested
  class PropertiesToMap {

    @Test
    void shouldConvertPropertiesToHashMap() {
      Properties properties = new Properties();
      properties.setProperty("key1", "value1");
      properties.setProperty("key2", "value2");
      properties.setProperty("key3", "value3");

      HashMap<String, String> result = PropertyUtils.propertiesToMap(
        properties
      );

      assertThat(result)
        .isInstanceOf(HashMap.class)
        .hasSize(3)
        .containsEntry("key1", "value1")
        .containsEntry("key2", "value2")
        .containsEntry("key3", "value3");
    }

    @Test
    void shouldHandleEmptyProperties() {
      Properties properties = new Properties();

      HashMap<String, String> result = PropertyUtils.propertiesToMap(
        properties
      );

      assertThat(result).isInstanceOf(HashMap.class).isEmpty();
    }

    @Test
    void shouldHandleDuplicateKeys() {
      Properties properties = new Properties();
      properties.setProperty("key1", "value1");
      properties.setProperty("key1", "value2");

      HashMap<String, String> result = PropertyUtils.propertiesToMap(
        properties
      );

      assertThat(result)
        .isInstanceOf(HashMap.class)
        .hasSize(1)
        .containsEntry("key1", "value2");
    }

    @Test
    void shouldConvertNonStringValues() {
      Properties properties = new Properties();
      properties.put("intKey", 123);
      properties.put("boolKey", true);
      properties.put("doubleKey", 3.14);

      HashMap<String, String> result = PropertyUtils.propertiesToMap(
        properties
      );

      assertThat(result)
        .isInstanceOf(HashMap.class)
        .hasSize(3)
        .containsEntry("intKey", "123")
        .containsEntry("boolKey", "true")
        .containsEntry("doubleKey", "3.14");
    }
  }
}
