/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception;

import static java.lang.String.join;
import static java.util.Objects.isNull;

import java.util.List;

public class UnparseableOpenApiException extends Exception {

  public UnparseableOpenApiException(List<String> messages) {
    super(
      "Unparsable OpenAPI" +
        ((isNull(messages) || messages.isEmpty())
          ? " detected"
          : ": " + join(", ", messages))
    );
  }
}
