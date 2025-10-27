/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RedisHashUtilsTest {

  @Nested
  class GenerateRedisApiInformationId {

    @Test
    void shouldGenerateId() {
      assertThat(
        RedisHashUtils.generateRedisApiInformationId(
          "otelServiceName",
          "apiName",
          "apiVersion"
        )
      ).isEqualTo("otelServiceName:apiName:apiVersion");
    }

    public static Stream<Arguments> shouldThrowExceptionOnNullishValues() {
      return Stream.of(
        arguments(null, "apiName", "apiVersion"),
        arguments("", "apiName", "apiVersion"),
        arguments("serviceName", null, "apiVersion"),
        arguments("serviceName", "", "apiVersion"),
        arguments("serviceName", "apiName", null),
        arguments("serviceName", "apiName", "")
      );
    }

    @MethodSource
    @ParameterizedTest
    void shouldThrowExceptionOnNullishValues(
      String serviceName,
      String apiName,
      String apiVersion
    ) {
      assertThatThrownBy(() ->
        RedisHashUtils.generateRedisApiInformationId(
          serviceName,
          apiName,
          apiVersion
        )
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("ID cannot be constructed from nullish values!");
    }
  }
}
