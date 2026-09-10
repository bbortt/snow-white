/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ApiReferenceMapperUnitTest {

  private ApiReferenceMapper fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiReferenceMapperImpl();
  }

  @Nested
  class EmptyStringToNullTest {

    public static Stream<String> emptyOrNullStringIsNull() {
      return Stream.of(null, "", " ");
    }

    @MethodSource
    @ParameterizedTest
    void emptyOrNullStringIsNull(String value) {
      assertThat(fixture.emptyStringToNull(value)).isNull();
    }

    @Test
    void valueStringIsValue() {
      var value = "some-string";

      assertThat(fixture.emptyStringToNull(value)).isEqualTo(value);
    }
  }
}
