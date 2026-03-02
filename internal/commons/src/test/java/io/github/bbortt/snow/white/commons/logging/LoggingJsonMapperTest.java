/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LoggingJsonMapperTest {

  static class SimpleConfig {

    public String name = "snow-white";
    public String token = "SECRET_TOKEN";
  }

  static class NestedConfig {

    public String service = "api";
    public Credentials credentials = new Credentials();
  }

  static class Credentials {

    public String accessToken = "VERY_SECRET";
  }

  static class NullValues {

    public String token = null;
  }

  static class BrokenBean {

    public String getValue() {
      throw new RuntimeException("boom");
    }
  }

  @Nested
  class ToMaskedJsonRepresentationTest {

    @Test
    void shouldMaskSensitiveFields() {
      var config = new SimpleConfig();

      String json = LoggingJsonMapper.toMaskedJsonRepresentation(config);

      assertThat(json)
        .contains("\"name\":\"snow-white\"")
        .contains("\"token\":\"***\"")
        .doesNotContain("SECRET_TOKEN");
    }

    @Test
    void shouldKeepNormalFieldsUnchanged() {
      var config = new SimpleConfig();

      String json = LoggingJsonMapper.toMaskedJsonRepresentation(config);

      assertThat(json).contains("\"name\":\"snow-white\"");
    }

    @Test
    void shouldMaskNestedSensitiveFields() {
      var config = new NestedConfig();

      String json = LoggingJsonMapper.toMaskedJsonRepresentation(config);

      assertThat(json)
        .contains("\"service\":\"api\"")
        .contains("\"accessToken\":\"***\"")
        .doesNotContain("VERY_SECRET");
    }

    @Test
    void shouldHandleNullValues() {
      var config = new NullValues();

      String json = LoggingJsonMapper.toMaskedJsonRepresentation(config);

      assertThat(json).contains("\"token\":null");
    }

    @Test
    void shouldReturnFallbackOnSerializationFailure() {
      var broken = new BrokenBean();

      String json = LoggingJsonMapper.toMaskedJsonRepresentation(broken);

      assertThat(json).isEqualTo("<serialization failed>");
    }

    @Test
    void shouldProduceValidJson() {
      var config = new SimpleConfig();

      String json = LoggingJsonMapper.toMaskedJsonRepresentation(config);

      assertThat(json).startsWith("{").endsWith("}");
    }
  }
}
