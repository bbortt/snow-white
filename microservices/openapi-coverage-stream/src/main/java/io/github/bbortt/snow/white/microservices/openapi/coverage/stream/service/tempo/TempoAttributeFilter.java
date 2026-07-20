/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.tempo;

import static io.github.bbortt.snow.white.commons.event.dto.AttributeFilter.STRING_OPERANDS;

import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;

public record TempoAttributeFilter(AttributeFilter baseAttributeFilter) {
  public String toTraceQLString() {
    return "span." + getKey() + " = " + getValue();
  }

  public String getKey() {
    return baseAttributeFilter.key();
  }

  private String getValue() {
    if (STRING_OPERANDS.contains(baseAttributeFilter.operator())) {
      return "\"" + baseAttributeFilter.value() + "\"";
    }

    return baseAttributeFilter.value();
  }
}
