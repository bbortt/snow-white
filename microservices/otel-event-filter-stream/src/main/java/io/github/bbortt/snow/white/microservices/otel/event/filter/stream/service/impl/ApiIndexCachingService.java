/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.service.impl;

import static org.springframework.http.HttpStatus.OK;

import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.client.apiindexapi.api.ApiIndexApi;
import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.service.CachingService;
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

  @Override
  @Retryable(
    retryFor = {
      RestClientResponseException.class,
      ResourceAccessException.class,
    },
    maxAttempts = 3,
    backoff = @Backoff(delay = 200, multiplier = 2)
  )
  public boolean apiExists(
    String otelServiceName,
    String apiName,
    String apiVersion
  ) {
    return apiIndexApi
      .checkApiExistsWithHttpInfo(otelServiceName, apiName, apiVersion, true)
      .getStatusCode()
      .equals(OK);
  }

  @Recover
  boolean recoverApiExists(
    Exception e,
    String otelServiceName,
    String apiName,
    String apiVersion
  ) {
    logger.debug("Failed to check if API exists - recovering!", e);
    return false;
  }
}
