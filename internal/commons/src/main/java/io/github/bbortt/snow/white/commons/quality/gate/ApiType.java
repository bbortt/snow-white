/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.quality.gate;

import static java.util.Arrays.stream;
import static java.util.Objects.isNull;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Getter
public enum ApiType {
  UNSPECIFIED((short) 0),
  ASYNCAPI((short) 1),
  OPENAPI((short) 2),
  GRAPHQL((short) 3);

  final short val;

  ApiType(short val) {
    this.val = val;
  }

  public static ApiType apiType(@Nullable String typeName) {
    if (isNull(typeName)) {
      return UNSPECIFIED;
    }

    return ApiType.valueOf(typeName.toUpperCase());
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
