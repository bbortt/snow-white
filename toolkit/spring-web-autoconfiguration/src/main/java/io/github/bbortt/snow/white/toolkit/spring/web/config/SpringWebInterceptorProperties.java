/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.spring.web.config;

import static io.github.bbortt.snow.white.toolkit.spring.web.config.SpringWebInterceptorProperties.PREFIX;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

@Getter
@Setter
@Configuration
@ConfigurationProperties(PREFIX)
public class SpringWebInterceptorProperties {

  static final String PREFIX = "io.github.bbortt.snow.white.toolkit.spring.web";

  private static final String DEFAULT_API_NAME_ATTRIBUTE = "api.name";
  private static final String DEFAULT_API_VERSION_ATTRIBUTE = "api.version";
  private static final String DEFAULT_OPERATION_ID_ATTRIBUTE =
    "openapi.operation.id";

  /**
   * Name of the attribute correlating an OTEL span with the name of the OpenAPI it presents.
   */
  private String apiNameAttribute = DEFAULT_API_NAME_ATTRIBUTE;

  /**
   * Name of the attribute correlating an OTEL span with the version of the OpenAPI it presents.
   */
  private String apiVersionAttribute = DEFAULT_API_VERSION_ATTRIBUTE;

  /**
   * Name of the attribute correlating an OTEL span with the name of the service implementing the OpenAPI.
   */
  private @Nullable String otelServiceNameAttribute;

  /**
   * Name of the attribute correlating an OTEL span with specific operation of an OpenAPI.
   */
  private String operationIdAttribute = DEFAULT_OPERATION_ID_ATTRIBUTE;
}
