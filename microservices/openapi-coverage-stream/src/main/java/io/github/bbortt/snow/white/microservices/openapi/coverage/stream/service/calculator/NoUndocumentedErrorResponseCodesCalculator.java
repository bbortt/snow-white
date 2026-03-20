/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.NO_UNDOCUMENTED_ERROR_RESPONSE_CODES;
import static java.util.stream.Collectors.toSet;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Calculator for the following criteria:
 * All error response codes that occurred must be documented in the OpenAPI specification.
 * This is a subset of {@link OpenApiCriteria#NO_UNDOCUMENTED_RESPONSE_CODES}.
 *
 * @see OpenApiCriteria#NO_UNDOCUMENTED_ERROR_RESPONSE_CODES
 */
@Slf4j
@Component
public class NoUndocumentedErrorResponseCodesCalculator
  extends NoUndocumentedResponseCodesCalculator
{

  @Override
  protected @NonNull OpenApiCriteria getSupportedOpenApiCriteria() {
    return NO_UNDOCUMENTED_ERROR_RESPONSE_CODES;
  }

  @Override
  protected Set<String> filterObservedResponseCodes(
    Set<String> observedResponseCodes
  ) {
    return observedResponseCodes
      .stream()
      .filter(HttpStatusCodeUtils::isErrorHttpStatusCode)
      .collect(toSet());
  }

  @Override
  protected @Nullable String getAdditionalInformationOrNull(
    @NonNull Set<String> undocumentedCodes
  ) {
    return super.getAdditionalInformationOrNull(
      "The following observed error response codes are not documented in the OpenAPI specification: `%s`",
      undocumentedCodes
    );
  }
}
