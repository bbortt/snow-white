/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event.dto;

import static io.github.bbortt.snow.white.commons.event.dto.AttributeFilterOperator.STRING_EQUALS;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record AttributeFilter(
  String key,
  AttributeFilterOperator operator,
  String value
) {
  public static final List<AttributeFilterOperator> STRING_OPERANDS = List.of(
    STRING_EQUALS
  );

  public static Builder attributeFilters() {
    return new Builder();
  }

  public static class Builder {

    private final Set<AttributeFilter> attributeFilters = new HashSet<>();

    public Builder with(AttributeFilter attributeFilter) {
      attributeFilters.add(attributeFilter);
      return this;
    }

    public Set<AttributeFilter> build() {
      return attributeFilters;
    }
  }
}
