/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.quality.gate;

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
    "Every path defined in the OpenAPI specification has been called. This is a subset of `HTTP_METHOD_COVERAGE`."
  ),
  HTTP_METHOD_COVERAGE(
    "HTTP Method Coverage",
    "Each HTTP method (`GET`, `POST`, `PUT`, `DELETE`, etc.) for each path has been tested."
  ),
  OPERATION_SUCCESS_COVERAGE(
    "Operation Success Coverage",
    "Each operation (unique path + HTTP method combination) has produced at least one successful (2xx) response. Complements `HTTP_METHOD_COVERAGE`, which only checks that an operation was called at all."
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
   * The containment forest: every criterion that is contained by another one, mapped to its
   * container. A criterion absent from this map is a root criterion. Containment means every target
   * the contained criterion judges is also a target the container judges, at the same spec pointer
   * — it carries no inheritance of calculation, of inclusion in a quality gate, or of coverage.
   *
   * <p>This declaration is the single source of the relation.
   * {@code pages/_pages/quality-gate-criteria.md} publishes it as a tree, and
   * {@code OpenApiCoverageCriteriaUnitTest} asserts the page and this map agree.
   *
   * <p>Filled from a static block rather than built around {@code Map.of}: an {@code EnumMap}
   * constructed from an empty map cannot infer its key type and would make "no criterion is
   * contained" a startup failure in every service instead of a plain, empty relation.
   */
  private static final Map<
    OpenApiCoverageCriteria,
    OpenApiCoverageCriteria
  > CONTAINED_BY = new EnumMap<>(OpenApiCoverageCriteria.class);

  static {
    CONTAINED_BY.putAll(
      Map.of(
        POSITIVE_RESPONSE_CODE_COVERAGE,
        RESPONSE_CODE_COVERAGE,
        ERROR_RESPONSE_CODE_COVERAGE,
        RESPONSE_CODE_COVERAGE,
        NO_UNDOCUMENTED_POSITIVE_RESPONSE_CODES,
        NO_UNDOCUMENTED_RESPONSE_CODES,
        NO_UNDOCUMENTED_ERROR_RESPONSE_CODES,
        NO_UNDOCUMENTED_RESPONSE_CODES,
        REQUIRED_PARAMETER_COVERAGE,
        PARAMETER_COVERAGE,
        OPTIONAL_PARAMETER_COVERAGE,
        PARAMETER_COVERAGE,
        PATH_COVERAGE,
        HTTP_METHOD_COVERAGE
      )
    );
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
    return CONTAINED_BY.get(this);
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
