/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static java.lang.Boolean.FALSE;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.OK;

import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.apiindexapi.api.ApiIndexApi;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformationMapper;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.CachingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiIndexCachingService implements CachingService {

  private final ApiIndexApi apiIndexApi;
  private final ApiInformationMapper apiInformationMapper;

  @Override
  @Retryable(
    retryFor = {
      RestClientResponseException.class, ResourceAccessException.class,
    },
    maxAttempts = 3,
    backoff = @Backoff(delay = 200, multiplier = 2)
  )
  public boolean apiInformationIndexed(ApiInformation apiInformation) {
    return apiIndexApi
      .checkApiExistsWithHttpInfo(
        apiInformation.getServiceName(),
        apiInformation.getName(),
        apiInformation.getVersion(),
        FALSE
      )
      .getStatusCode()
      .equals(OK);
  }

  @Recover
  boolean recoverApiInformationIndexed(
    Exception e,
    ApiInformation apiInformation
  ) {
    logger.debug("Failed to check if API exists - recovering!", e);
    return false;
  }

  @Override
  @Retryable(
    retryFor = {
      RestClientResponseException.class, ResourceAccessException.class,
    },
    maxAttempts = 3,
    backoff = @Backoff(delay = 200, multiplier = 2)
  )
  public void publishApiInformation(ApiInformation apiInformation) {
    if (
      apiIndexApi
        .ingestApiWithHttpInfo(apiInformationMapper.toDto(apiInformation))
        .getStatusCode()
        .equals(CONFLICT)
    ) {
      logger.warn(
        "API information '{}' already indexed - this should have been checked beforehand!",
        apiInformation
      );
    }
  }

  @Recover
  void recoverPublishApiInformation(
    Exception e,
    ApiInformation apiInformation
  ) {
    logger.error("Failed to publish API information!", e);
  }
}
