/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.quality.gate;

import static java.util.Arrays.stream;
import static java.util.Locale.ROOT;
import static java.util.Objects.isNull;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Getter
public enum ApiType {
  UNSPECIFIED((short) 0, false),
  ASYNCAPI((short) 1, false),
  OPENAPI((short) 2, true),
  GRAPHQL((short) 3, false);

  final short val;

  /**
   * Whether a coverage-calculating stream service consumes calculation requests of this type. Flip
   * this on in the same change that deploys the calculator, because a request for a type nothing
   * consumes is never answered.
   */
  final boolean coverageCalculatorAvailable;

  ApiType(short val, boolean coverageCalculatorAvailable) {
    this.val = val;
    this.coverageCalculatorAvailable = coverageCalculatorAvailable;
  }

  public static ApiType apiType(@Nullable String typeName) {
    if (isNull(typeName)) {
      return UNSPECIFIED;
    }

    return ApiType.valueOf(typeName.toUpperCase(ROOT));
  }

  public static ApiType apiType(@Nullable Short val) {
    if (isNull(val)) {
      return UNSPECIFIED;
    }

    return stream(ApiType.values())
      .filter(apiType -> apiType.getVal() == val)
      .findFirst()
      .orElse(UNSPECIFIED);
  }
}
