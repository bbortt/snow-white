/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.mapper;

import static java.lang.String.format;

public class OpenApiCriterionDoesNotExistException extends RuntimeException {

  public OpenApiCriterionDoesNotExistException(String criteriaName) {
    super(format("OpenApi Criterion '%s' does not exist!", criteriaName));
  }
}
