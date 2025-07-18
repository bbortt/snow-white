/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.mapper;

public class FailedToCopyFieldException extends RuntimeException {

  public FailedToCopyFieldException(String fieldName, Throwable cause) {
    super("Failed to copy field: " + fieldName, cause);
  }
}
