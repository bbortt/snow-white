/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl;

import static java.util.Objects.nonNull;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.CachingService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.OpenApiNotIndexedException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl.client.ApiIndexApiClient;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiIndexCachingService implements CachingService {

  private final ApiIndexApiClient apiIndexApiClient;

  @Override
  @WithSpan
  public @NonNull String fetchApiSourceUrl(ApiInformation apiInformation)
    throws OpenApiNotIndexedException {
    var response = apiIndexApiClient.getApiDetailsWithHttpInfo(
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
