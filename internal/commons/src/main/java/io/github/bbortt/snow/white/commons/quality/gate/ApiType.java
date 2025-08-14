/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.quality.gate;

import static java.util.Arrays.stream;

import lombok.Getter;

@Getter
public enum ApiType {
  UNSPECIFIED(0),
  ASYNCAPI(1),
  OPENAPI(2),
  GRAPHQL(3);

  final int val;

  ApiType(int val) {
    this.val = val;
  }

  public static ApiType apiType(String typeName) {
    return ApiType.valueOf(typeName.toUpperCase());
  }

  public static ApiType apiType(Integer val) {
    return stream(ApiType.values())
      .filter(apiType -> apiType.getVal() == val)
      .findFirst()
      .orElse(UNSPECIFIED);
  }
}
