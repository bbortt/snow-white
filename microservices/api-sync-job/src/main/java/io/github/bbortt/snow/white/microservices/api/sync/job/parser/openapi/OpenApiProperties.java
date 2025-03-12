package io.github.bbortt.snow.white.microservices.api.sync.job.parser.openapi;

import io.github.bbortt.snow.white.microservices.api.sync.job.parser.ApiProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OpenApiProperties implements ApiProperty {
  OAS_INFO_TITLE("oas.info.title", true),
  OAS_INFO_VERSION("oas.info.version", true),

  OAS_TYPE("oas.type", true);

  private final String propertyName;
  private final boolean required;
}
