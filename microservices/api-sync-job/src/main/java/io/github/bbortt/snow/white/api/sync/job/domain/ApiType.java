package io.github.bbortt.snow.white.api.sync.job.domain;

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
