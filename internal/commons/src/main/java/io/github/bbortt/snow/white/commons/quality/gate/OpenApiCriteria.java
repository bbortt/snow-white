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
  REQUIRED_ERROR_FIELDS(
    "Required Error Fields Coverage",
    "Error responses include all required fields."
  );

  private final String label;
  private final String description;

  OpenApiCriteria(String label, String description) {
    this.label = label;
    this.description = description;
  }
}
