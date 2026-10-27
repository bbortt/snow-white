/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.mcp.api.config;

import static io.github.bbortt.snow.white.microservices.mcp.api.config.McpApiProperties.PREFIX;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ConfigurationProperties(PREFIX)
@Configuration(proxyBeanMethods = false)
public class McpApiProperties {

  public static final String PREFIX = "snow.white.mcp.api";

  private final @NonNull ApiIndexProperties apiIndex = new ApiIndexProperties();
  private final @NonNull CriteriaProperties criteria = new CriteriaProperties();
  private final @NonNull ReportProperties report = new ReportProperties();

  @Getter
  @Setter
  public static class ApiIndexProperties {

    public static final String BASE_URL_PROPERTY_NAME =
      PREFIX + ".api-index.base-url";

    private String baseUrl;
  }

  @Getter
  @Setter
  public static class CriteriaProperties {

    public static final String BASE_URL_PROPERTY_NAME =
      PREFIX + ".criteria.base-url";

    private String baseUrl;
  }

  @Getter
  @Setter
  public static class ReportProperties {

    public static final String BASE_URL_PROPERTY_NAME =
      PREFIX + ".report.base-url";

    private String baseUrl;
  }
}
