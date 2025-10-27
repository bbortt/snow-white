/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.influxdb;

import static io.github.bbortt.snow.white.commons.event.dto.AttributeFilter.STRING_OPERANDS;

import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;

public class FluxAttributeFilter {

  private final AttributeFilter baseAttributeFilter;
  private final String normalizedKey;

  public FluxAttributeFilter(AttributeFilter baseAttributeFilter) {
    this.baseAttributeFilter = baseAttributeFilter;
    this.normalizedKey = baseAttributeFilter.key().replace(".", "_");
  }

  public String toFluxString() {
    return (
      " |> filter(fn: (r) => r." +
      getNormalizedKey() +
      " " +
      baseAttributeFilter.operator().toFluxString() +
      " " +
      getValue() +
      ")"
    );
  }

  public String getKey() {
    return baseAttributeFilter.key();
  }

  public String getNormalizedKey() {
    return normalizedKey;
  }

  private String getValue() {
    if (STRING_OPERANDS.contains(baseAttributeFilter.operator())) {
      return "\"" + baseAttributeFilter.value() + "\"";
    }

    return baseAttributeFilter.value();
  }
}
