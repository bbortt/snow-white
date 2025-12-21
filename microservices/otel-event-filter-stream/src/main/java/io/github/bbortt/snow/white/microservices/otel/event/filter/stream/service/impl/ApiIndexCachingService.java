/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.service.impl;

import static org.springframework.http.HttpStatus.OK;

import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.client.api.index.api.ApiIndexApi;
import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.service.CachingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiIndexCachingService implements CachingService {

  private final ApiIndexApi apiIndexApi;

  @Override
  public boolean apiExists(
    String otelServiceName,
    String apiName,
    String apiVersion
  ) {
    try {
      return apiIndexApi
        .checkApiExistsWithHttpInfo(otelServiceName, apiName, apiVersion)
        .getStatusCode()
        .equals(OK);
    } catch (Exception e) {
      logger.debug("API existence check failed!", e);
      return false;
    }
  }
}
