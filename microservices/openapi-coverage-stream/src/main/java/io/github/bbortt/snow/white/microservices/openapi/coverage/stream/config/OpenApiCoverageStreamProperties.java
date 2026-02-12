/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config;

import static io.github.bbortt.snow.white.commons.PropertyUtils.assertRequiredProperties;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties.PREFIX;

import io.github.bbortt.snow.white.commons.DefaultFilteringProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = PREFIX)
public class OpenApiCoverageStreamProperties implements InitializingBean {

  public static final String PREFIX = "snow.white.openapi.coverage.stream";

  private String calculationRequestTopic;

  private String openapiCalculationResponseTopic;

  private Boolean initTopics = false;

  private final ApiIndexProperties apiIndex = new ApiIndexProperties();
  private final FilteringProperties filtering = new FilteringProperties();

  @Override
  public void afterPropertiesSet() {
    Map<String, String> fields = new HashMap<>();
    fields.put(ApiIndexProperties.BASE_URL_PROPERTY_NAME, apiIndex.baseUrl);
    fields.put(PREFIX + ".calculation-request-topic", calculationRequestTopic);
    fields.put(
      PREFIX + ".openapi-calculation-response-topic",
      openapiCalculationResponseTopic
    );

    assertRequiredProperties(fields);
  }

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
