/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding.MAX_SPEC_POINTER_BYTES;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.escape;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static lombok.AccessLevel.PRIVATE;
import static org.springframework.util.CollectionUtils.isEmpty;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.List;
import java.util.Map;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Turns the pointer a verdict carried into the pointer a finding is identified by.
 * <p>
 * A verdict's pointer is relative to the response schema the document declares; a finding's is
 * absolute in the document. Appending the one to the other is the whole translation - but only as
 * far as the appended part addresses something the document actually declares, which an inline
 * schema does and a {@code $ref} to a component does not, since nothing dereferences the document
 * before it is measured.
 * <p>
 * So the pointer walks back to its nearest resolvable ancestor rather than being dropped: an
 * imperfect translation costs a finding's precision, never the finding that the response did not
 * conform.
 * <p>
 * That walk is the ordinary path, not a fallback for an unusual document. Most specifications
 * {@code $ref} their response schemas, and such a schema carries only its reference and no walkable
 * node, so every pointer on the pair resolves nowhere and every violation of it is published at the
 * response entry. Callers grouping by the resolved pointer therefore see one finding per pair in the
 * common case, not one per violation.
 */
@NoArgsConstructor(access = PRIVATE)
@RealizesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
final class ViolationPointerResolver {

  /**
   * The response entry's pointer, the media type whose schema the violation resolves in, and as
   * much of the violation's pointer as resolves into that schema - and the response entry's own
   * pointer where none of it does.
   * <p>
   * The media type is part of the published pointer because the schema is declared under it: a
   * pointer that skipped {@code /content/<mediaType>/schema} would address no node of the document,
   * and two media types declaring the same property could not be told apart. A verdict names no
   * media type of its own, so where several declare a schema the violation resolves in, the one the
   * document states first is published.
   */
  static @NonNull String resolve(
    @NonNull String responseEntryPointer,
    @Nullable ApiResponse response,
    @NonNull String violationPointer
  ) {
    var tokens = tokensOf(violationPointer);

    for (var depth = tokens.size(); depth > 0; depth--) {
      var prefix = tokens.subList(0, depth);
      var mediaType = mediaTypeDeclaring(response, prefix);

      if (isNull(mediaType)) {
        continue;
      }

      var pointer =
        responseEntryPointer +
        "/content/" +
        escape(mediaType) +
        "/schema/" +
        String.join("/", prefix);

      if (isStorable(pointer)) {
        return pointer;
      }
    }

    return responseEntryPointer;
  }

  /**
   * Whether the pointer fits the length a finding is stored under.
   * <p>
   * A violation pointer is a third party's value bounded on its own, and appending it to a pointer
   * this service derived is the one place the two lengths meet. Over the bound the walk continues
   * one token further up rather than publishing a finding no report can store - the response entry
   * pointer, which this service derived alone, is the floor either way.
   */
  private static boolean isStorable(String pointer) {
    return pointer.getBytes(UTF_8).length <= MAX_SPEC_POINTER_BYTES;
  }

  /**
   * The tokens of a JSON Pointer, still escaped, or none where the value is not one. A pointer has
   * to start with {@code /}; anything else was never a pointer, and is read as naming no node
   * rather than as naming the root.
   * <p>
   * Escaped because the tokens are appended to a pointer again: unescaping them here would publish
   * a finding whose pointer addresses a different node than the one that resolved, for any
   * property whose name contains a {@code /}.
   */
  private static List<String> tokensOf(String violationPointer) {
    if (!violationPointer.startsWith("/")) {
      return List.of();
    }

    return List.of(violationPointer.substring(1).split("/", -1));
  }

  /**
   * A pointer token as the key it addresses.
   */
  private static String unescape(String token) {
    return token.replace("~1", "/").replace("~0", "~");
  }

  /**
   * The first media type of the response whose schema the tokens address, or {@code null} where no
   * schema the response declares does.
   */
  private static @Nullable String mediaTypeDeclaring(
    @Nullable ApiResponse response,
    List<String> tokens
  ) {
    if (isNull(response) || isNull(response.getContent())) {
      return null;
    }

    return response
      .getContent()
      .entrySet()
      .stream()
      .filter(
        entry ->
          nonNull(entry.getValue()) &&
          resolves(entry.getValue().getSchema(), tokens)
      )
      .map(Map.Entry::getKey)
      .findFirst()
      .orElse(null);
  }

  /**
   * Whether the tokens address a node of the given schema.
   * <p>
   * Only the keywords a violation pointer can name are walked. An unrecognised keyword resolves to
   * nothing, which sends the caller one token further up - the same outcome as a keyword that is
   * recognised but absent, and the reason this does not need to know every keyword JSON Schema has.
   */
  private static boolean resolves(
    @Nullable Schema<?> schema,
    List<String> tokens
  ) {
    if (isNull(schema)) {
      return false;
    }

    if (tokens.isEmpty()) {
      return true;
    }

    var token = tokens.getFirst();
    var rest = tokens.subList(1, tokens.size());

    return switch (token) {
      case "properties" -> resolvesInProperty(schema, rest);
      case "items" -> resolves(schema.getItems(), rest);
      case "additionalProperties" -> resolves(
        additionalPropertiesSchemaOf(schema),
        rest
      );
      case "allOf" -> resolvesInComposition(schema.getAllOf(), rest);
      case "anyOf" -> resolvesInComposition(schema.getAnyOf(), rest);
      case "oneOf" -> resolvesInComposition(schema.getOneOf(), rest);
      case "required" -> rest.isEmpty() && !isEmpty(schema.getRequired());
      default -> false;
    };
  }

  private static boolean resolvesInProperty(
    Schema<?> schema,
    List<String> tokens
  ) {
    if (tokens.isEmpty() || isNull(schema.getProperties())) {
      return false;
    }

    return resolves(
      schema.getProperties().get(unescape(tokens.getFirst())),
      tokens.subList(1, tokens.size())
    );
  }

  /**
   * One branch of a composition, addressed by position the way the document states it. A token that
   * is not an index of that list resolves to nothing.
   */
  private static boolean resolvesInComposition(
    @Nullable List<Schema> branches,
    List<String> tokens
  ) {
    if (isEmpty(branches) || tokens.isEmpty()) {
      return false;
    }

    int index;
    try {
      index = Integer.parseInt(tokens.getFirst());
    } catch (NumberFormatException _) {
      return false;
    }

    if (index < 0 || index >= branches.size()) {
      return false;
    }

    return resolves(branches.get(index), tokens.subList(1, tokens.size()));
  }

  /**
   * {@code additionalProperties} is a schema or a boolean in OpenAPI's model, and only the schema
   * form has nodes below it.
   */
  private static @Nullable Schema<?> additionalPropertiesSchemaOf(
    Schema<?> schema
  ) {
    return schema.getAdditionalProperties() instanceof
      Schema<?> additionalProperties
      ? additionalProperties
      : null;
  }
}
