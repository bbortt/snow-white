/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.quality.gate;

import lombok.Getter;

@Getter
public enum OpenApiCriteria {
  PATH_COVERAGE(
    "Path Coverage",
    "Every path defined in the OpenAPI specification has been called. This is a subset of `HTTP_METHOD_COVERAGE`."
  ),
  HTTP_METHOD_COVERAGE(
    "HTTP Method Coverage",
    "Each HTTP method (`GET`, `POST`, `PUT`, `DELETE`, etc.) for each path has been tested."
  ),
  ERROR_RESPONSE_CODE_COVERAGE(
    "Error Response Code Coverage",
    "Each documented error response code for each endpoint is tested. This is a subset of `RESPONSE_CODE_COVERAGE`."
  ),
  RESPONSE_CODE_COVERAGE(
    "Response Code Coverage",
    "Each documented response code for each endpoint is tested."
  ),
  REQUIRED_PARAMETER_COVERAGE(
    "Required Parameter Coverage",
    "Each required parameter (in path, query) has been tested with valid values. This is a subset of `PARAMETER_COVERAGE`."
  ),
  PARAMETER_COVERAGE(
    "Parameter Coverage",
    "Each parameter (in path, query) has been tested with valid values."
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

  private final String label;
  private final String description;

  OpenApiCriteria(String label, String description) {
    this.label = label;
    this.description = description;
  }
}
