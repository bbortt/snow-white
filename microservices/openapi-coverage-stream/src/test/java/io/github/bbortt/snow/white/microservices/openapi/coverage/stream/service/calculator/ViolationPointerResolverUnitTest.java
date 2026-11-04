/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ViolationPointerResolverUnitTest {

  private static final String RESPONSE_ENTRY_POINTER =
    "/paths/~1pets/get/responses/200";

  /**
   * The schema of the response's {@code application/json} entry, as the document addresses it: a
   * violation's pointer is relative to that node, not to the response entry.
   */
  private static String jsonSchema(String relativePointer) {
    return (
      RESPONSE_ENTRY_POINTER +
      "/content/application~1json/schema" +
      relativePointer
    );
  }

  private static ApiResponse responseWith(Schema<?> schema) {
    return new ApiResponse().content(
      new Content().addMediaType(
        "application/json",
        new MediaType().schema(schema)
      )
    );
  }

  private static Schema<?> objectWithId() {
    return new ObjectSchema().addProperty("id", new StringSchema());
  }

  @Nested
  class ResolveTest {

    @Test
    void shouldAppendPointer_whenItFullyResolves() {
      var result = ViolationPointerResolver.resolve(
        RESPONSE_ENTRY_POINTER,
        responseWith(objectWithId()),
        "/properties/id"
      );

      assertThat(result).isEqualTo(jsonSchema("/properties/id"));
    }

    @Test
    void shouldFallBackToLongestResolvingPrefix_whenLastTokenDoesNotResolve() {
      var result = ViolationPointerResolver.resolve(
        RESPONSE_ENTRY_POINTER,
        responseWith(objectWithId()),
        "/properties/id/properties/nope"
      );

      assertThat(result).isEqualTo(jsonSchema("/properties/id"));
    }

    /**
     * The floor of the walk, reached by each way the document can decline the pointer: a node it
     * does not declare, a response it declares nothing about, and no response at all.
     */
    @MethodSource("unresolvableViolations")
    @ParameterizedTest
    void shouldReturnEntryPointer_whenThePointerResolvesInNoSchema(
      @Nullable ApiResponse response,
      String violationPointer
    ) {
      var result = ViolationPointerResolver.resolve(
        RESPONSE_ENTRY_POINTER,
        response,
        violationPointer
      );

      assertThat(result).isEqualTo(RESPONSE_ENTRY_POINTER);
    }

    private static Stream<Arguments> unresolvableViolations() {
      return Stream.of(
        arguments(responseWith(objectWithId()), "/properties/nope"),
        arguments(null, "/properties/id"),
        arguments(new ApiResponse(), "/properties/id")
      );
    }

    @Test
    void shouldReturnEntryPointer_whenPointerDoesNotStartWithSlash() {
      var result = ViolationPointerResolver.resolve(
        RESPONSE_ENTRY_POINTER,
        responseWith(objectWithId()),
        "properties/id"
      );

      assertThat(result).isEqualTo(RESPONSE_ENTRY_POINTER);
    }

    @Test
    void shouldResolveRequired_whenSchemaDeclaresRequiredFields() {
      var schema = objectWithId().addRequiredItem("id");

      var result = ViolationPointerResolver.resolve(
        RESPONSE_ENTRY_POINTER,
        responseWith(schema),
        "/required"
      );

      assertThat(result).isEqualTo(jsonSchema("/required"));
    }

    @Test
    void shouldNotResolveRequired_whenSchemaDeclaresNone() {
      var result = ViolationPointerResolver.resolve(
        RESPONSE_ENTRY_POINTER,
        responseWith(objectWithId()),
        "/required"
      );

      assertThat(result).isEqualTo(RESPONSE_ENTRY_POINTER);
    }

    @Test
    void shouldResolveThroughArrayItems() {
      var schema = new ArraySchema().items(
        new ObjectSchema().addProperty("name", new StringSchema())
      );

      var result = ViolationPointerResolver.resolve(
        RESPONSE_ENTRY_POINTER,
        responseWith(schema),
        "/items/properties/name"
      );

      assertThat(result).isEqualTo(jsonSchema("/items/properties/name"));
    }

    @Test
    void shouldResolveIntoTheIndexedOneOfBranch() {
      var schema = new Schema<>()
        .addOneOfItem(objectWithId())
        .addOneOfItem(
          new ObjectSchema().addProperty("name", new StringSchema())
        );

      var result = ViolationPointerResolver.resolve(
        RESPONSE_ENTRY_POINTER,
        responseWith(schema),
        "/oneOf/1/properties/name"
      );

      assertThat(result).isEqualTo(jsonSchema("/oneOf/1/properties/name"));
    }

    @Test
    void shouldNotResolveOneOf_whenIndexIsOutOfRange() {
      var schema = new Schema<>()
        .addOneOfItem(objectWithId())
        .addOneOfItem(
          new ObjectSchema().addProperty("name", new StringSchema())
        );

      var result = ViolationPointerResolver.resolve(
        RESPONSE_ENTRY_POINTER,
        responseWith(schema),
        "/oneOf/5/properties/name"
      );

      assertThat(result).isEqualTo(RESPONSE_ENTRY_POINTER);
    }

    @Test
    void shouldNotResolveOneOf_whenIndexIsNotANumber() {
      var schema = new Schema<>()
        .addOneOfItem(objectWithId())
        .addOneOfItem(
          new ObjectSchema().addProperty("name", new StringSchema())
        );

      var result = ViolationPointerResolver.resolve(
        RESPONSE_ENTRY_POINTER,
        responseWith(schema),
        "/oneOf/notAnIndex/properties/name"
      );

      assertThat(result).isEqualTo(RESPONSE_ENTRY_POINTER);
    }

    /**
     * The tokens are appended again, so unescaping them would publish a pointer to a different node
     * than the one that resolved.
     */
    @Test
    void shouldKeepEscapeInReturnedPointer_whenPropertyNameContainsSlash() {
      var schema = new ObjectSchema().addProperty("a/b", new StringSchema());

      var result = ViolationPointerResolver.resolve(
        RESPONSE_ENTRY_POINTER,
        responseWith(schema),
        "/properties/a~1b"
      );

      assertThat(result).isEqualTo(jsonSchema("/properties/a~1b"));
    }

    @Test
    void shouldResolve_whenOnlyTheSecondMediaTypeResolves() {
      var response = new ApiResponse().content(
        new Content()
          .addMediaType(
            "text/plain",
            new MediaType().schema(new StringSchema())
          )
          .addMediaType(
            "application/json",
            new MediaType().schema(objectWithId())
          )
      );

      var result = ViolationPointerResolver.resolve(
        RESPONSE_ENTRY_POINTER,
        response,
        "/properties/id"
      );

      assertThat(result).isEqualTo(jsonSchema("/properties/id"));
    }

    /**
     * A verdict names no media type of its own, so the published pointer names the first one whose
     * schema the violation resolves in - which is the only reading the document supports.
     */
    @Test
    void shouldNameTheFirstMediaType_whenSeveralResolve() {
      var response = new ApiResponse().content(
        new Content()
          .addMediaType(
            "application/vnd.api+json",
            new MediaType().schema(objectWithId())
          )
          .addMediaType(
            "application/json",
            new MediaType().schema(objectWithId())
          )
      );

      var result = ViolationPointerResolver.resolve(
        RESPONSE_ENTRY_POINTER,
        response,
        "/properties/id"
      );

      assertThat(result).isEqualTo(
        RESPONSE_ENTRY_POINTER +
          "/content/application~1vnd.api+json/schema/properties/id"
      );
    }

    /**
     * A pointer no report can store is worse than an imprecise one: the finding is inserted into a
     * {@code VARCHAR(512)} column, and the whole calculation's result is lost where it does not fit.
     */
    @Test
    void shouldWalkUp_whenTheResolvedPointerWouldNotFitTheColumn() {
      var longProperty = "x".repeat(500);
      var schema = new ArraySchema().items(
        new ObjectSchema().addProperty(longProperty, new StringSchema())
      );

      var result = ViolationPointerResolver.resolve(
        RESPONSE_ENTRY_POINTER,
        responseWith(schema),
        "/items/properties/" + longProperty
      );

      assertThat(result).isEqualTo(jsonSchema("/items"));
    }

    @Test
    void shouldReturnEntryPointer_whenNoPrefixBothResolvesAndFits() {
      var longProperty = "x".repeat(500);
      var schema = new ObjectSchema().addProperty(
        longProperty,
        new StringSchema()
      );

      var result = ViolationPointerResolver.resolve(
        RESPONSE_ENTRY_POINTER,
        responseWith(schema),
        "/properties/" + longProperty
      );

      assertThat(result).isEqualTo(RESPONSE_ENTRY_POINTER);
    }
  }
}
