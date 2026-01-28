/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.exception;

import tools.jackson.core.JacksonException;

public class ApiCatalogException extends JacksonException {

  public ApiCatalogException(String message) {
    super(message);
  }

  public ApiCatalogException(String message, Throwable cause) {
    super(message, cause);
  }
}
