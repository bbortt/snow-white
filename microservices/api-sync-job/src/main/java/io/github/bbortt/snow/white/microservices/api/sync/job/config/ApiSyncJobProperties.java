/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static io.github.bbortt.snow.white.commons.PropertyUtils.assertRequiredProperties;
import static io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties.PREFIX;

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
@ConfigurationProperties(PREFIX)
public class ApiSyncJobProperties implements InitializingBean {

  public static final String PREFIX = "snow.white.api.sync.job";

  private final ApiIndexProperties apiIndex = new ApiIndexProperties();
  private final ArtifactoryProperties artifactory = new ArtifactoryProperties();

  @Override
  public void afterPropertiesSet() {
    Map<String, String> properties = new HashMap<>();
    properties.put(ApiIndexProperties.BASE_URL_PROPERTY_NAME, apiIndex.baseUrl);
    properties.put(
      ArtifactoryProperties.BASE_URL_PROPERTY_NAME,
      artifactory.baseUrl
    );

    assertRequiredProperties(properties);
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
  public static class ArtifactoryProperties {

    public static final String BASE_URL_PROPERTY_NAME =
      PREFIX + ".artifactory.base-url";

    private String baseUrl;
    private String accessToken;
  }
}
