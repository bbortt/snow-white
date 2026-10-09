/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.quality.gate;

import static java.util.Objects.isNull;
import static java.util.stream.Stream.iterate;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Getter
@RealizesArch(ArchTraceables.ARCH_016_CRITERIA_CONTAINMENT_DECLARED_ON_THE_ENUM)
public enum OpenApiCoverageCriteria {
  PATH_COVERAGE(
    "Path Coverage",
    "Every path defined in the OpenAPI specification has been called, by any HTTP method. Judged per path, where `HTTP_METHOD_COVERAGE` is judged per operation."
  ),
  HTTP_METHOD_COVERAGE(
    "HTTP Method Coverage",
    "Each HTTP method (`GET`, `POST`, `PUT`, `DELETE`, etc.) for each path has been tested."
  ),
  OPERATION_SUCCESS_COVERAGE(
    "Operation Success Coverage",
    "Each operation (unique path + HTTP method combination) has produced at least one successful (2xx) response, not merely a call. This is a stricter check than `HTTP_METHOD_COVERAGE`."
  ),
  ERROR_RESPONSE_CODE_COVERAGE(
    "Error Response Code Coverage",
    "Each documented error response code for each endpoint is tested. This is a subset of `RESPONSE_CODE_COVERAGE`."
  ),
  POSITIVE_RESPONSE_CODE_COVERAGE(
    "Positive Response Code Coverage",
    "Each documented positive (non-error) response code (1xx, 2xx, 3xx) for each endpoint is tested. This is a subset of `RESPONSE_CODE_COVERAGE`."
  ),
  RESPONSE_CODE_COVERAGE(
    "Response Code Coverage",
    "Each documented response code for each endpoint is tested."
  ),
  REQUIRED_PARAMETER_COVERAGE(
    "Required Parameter Coverage",
    "Each required parameter (in path, query) has been tested with valid values. This is a subset of `PARAMETER_COVERAGE`."
  ),
  OPTIONAL_PARAMETER_COVERAGE(
    "Optional Parameter Coverage",
    "Each optional (non-required) parameter (in path, query) has been tested with valid values. This is a subset of `PARAMETER_COVERAGE`."
  ),
  PARAMETER_COVERAGE(
    "Parameter Coverage",
    "Each parameter (in path, query) has been tested with valid values."
  ),
  CONTENT_TYPE_COVERAGE(
    "Content Type Coverage",
    "Each documented request body content type (e.g. `application/json`, `multipart/form-data`) for each endpoint has been exercised."
  ),
  REQUIRED_ERROR_FIELDS_COVERAGE(
    "Required Error Fields Coverage",
    "Error responses include all required fields."
  ),
  NO_UNDOCUMENTED_RESPONSE_CODES(
    "All Response Codes must be Specified",
    "All response codes (including errors) that occurred must be documented in the OpenAPI specification."
  ),
  NO_UNDOCUMENTED_ERROR_RESPONSE_CODES(
    "All Error Response Codes must be Specified",
    "All error response codes that occurred must be documented in the OpenAPI specification. This is a subset of `NO_UNDOCUMENTED_RESPONSE_CODES`."
  ),
  NO_UNDOCUMENTED_POSITIVE_RESPONSE_CODES(
    "All Non-Erroneous Response Codes must be Specified",
    "All response codes that occurred and are not being considered errors (0 - 399) must be documented in the OpenAPI specification. This is a subset of `NO_UNDOCUMENTED_RESPONSE_CODES`."
  );

  /**
   * The form a declared containment takes. A criterion is judged on two things — the set of targets
   * it selects and the check it applies to them — and containment requires the contained criterion
   * to be strictly narrower on at least one of them while matching on the other. Two criteria
   * identical on both contain neither the other, which is what keeps the relation directional and a
   * forest without a separate rule saying so.
   */
  public enum ContainmentForm {
    /**
     * The same check over a strictly smaller set of targets, at the same spec pointer.
     */
    SUBSET,

    /**
     * A strictly stronger check over the same set of targets, at the same spec pointer.
     */
    STRENGTH,
  }

  private record Containment(
    OpenApiCoverageCriteria container,
    ContainmentForm form
  ) {}

  /**
   * The containment forest: every criterion that is contained by another one, mapped to its
   * container and the form of the containment. A criterion absent from this map is a root
   * criterion. Containment means the contained criterion judges at the same spec pointer as its
   * container and is strictly narrower in one of two ways: {@link ContainmentForm#SUBSET}, the same
   * check over a strictly smaller set of targets, or {@link ContainmentForm#STRENGTH}, a strictly
   * stronger check over the same set of targets. It carries no inheritance of calculation, of
   * inclusion in a quality gate, or of coverage.
   *
   * <p>In particular no coverage implication rides along this relation in a fixed direction. Under
   * {@code SUBSET} a container at full coverage implies its contained criteria at full coverage —
   * full {@code RESPONSE_CODE_COVERAGE} is full {@code ERROR_RESPONSE_CODE_COVERAGE}. Under {@code
   * STRENGTH} the implication runs the other way: full {@code OPERATION_SUCCESS_COVERAGE} implies
   * full {@code HTTP_METHOD_COVERAGE}, while full method coverage says only that every operation
   * was called, not that any of them succeeded. One relation, two directions of implication, so
   * neither direction may be read off an edge.
   *
   * <p>This declaration is the single source of the relation.
   * {@code pages/_pages/quality-gate-criteria.md} publishes it as a tree, and
   * {@code OpenApiCoverageCriteriaUnitTest} asserts the page and this map agree.
   *
   * <p>{@code PATH_COVERAGE} is deliberately absent under either form: full
   * {@code HTTP_METHOD_COVERAGE} does imply full path coverage, but the two judge different targets
   * at different pointers — a path item and an operation within it — so neither the target sets
   * match nor is one a subset of the other. That implication between their ratios is a separate,
   * documented fact; it is not containment, and a waiver on an operation must not reach the path
   * around it.
   *
   * <p>Filled from a static block rather than built around {@code Map.of}: an {@code EnumMap}
   * constructed from an empty map cannot infer its key type and would make "no criterion is
   * contained" a startup failure in every service instead of a plain, empty relation.
   */
  private static final Map<OpenApiCoverageCriteria, Containment> CONTAINED_BY =
    new EnumMap<>(OpenApiCoverageCriteria.class);

  static {
    CONTAINED_BY.putAll(
      Map.of(
        POSITIVE_RESPONSE_CODE_COVERAGE,
        subsetOf(RESPONSE_CODE_COVERAGE),
        ERROR_RESPONSE_CODE_COVERAGE,
        subsetOf(RESPONSE_CODE_COVERAGE),
        NO_UNDOCUMENTED_POSITIVE_RESPONSE_CODES,
        subsetOf(NO_UNDOCUMENTED_RESPONSE_CODES),
        NO_UNDOCUMENTED_ERROR_RESPONSE_CODES,
        subsetOf(NO_UNDOCUMENTED_RESPONSE_CODES),
        REQUIRED_PARAMETER_COVERAGE,
        subsetOf(PARAMETER_COVERAGE),
        OPTIONAL_PARAMETER_COVERAGE,
        subsetOf(PARAMETER_COVERAGE),
        OPERATION_SUCCESS_COVERAGE,
        stricterThan(HTTP_METHOD_COVERAGE)
      )
    );
  }

  private static Containment subsetOf(OpenApiCoverageCriteria container) {
    return new Containment(container, ContainmentForm.SUBSET);
  }

  private static Containment stricterThan(OpenApiCoverageCriteria container) {
    return new Containment(container, ContainmentForm.STRENGTH);
  }

  private final String label;
  private final String description;

  OpenApiCoverageCriteria(String label, String description) {
    this.label = label;
    this.description = description;
  }

  /**
   * The criterion containing this one, or {@code null} if this is a root criterion.
   */
  public @Nullable OpenApiCoverageCriteria getContainedBy() {
    var containment = CONTAINED_BY.get(this);

    return isNull(containment) ? null : containment.container();
  }

  /**
   * The form in which {@link #getContainedBy()} contains this one, or {@code null} if this is a
   * root criterion. Consumed by the documentation views, which state the two forms in different
   * words because "subset" is false of a {@link ContainmentForm#STRENGTH} edge.
   */
  public @Nullable ContainmentForm getContainmentForm() {
    var containment = CONTAINED_BY.get(this);

    return isNull(containment) ? null : containment.form();
  }

  /**
   * Every criterion containing this one, nearest container first, up to its root. Empty for a root
   * criterion.
   *
   * <p>Bounded by the number of declared containments, which no chain through a forest can exceed: a
   * cycle edited into the declaration truncates the walk here rather than hanging the caller, and
   * {@code OpenApiCoverageCriteriaUnitTest} is what reports it.
   */
  public List<OpenApiCoverageCriteria> getContainingCriteria() {
    return iterate(
      getContainedBy(),
      Objects::nonNull,
      OpenApiCoverageCriteria::getContainedBy
    )
      .limit(CONTAINED_BY.size())
      .toList();
  }
}
