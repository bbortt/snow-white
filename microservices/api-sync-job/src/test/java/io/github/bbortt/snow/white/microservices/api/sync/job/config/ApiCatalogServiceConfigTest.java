/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.api.EntityApi;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.ApiCatalogService;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.OpenApiValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class ApiCatalogServiceConfigTest {

  private ApplicationContextRunner contextRunner;

  @BeforeEach
  void beforeEachSetup() {
    contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(ApiCatalogServiceConfig.class)
      .withBean(OpenApiValidationService.class, () ->
        mock(OpenApiValidationService.class)
      );
  }

  @Nested
  class ServiceInterfaceCatalogService {

    @Test
    void shouldCreateServiceInterfaceCatalogService_whenBaseUrlPropertyIsSet() {
      var apiSyncJobPropertiesMock = mock(ApiSyncJobProperties.class);
      doReturn(new ApiSyncJobProperties.ServiceInterfaceProperties())
        .when(apiSyncJobPropertiesMock)
        .getServiceInterface();

      contextRunner
        .withBean(ApiSyncJobProperties.class, () -> apiSyncJobPropertiesMock)
        .withBean(RestClient.Builder.class, RestClient::builder)
        .withBean(AsyncTaskExecutor.class, () -> mock(AsyncTaskExecutor.class))
        .withPropertyValues(
          ApiSyncJobProperties.ServiceInterfaceProperties.BASE_URL_PROPERTY_NAME +
            "=http://localhost:8080"
        )
        .run(context ->
          assertThat(context)
            .asInstanceOf(type(AssertableApplicationContext.class))
            .satisfies(
              c -> assertThat(c).hasSingleBean(ApiCatalogService.class),
              c ->
                assertThat(c.getBean(ApiCatalogService.class)).isInstanceOf(
                  io.github.bbortt.snow.white.microservices.api.sync.job.service.impl
                    .ServiceInterfaceCatalogService.class
                ),
              c ->
                assertThat(c)
                  .getBean("serviceInterfaceCatalogService")
                  .isEqualTo(c.getBean(ApiCatalogService.class))
            )
        );

      verify(apiSyncJobPropertiesMock).getServiceInterface();
    }

    @Test
    void shouldNotCreateServiceInterfaceCatalogService_whenBaseUrlPropertyIsMissing() {
      contextRunner.run(
        ServiceInterfaceCatalogService::assertThatServiceInterfaceCatalogServiceBeanIsNotRegistered
      );
    }

    private static void assertThatServiceInterfaceCatalogServiceBeanIsNotRegistered(
      AssertableApplicationContext context
    ) {
      assertThat(context)
        .asInstanceOf(type(AssertableApplicationContext.class))
        .satisfies(
          c -> assertThat(c).doesNotHaveBean(ApiCatalogService.class),
          c -> assertThat(c).doesNotHaveBean("serviceInterfaceCatalogService")
        );
    }
  }

  @Nested
  class BackstageCatalogService {

    @Test
    void shouldCreateBackstageCatalogService_whenBaseUrlPropertyIsSet() {
      var apiSyncJobPropertiesMock = mock(ApiSyncJobProperties.class);
      doReturn(new ApiSyncJobProperties.BackstageProperties())
        .when(apiSyncJobPropertiesMock)
        .getBackstage();

      contextRunner
        .withBean(ApiSyncJobProperties.class, () -> apiSyncJobPropertiesMock)
        .withBean(EntityApi.class, () -> mock(EntityApi.class))
        .withBean(JsonMapper.class, () -> mock(JsonMapper.class))
        .withBean(AsyncTaskExecutor.class, () -> mock(AsyncTaskExecutor.class))
        .withPropertyValues(
          ApiSyncJobProperties.BackstageProperties.BASE_URL_PROPERTY_NAME +
            "=http://localhost:3000"
        )
        .run(context ->
          assertThat(context)
            .asInstanceOf(type(AssertableApplicationContext.class))
            .satisfies(
              c -> assertThat(c).hasSingleBean(ApiCatalogService.class),
              c ->
                assertThat(c.getBean(ApiCatalogService.class)).isInstanceOf(
                  io.github.bbortt.snow.white.microservices.api.sync.job.service.impl
                    .BackstageCatalogService.class
                ),
              c ->
                assertThat(c)
                  .getBean("backstageCatalogService")
                  .isEqualTo(c.getBean(ApiCatalogService.class))
            )
        );

      verify(apiSyncJobPropertiesMock).getBackstage();
    }

    @Test
    void shouldNotCreateBackstageCatalogService_whenBaseUrlPropertyIsMissing() {
      contextRunner.run(
        BackstageCatalogService::assertThatBackstageCatalogServiceBeanIsNotRegistered
      );
    }

    private static void assertThatBackstageCatalogServiceBeanIsNotRegistered(
      AssertableApplicationContext context
    ) {
      assertThat(context)
        .asInstanceOf(type(AssertableApplicationContext.class))
        .satisfies(
          c -> assertThat(c).doesNotHaveBean(ApiCatalogService.class),
          c -> assertThat(c).doesNotHaveBean("backstageCatalogService")
        );
    }
  }
}
