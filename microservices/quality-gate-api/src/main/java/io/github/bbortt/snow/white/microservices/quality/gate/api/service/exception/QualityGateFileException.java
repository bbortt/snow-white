/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception;

import static java.lang.String.format;

import org.jspecify.annotations.Nullable;

/**
 * Thrown when a configured quality-gate file cannot be turned into a gate set. Unchecked and
 * deliberately not caught on the startup path: it has to fail the context, because the alternative
 * is interpreting an unreadable file as an empty gate set and deleting every gate the instance has.
 */
public class QualityGateFileException extends RuntimeException {

  public QualityGateFileException(String location, String problem) {
    this(location, problem, null);
  }

  public QualityGateFileException(
    String location,
    String problem,
    @Nullable Throwable cause
  ) {
    super(
      format("Quality-Gate file '%s' cannot be used: %s!", location, problem),
      cause
    );
  }
}
