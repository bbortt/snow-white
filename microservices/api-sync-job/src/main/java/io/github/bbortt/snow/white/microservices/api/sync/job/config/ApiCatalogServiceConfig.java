/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.api.EntityApi;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.ApiCatalogService;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.OpenApiValidationService;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.impl.BackstageCatalogService;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.impl.MinioService;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.impl.ServiceInterfaceCatalogService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@RequiredArgsConstructor
public class ApiCatalogServiceConfig {

  private final OpenApiValidationService openApiValidationService;

  @Bean
  @ConditionalOnProperty(
    ApiSyncJobProperties.ServiceInterfaceProperties.BASE_URL_PROPERTY_NAME
  )
  public ApiCatalogService serviceInterfaceCatalogService(
    RestClient.Builder restClientBuilder,
    ApiSyncJobProperties apiSyncJobProperties
  ) {
    return new ServiceInterfaceCatalogService(
      restClientBuilder,
      openApiValidationService,
      apiSyncJobProperties.getServiceInterface()
    );
  }

  @Bean
  @ConditionalOnProperty(
    ApiSyncJobProperties.BackstageProperties.BASE_URL_PROPERTY_NAME
  )
  public ApiCatalogService backstageCatalogService(
    ApiSyncJobProperties apiSyncJobProperties,
    EntityApi backstageEntityApi,
    JsonMapper jsonMapper,
    @Autowired(required = false) @Nullable MinioService minioService
  ) {
    return new BackstageCatalogService(
      apiSyncJobProperties.getBackstage(),
      backstageEntityApi,
      jsonMapper,
      openApiValidationService,
      minioService
    );
  }
}
