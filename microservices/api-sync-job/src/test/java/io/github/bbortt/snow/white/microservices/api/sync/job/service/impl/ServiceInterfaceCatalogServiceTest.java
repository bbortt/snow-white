/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.OpenApiValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.client.RestClient;

@ExtendWith({ MockitoExtension.class })
class ServiceInterfaceCatalogServiceTest {

  private static final String BASE_URL = "http://localhost:8080";

  @Mock
  private RestClient restClientMock;

  @Mock
  private RestClient.Builder restCLientBuilderMock;

  @Mock
  private OpenApiValidationService openApiValidationServiceMock;

  @Mock
  private AsyncTaskExecutor taskExecutorMock;

  private ApiSyncJobProperties.ServiceInterfaceProperties serviceInterfaceProperties;

  @BeforeEach
  void beforeEachSetup() {
    serviceInterfaceProperties =
      new ApiSyncJobProperties.ServiceInterfaceProperties();
    serviceInterfaceProperties.setBaseUrl(BASE_URL);
    serviceInterfaceProperties.setIndexUri("/sir/index");

    doReturn(restCLientBuilderMock)
      .when(restCLientBuilderMock)
      .baseUrl(BASE_URL);
    doReturn(restClientMock).when(restCLientBuilderMock).build();
  }

  @Nested
  class Constructor {

    @Test
    void createsRestClient() {
      assertThat(
        new ServiceInterfaceCatalogService(
          restCLientBuilderMock,
          openApiValidationServiceMock,
          serviceInterfaceProperties,
          taskExecutorMock
        )
      ).hasNoNullFieldsOrProperties();
    }
  }

  @Nested
  class ValidateApiInformation {

    @ParameterizedTest
    @EnumSource(ParsingMode.class)
    void validatesApiUsingService(ParsingMode parsingMode) {
      serviceInterfaceProperties.setParsingMode(parsingMode);

      var fixture = new ServiceInterfaceCatalogService(
        restCLientBuilderMock,
        openApiValidationServiceMock,
        serviceInterfaceProperties,
        taskExecutorMock
      );

      var apiInformation = mock(ApiInformation.class);
      fixture.validateApiInformation(apiInformation);

      verify(openApiValidationServiceMock).validateApiInformationFromIndex(
        apiInformation,
        parsingMode
      );
    }
  }
}
