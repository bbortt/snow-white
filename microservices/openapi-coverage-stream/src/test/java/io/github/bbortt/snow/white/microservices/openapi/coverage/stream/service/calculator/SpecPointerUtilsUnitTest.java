/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.toOperationPointer;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.toParameterPointer;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.toPathItemPointer;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.toRequestBodyContentPointer;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.toResponseEntryPointer;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.toResponsesPointer;
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
          "parameters": [
            { "name": "message", "in": "path" },
            { "name": "delay", "in": "query" }
          ],
          "requestBody": {
            "content": {
              "application/json": {},
              "application/vnd.pung+json;version=1": {}
            }
          },
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
  class ToParameterPointerTest {

    @Test
    void shouldAddressTheParameterByItsPositionInTheArray() {
      var result = toParameterPointer("GET_/pung/{message}", 1);

      assertThat(result).isEqualTo("/paths/~1pung~1{message}/get/parameters/1");
    }

    @Test
    @VerifiesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
    void shouldResolveAgainstTheDocumentItAddresses() {
      JsonNode document = JsonMapper.shared().readTree(OPENAPI_DOCUMENT);

      var parameters = document
        .get("paths")
        .get("/pung/{message}")
        .get("get")
        .get("parameters");

      assertThat(
        document.at(toParameterPointer("GET_/pung/{message}", 0))
      ).isSameAs(parameters.get(0));
      assertThat(
        document.at(toParameterPointer("GET_/pung/{message}", 1))
      ).isSameAs(parameters.get(1));
    }

    @Test
    @VerifiesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
    void shouldLandOnTheNodeDeclaringTheParameterTheFindingNames() {
      JsonNode document = JsonMapper.shared().readTree(OPENAPI_DOCUMENT);

      assertThat(
        document
          .at(toParameterPointer("GET_/pung/{message}", 0))
          .get("name")
          .asString()
      ).isEqualTo("message");
      assertThat(
        document
          .at(toParameterPointer("GET_/pung/{message}", 1))
          .get("name")
          .asString()
      ).isEqualTo("delay");
    }

    @Test
    void shouldNotResolveWhenThePositionIsPastTheEndOfTheArray() {
      JsonNode document = JsonMapper.shared().readTree(OPENAPI_DOCUMENT);

      assertThat(
        document
          .at(toParameterPointer("GET_/pung/{message}", 2))
          .isMissingNode()
      ).isTrue();
    }
  }

  @Nested
  class ToRequestBodyContentPointerTest {

    @Test
    void shouldEscapeTheSlashOfTheMediaType() {
      var result = toRequestBodyContentPointer(
        "GET_/pung/{message}",
        "application/json"
      );

      assertThat(result).isEqualTo(
        "/paths/~1pung~1{message}/get/requestBody/content/application~1json"
      );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
    void shouldResolveAgainstTheDocumentItAddresses() {
      JsonNode document = JsonMapper.shared().readTree(OPENAPI_DOCUMENT);

      var content = document
        .get("paths")
        .get("/pung/{message}")
        .get("get")
        .get("requestBody")
        .get("content");

      assertThat(
        document.at(
          toRequestBodyContentPointer("GET_/pung/{message}", "application/json")
        )
      ).isSameAs(content.get("application/json"));
      assertThat(
        document.at(
          toRequestBodyContentPointer(
            "GET_/pung/{message}",
            "application/vnd.pung+json;version=1"
          )
        )
      ).isSameAs(content.get("application/vnd.pung+json;version=1"));
    }

    @Test
    void shouldNotResolveWhenTheMediaTypeIsLeftUnescaped() {
      JsonNode document = JsonMapper.shared().readTree(OPENAPI_DOCUMENT);

      assertThat(
        document
          .at(
            "/paths/~1pung~1{message}/get/requestBody/content/application/json"
          )
          .isMissingNode()
      ).isTrue();
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

  @Nested
  class ToResponsesPointerTest {

    @Test
    void shouldStopAtTheResponsesMap() {
      var result = toResponsesPointer("GET_/pung/{message}");

      assertThat(result).isEqualTo("/paths/~1pung~1{message}/get/responses");
    }

    @Test
    @VerifiesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
    void shouldResolveAgainstTheDocumentItAddresses() {
      JsonNode document = JsonMapper.shared().readTree(OPENAPI_DOCUMENT);

      assertThat(
        document.at(toResponsesPointer("GET_/pung/{message}"))
      ).isSameAs(
        document.get("paths").get("/pung/{message}").get("get").get("responses")
      );
    }

    /**
     * The container is what an inverted criterion can name for a code the document never lists —
     * so it has to resolve where the entry below it would not.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
    void shouldResolveWhereAnUndocumentedEntryBelowItWouldNot() {
      JsonNode document = JsonMapper.shared().readTree(OPENAPI_DOCUMENT);

      assertThat(
        document.at(toResponsesPointer("GET_/pung/{message}")).isMissingNode()
      ).isFalse();
      assertThat(
        document
          .at(toResponseEntryPointer("GET_/pung/{message}", "418"))
          .isMissingNode()
      ).isTrue();
    }
  }

  @Nested
  class PathsPointerTest {

    @Test
    @VerifiesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
    void shouldResolveAgainstTheDocumentItAddresses() {
      JsonNode document = JsonMapper.shared().readTree(OPENAPI_DOCUMENT);

      assertThat(document.at(SpecPointerUtils.PATHS_POINTER)).isSameAs(
        document.get("paths")
      );
    }
  }
}
