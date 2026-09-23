/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.toOperationPointer;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.toPathItemPointer;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.toResponseEntryPointer;
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
      "/pung/{message}": {
        "get": {
          "responses": {
            "200": {},
            "4XX": {},
            "default": {}
          }
        }
      },
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

  @Nested
  class ToOperationPointerTest {

    @Test
    void shouldLowercaseTheMethodTheOperationKeyCarriesUppercased() {
      var result = toOperationPointer("GET_/pung/{message}");

      assertThat(result).isEqualTo("/paths/~1pung~1{message}/get");
    }
  }

  @Nested
  class ToResponseEntryPointerTest {

    @Test
    void shouldPointAtTheResponseEntryBelowTheOperation() {
      var result = toResponseEntryPointer("GET_/pung/{message}", "404");

      assertThat(result).isEqualTo(
        "/paths/~1pung~1{message}/get/responses/404"
      );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
    void shouldResolveAgainstTheDocumentItAddresses() {
      JsonNode document = JsonMapper.shared().readTree(OPENAPI_DOCUMENT);

      var responses = document
        .get("paths")
        .get("/pung/{message}")
        .get("get")
        .get("responses");

      assertThat(
        document.at(toResponseEntryPointer("GET_/pung/{message}", "200"))
      ).isSameAs(responses.get("200"));
      assertThat(
        document.at(toResponseEntryPointer("GET_/pung/{message}", "4XX"))
      ).isSameAs(responses.get("4XX"));
      assertThat(
        document.at(toResponseEntryPointer("GET_/pung/{message}", "default"))
      ).isSameAs(responses.get("default"));
    }

    @Test
    void shouldNotResolveWhenTheMethodIsLeftUppercased() {
      JsonNode document = JsonMapper.shared().readTree(OPENAPI_DOCUMENT);

      assertThat(
        document
          .at("/paths/~1pung~1{message}/GET/responses/200")
          .isMissingNode()
      ).isTrue();
    }
  }
}
