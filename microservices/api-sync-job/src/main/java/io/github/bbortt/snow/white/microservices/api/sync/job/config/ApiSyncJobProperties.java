/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties.PREFIX;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode.GRACEFUL;
import static java.lang.String.format;
import static org.springframework.util.StringUtils.hasText;

import io.github.bbortt.snow.white.commons.testing.VisibleForTesting;
import io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode;
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

  private final BackstageProperties backstage = new BackstageProperties();
  private final ServiceInterfaceProperties serviceInterface =
    new ServiceInterfaceProperties();
  private final MinIOProperties minio = new MinIOProperties();

  @Override
  public void afterPropertiesSet() {
    if (
      hasText(serviceInterface.baseUrl) && !hasText(serviceInterface.indexUri)
    ) {
      var sirPrefix = PREFIX + ".service-interface";
      throw new IllegalArgumentException(
        format(
          "Both '%s.base-url' and '%s.index-uri' must be set!",
          sirPrefix,
          sirPrefix
        )
      );
    }

    if (
      hasText(backstage.baseUrl) &&
      !hasText(backstage.customVersionAnnotation) &&
      !hasText(minio.endpoint)
    ) {
      throw new IllegalArgumentException(
        "Backstage API entities can only be parsed if a MinIO storage is configured!"
      );
    } else if (hasText(minio.endpoint) && !hasText(minio.bucketName)) {
      throw new IllegalArgumentException(
        "Please configure a MinIO bucket name!"
      );
    }
  }

  @Getter
  @Setter
  public static class MinIOProperties {

    private String endpoint;
    private String bucketName;

    private String accessKey;
    private String secretKey;

    private Boolean initBucket = false;
  }

  @Getter
  @Setter
  public static class BackstageProperties {

    public static final String BASE_URL_PROPERTY_NAME =
      PREFIX + ".backstage.base-url";

    private String baseUrl;

    private String customVersionAnnotation;

    private String customApiNameJsonPath = "info.title";
    private String customApiVersionJsonPath = "info.version";
    private String customServiceNameJsonPath = "info.x-service-name";

    private ParsingMode parsingMode = GRACEFUL;
  }

  @Getter
  @Setter
  public static class ServiceInterfaceProperties {

    public static final String BASE_URL_PROPERTY_NAME =
      PREFIX + ".service-interface.base-url";

    private static final String DEFAULT_OTEL_SERVICE_NAME_PROPERTY =
      "oas.info.x-service-name";

    private String baseUrl;
    private String indexUri;

    private String apiNameProperty;
    private String apiVersionProperty;
    private String serviceNameProperty = DEFAULT_OTEL_SERVICE_NAME_PROPERTY;

    private ParsingMode parsingMode = GRACEFUL;
  }
}
