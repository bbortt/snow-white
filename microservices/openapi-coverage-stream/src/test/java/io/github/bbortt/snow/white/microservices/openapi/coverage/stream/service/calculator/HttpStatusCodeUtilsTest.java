/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HttpStatusCodeUtilsTest {

  @Nested
  class IsErrorHttpStatusCodeTest {

    @ParameterizedTest(name = "{0} → false")
    @ValueSource(
      strings = { "100", "101", "200", "201", "204", "301", "302", "304" }
    )
    void returnsFalseForNonErrorCodes(String code) {
      assertThat(HttpStatusCodeUtils.isErrorHttpStatusCode(code)).isFalse();
    }

    @ParameterizedTest(name = "{0} → true")
    @ValueSource(strings = { "400", "401", "403", "404", "409", "422", "429" })
    void returnsTrueForClientErrorCodes(String code) {
      assertThat(HttpStatusCodeUtils.isErrorHttpStatusCode(code)).isTrue();
    }

    @ParameterizedTest(name = "{0} → true")
    @ValueSource(strings = { "500", "501", "502", "503", "504" })
    void returnsTrueForServerErrorCodes(String code) {
      assertThat(HttpStatusCodeUtils.isErrorHttpStatusCode(code)).isTrue();
    }

    @ParameterizedTest(name = "{0} → false")
    @ValueSource(strings = { "1XX", "2XX", "3XX", "1xx", "2xx", "3xx" })
    void returnsFalseForNonErrorWildcards(String pattern) {
      assertThat(HttpStatusCodeUtils.isErrorHttpStatusCode(pattern)).isFalse();
    }

    @ParameterizedTest(name = "{0} → true")
    @ValueSource(strings = { "4XX", "4xx", "5XX", "5xx" })
    void returnsTrueForErrorWildcards(String pattern) {
      assertThat(HttpStatusCodeUtils.isErrorHttpStatusCode(pattern)).isTrue();
    }

    @ParameterizedTest(name = "\"{0}\" → true")
    @ValueSource(strings = { "default", "DEFAULT", "Default", "dEfAuLt" })
    void returnsTrueForDefaultInAnyCasing(String value) {
      assertThat(HttpStatusCodeUtils.isErrorHttpStatusCode(value)).isTrue();
    }

    @ParameterizedTest(name = "\"{0}\" → false")
    @ValueSource(strings = { "ok", "error", "none", "XXX", "2xx-custom" })
    void returnsFalseForArbitraryStrings(String value) {
      assertThat(HttpStatusCodeUtils.isErrorHttpStatusCode(value)).isFalse();
    }
  }
}
