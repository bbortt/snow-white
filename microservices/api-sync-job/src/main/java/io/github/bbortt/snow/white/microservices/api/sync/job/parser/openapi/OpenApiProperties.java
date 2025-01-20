package io.github.bbortt.snow.white.microservices.api.sync.job.parser.openapi;

import io.github.bbortt.snow.white.microservices.api.sync.job.parser.ApiProperty;
import lombok.Getter;

@Getter
public enum OpenApiProperties implements ApiProperty {
  OAS_INFO_TITLE("oas.info.title", false),
  OAS_INFO_VERSION("oas.info.version", true),

  OAS_TYPE("oas.type", true);

  private final String propertyName;
  private final boolean required;

  OpenApiProperties(String propertyName, boolean required) {
    this.propertyName = propertyName;
    this.required = required;
  }
}
