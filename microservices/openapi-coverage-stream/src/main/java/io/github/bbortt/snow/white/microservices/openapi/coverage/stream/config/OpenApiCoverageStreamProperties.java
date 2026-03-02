/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties.PREFIX;

import io.github.bbortt.snow.white.commons.DefaultFilteringProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ConfigurationProperties(prefix = PREFIX)
@Configuration(proxyBeanMethods = false)
public class OpenApiCoverageStreamProperties {

  public static final String PREFIX = "snow.white.openapi.coverage.stream";

  private String calculationRequestTopic;

  private String openapiCalculationResponseTopic;

  private Boolean initTopics = false;

  private final ApiIndexProperties apiIndex = new ApiIndexProperties();
  private final FilteringProperties filtering = new FilteringProperties();

  @Getter
  @Setter
  public static class ApiIndexProperties {

    public static final String BASE_URL_PROPERTY_NAME =
      PREFIX + ".api-index.base-url";

    private String baseUrl;
  }

  @Getter
  @Setter
  public static class FilteringProperties extends DefaultFilteringProperties {}
}
