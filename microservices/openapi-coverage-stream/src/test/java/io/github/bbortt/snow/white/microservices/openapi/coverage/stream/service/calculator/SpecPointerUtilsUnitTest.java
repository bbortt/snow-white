/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.toPathItemPointer;
import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class SpecPointerUtilsUnitTest {

  private static final String OPENAPI_DOCUMENT = """
  {
    "paths": {
      "/pung/{message}": { "get": {} },
      "/tilde~name": { "get": {} }
    }
  }
  """;

  @Nested
  class ToPathItemPointerTest {

    @Test
    void shouldEscapeSlashesOfTheTemplatedPath() {
      var result = toPathItemPointer("/pung/{message}");

      assertThat(result).isEqualTo("/paths/~1pung~1{message}");
    }

    @Test
    void shouldEscapeTildeBeforeSlash() {
      var result = toPathItemPointer("/tilde~name");

      assertThat(result).isEqualTo("/paths/~1tilde~0name");
    }

    @Test
    @VerifiesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
    void shouldResolveAgainstTheDocumentItAddresses() {
      JsonNode document = JsonMapper.shared().readTree(OPENAPI_DOCUMENT);

      assertThat(document.at(toPathItemPointer("/pung/{message}")))
        .isNotNull()
        .isSameAs(document.get("paths").get("/pung/{message}"));
      assertThat(document.at(toPathItemPointer("/tilde~name")))
        .isNotNull()
        .isSameAs(document.get("paths").get("/tilde~name"));
    }

    @Test
    void shouldNotResolveWhenTheTemplatedPathIsLeftUnescaped() {
      JsonNode document = JsonMapper.shared().readTree(OPENAPI_DOCUMENT);

      assertThat(
        document.at("/paths//pung/{message}").isMissingNode()
      ).isTrue();
    }
  }
}
