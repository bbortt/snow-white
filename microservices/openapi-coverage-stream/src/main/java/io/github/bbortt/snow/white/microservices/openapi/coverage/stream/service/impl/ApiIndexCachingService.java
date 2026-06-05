/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl;

import static java.util.Objects.nonNull;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.client.apiindexapi.api.ApiIndexApi;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.CachingService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.OpenApiNotIndexedException;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.retry.annotation.Backoff;
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
  @WithSpan
  @Retryable(
    retryFor = {
      RestClientResponseException.class,
      ResourceAccessException.class,
    },
    maxAttempts = 3,
    backoff = @Backoff(delay = 200, multiplier = 2)
  )
  public @NonNull String fetchApiSourceUrl(ApiInformation apiInformation)
    throws OpenApiNotIndexedException {
    var response = apiIndexApi.getApiDetailsWithHttpInfo(
      apiInformation.getServiceName(),
      apiInformation.getApiName(),
      apiInformation.getApiVersion()
    );

    if (
      response.getStatusCode().is2xxSuccessful() && nonNull(response.getBody())
    ) {
      return response.getBody().getSourceUrl();
    }

    throw new OpenApiNotIndexedException(apiInformation);
  }
}
