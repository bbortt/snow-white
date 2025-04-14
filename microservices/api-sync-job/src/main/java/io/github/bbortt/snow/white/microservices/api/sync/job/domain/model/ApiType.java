/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.domain.model;

import lombok.Getter;

@Getter
public enum ApiType {
  UNSPECIFIED(0),
  OPENAPI(1),
  ASYNCAPI(2);

  final int val;

  ApiType(int val) {
    this.val = val;
  }

  public static ApiType apiType(String typeName) {
    return ApiType.valueOf(typeName.toUpperCase());
  }
}
