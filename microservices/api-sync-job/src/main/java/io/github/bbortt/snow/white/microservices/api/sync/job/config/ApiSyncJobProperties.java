/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties.PREFIX;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode.GRACEFUL;

import io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ConfigurationProperties(PREFIX)
@Configuration(proxyBeanMethods = false)
public class ApiSyncJobProperties {

  public static final String PREFIX = "snow.white.api.sync.job";

  private @NonNull Integer maxParallelSyncTasks = 3;
  private @NonNull Integer workQueueCapacity = 30;

  private final @NonNull ApiIndexProperties apiIndex = new ApiIndexProperties();
  private final @NonNull ArtifactoryProperties artifactory =
    new ArtifactoryProperties();

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

    public static final String PREFIX =
      ApiSyncJobProperties.PREFIX + ".artifactory";
    public static final String BASE_URL_PROPERTY_NAME = PREFIX + ".base-url";
    public static final String ACCESS_TOKEN_PROPERTY_NAME =
      PREFIX + ".access-token";
    public static final String REPOSITORY_PROPERTY_NAME =
      PREFIX + ".repository";

    private String baseUrl;
    private String accessToken;
    private String repository;

    private String customApiNameJsonPath = "info.title";
    private String customApiVersionJsonPath = "info.version";
    private String customServiceNameJsonPath = "info.extensions.x-service-name";

    private ParsingMode parsingMode = GRACEFUL;
  }
}
