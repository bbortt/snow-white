/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.kafka.stream.exception;

public class SerializationException extends RuntimeException {

  public SerializationException(String message, Exception cause) {
    super(message, cause);
  }
}
