/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.exception;

import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.OpenApiService;

public class OpenApiNotIndexedException extends Exception {

  public OpenApiNotIndexedException(
    OpenApiService.OpenApiIdentifier openApiIdentifier
  ) {
    super("OpenApi identifier not indexed: " + openApiIdentifier);
  }
}
