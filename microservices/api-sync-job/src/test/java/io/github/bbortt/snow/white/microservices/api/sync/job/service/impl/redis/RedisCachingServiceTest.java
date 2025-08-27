/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl.redis;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.commons.redis.ApiEndpointEntry;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.redis.ApiEndpointRepository;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.impl.RedisCachingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class RedisCachingServiceTest {

  @Mock
  private ApiEndpointRepository apiEndpointRepositoryMock;

  @Captor
  private ArgumentCaptor<ApiEndpointEntry> apiEndpointEntryCaptor;

  private RedisCachingService service;

  @BeforeEach
  void beforeEachSetup() {
    service = new RedisCachingService(apiEndpointRepositoryMock);
  }

  @Nested
  class PublishApiInformationInformation {

    @Test
    void shouldSaveApiInformationToRepository() {
      var serviceName = "test-service";
      var apiName = "test-api";
      var apiVersion = "1.2.3";
      var sourceUrl = "https://sample.repository";

      var api = new ApiInformation()
        .withServiceName(serviceName)
        .withName(apiName)
        .withVersion(apiVersion)
        .withSourceUrl(sourceUrl)
        .withApiType(OPENAPI);

      doAnswer(returnsFirstArg())
        .when(apiEndpointRepositoryMock)
        .save(any(ApiEndpointEntry.class));

      service.publishApiInformation(api);

      verify(apiEndpointRepositoryMock).save(apiEndpointEntryCaptor.capture());

      assertThat(apiEndpointEntryCaptor.getValue()).satisfies(
        e ->
          assertThat(e.getId()).isEqualTo(
            serviceName + ":" + apiName + ":" + apiVersion
          ),
        apiEndpointEntry ->
          assertThat(apiEndpointEntry.getOtelServiceName()).isEqualTo(
            serviceName
          ),
        apiEndpointEntry ->
          assertThat(apiEndpointEntry.getApiName()).isEqualTo(apiName),
        apiEndpointEntry ->
          assertThat(apiEndpointEntry.getApiVersion()).isEqualTo(apiVersion),
        apiEndpointEntry ->
          assertThat(apiEndpointEntry.getSourceUrl()).isEqualTo(sourceUrl),
        apiEndpointEntry ->
          assertThat(apiEndpointEntry.getApiType()).isEqualTo(OPENAPI.getVal())
      );
    }
  }
}
