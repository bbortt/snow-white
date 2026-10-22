/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception;

import static java.lang.String.format;

/**
 * Signals that the configured tracing/telemetry backend (e.g. Grafana Tempo, InfluxDB) could not
 * be reached, or responded with a server error, after all retries were exhausted. This is a
 * disruption in third-party infrastructure, not a Snow-White defect - callers should surface
 * {@link #getMessage()} to end users as-is instead of any lower-level transport exception.
 */
public class TelemetryBackendUnavailableException extends Exception {

  public TelemetryBackendUnavailableException(
    String backendName,
    Throwable cause
  ) {
    super(
      format(
        "The tracing backend \"%s\" is temporarily unavailable. This is a disruption in the " +
          "connected tracing infrastructure, not in Snow-White itself. Please try again shortly.",
        backendName
      ),
      cause
    );
  }
}
