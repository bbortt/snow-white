/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception;

import static java.lang.String.format;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;

public class OpenApiNotIndexedException extends Exception {

  public OpenApiNotIndexedException(ApiInformation apiInformation) {
    super(
      format(
        "OpenApi identifier not indexed: { \"serviceName\": \"%s\",\"apiName\": \"%s\",\"apiVersion\": \"%s\" }",
        apiInformation.getServiceName(),
        apiInformation.getApiName(),
        apiInformation.getApiVersion()
      )
    );
  }
}
