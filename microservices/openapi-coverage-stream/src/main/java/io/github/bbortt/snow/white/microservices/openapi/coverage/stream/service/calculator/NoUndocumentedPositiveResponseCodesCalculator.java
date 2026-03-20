/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.NO_UNDOCUMENTED_POSITIVE_RESPONSE_CODES;
import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.toSet;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

/**
 * Calculator for the following criteria:
 * All response codes that occurred and are not being considered errors (0 - 399) must be documented in the OpenAPI specification.
 * This is a subset of {@link OpenApiCriteria#NO_UNDOCUMENTED_RESPONSE_CODES}.
 *
 * @see OpenApiCriteria#NO_UNDOCUMENTED_POSITIVE_RESPONSE_CODES
 */
@Slf4j
@Component
public class NoUndocumentedPositiveResponseCodesCalculator
  extends NoUndocumentedResponseCodesCalculator
{

  @Override
  protected @NonNull OpenApiCriteria getSupportedOpenApiCriteria() {
    return NO_UNDOCUMENTED_POSITIVE_RESPONSE_CODES;
  }

  @Override
  protected Set<String> filterObservedResponseCodes(
    Set<String> observedResponseCodes
  ) {
    return observedResponseCodes
      .stream()
      .filter(this::isPositiveResponseCode)
      .collect(toSet());
  }

  private boolean isPositiveResponseCode(String statusCode) {
    try {
      var code = HttpStatusCode.valueOf(parseInt(statusCode));
      return (
        code.is1xxInformational() ||
        code.is2xxSuccessful() ||
        code.is3xxRedirection()
      );
    } catch (NumberFormatException _) {
      // If it's not a valid number, exclude it
      return false;
    }
  }

  @Override
  protected @Nullable String getAdditionalInformationOrNull(
    @NonNull Set<String> undocumentedCodes
  ) {
    return super.getAdditionalInformationOrNull(
      "The following observed non-erroneous response codes are not documented in the OpenAPI specification: `%s`",
      undocumentedCodes
    );
  }
}
