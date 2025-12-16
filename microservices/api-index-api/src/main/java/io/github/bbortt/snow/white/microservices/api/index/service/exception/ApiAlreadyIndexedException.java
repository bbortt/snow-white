/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.service.exception;

import static java.lang.String.format;

import io.github.bbortt.snow.white.microservices.api.index.domain.model.ApiReference;

public class ApiAlreadyIndexedException extends Exception {

  public ApiAlreadyIndexedException(ApiReference apiReference) {
    super(
      format(
        "API { otelServiceName=\"%s\", apiName=\"%s\", apiVersion=\"%s\"} already indexed!",
        apiReference.getOtelServiceName(),
        apiReference.getApiName(),
        apiReference.getApiVersion()
      )
    );
  }
}
