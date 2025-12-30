/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.apiindexapi.api.ApiIndexApi;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.apiindexapi.dto.GetAllApis200ResponseInner;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformationMapper;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith({ MockitoExtension.class })
class ApiIndexCachingServiceTest {

  private static final String OTEL_SERVICE_NAME = "otel-service-name";
  private static final String API_NAME = "api-name";
  private static final String API_VERSION = "api-version";

  @Mock
  private ApiIndexApi apiIndexApiMock;

  @Mock
  private ApiInformationMapper apiInformationMapperMock;

  private ApiInformation defaultApiInformation;

  private ApiIndexCachingService fixture;

  @BeforeEach
  void beforeEachSetup() {
    defaultApiInformation = ApiInformation.builder()
      .serviceName(OTEL_SERVICE_NAME)
      .name(API_NAME)
      .version(API_VERSION)
      .build();

    fixture = new ApiIndexCachingService(
      apiIndexApiMock,
      apiInformationMapperMock
    );
  }

  @Nested
  class ApiInformationIndexedTest {

    @Test
    void shouldReturnTrue_whenApiHasBeenIndexedBefore() {
      doReturn(ResponseEntity.ok().build())
        .when(apiIndexApiMock)
        .checkApiExistsWithHttpInfo(OTEL_SERVICE_NAME, API_NAME, API_VERSION);

      assertThat(fixture.apiInformationIndexed(defaultApiInformation)).isTrue();

      verifyNoInteractions(apiInformationMapperMock);
    }

    @Test
    void shouldReturnFalse_whenApiHasNotBeenIndexedBefore() {
      doReturn(ResponseEntity.notFound().build())
        .when(apiIndexApiMock)
        .checkApiExistsWithHttpInfo(OTEL_SERVICE_NAME, API_NAME, API_VERSION);

      assertThat(
        fixture.apiInformationIndexed(defaultApiInformation)
      ).isFalse();

      verifyNoInteractions(apiInformationMapperMock);
    }

    @Test
    void shouldReturnFalse_whenApiExistenceCheckFails() {
      doThrow(new IllegalArgumentException("API Exception"))
        .when(apiIndexApiMock)
        .checkApiExistsWithHttpInfo(OTEL_SERVICE_NAME, API_NAME, API_VERSION);

      assertThat(
        fixture.apiInformationIndexed(defaultApiInformation)
      ).isFalse();

      verifyNoInteractions(apiInformationMapperMock);
    }
  }

  @Nested
  class PublishApiInformationInformationTest {

    @Test
    void shouldPublishApiInformation() throws URISyntaxException {
      var dto = mock(GetAllApis200ResponseInner.class);
      doReturn(dto).when(apiInformationMapperMock).toDto(defaultApiInformation);

      doReturn(ResponseEntity.created(new URI("mock")).build())
        .when(apiIndexApiMock)
        .ingestApiWithHttpInfo(dto);

      assertThatCode(() ->
        fixture.publishApiInformation(defaultApiInformation)
      ).doesNotThrowAnyException();
    }
  }
}
