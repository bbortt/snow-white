/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service.client;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.apiindexapi.api.ApiIndexApi;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.apiindexapi.dto.GetAllApis200ResponseInner;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@Component
@RequiredArgsConstructor
public class ApiIndexApiClient {

  private final ApiIndexApi apiIndexApi;

  @Retryable(
    retryFor = {
      HttpServerErrorException.class,
      ResourceAccessException.class,
    },
    backoff = @Backoff(delay = 200, multiplier = 2)
  )
  public ResponseEntity<GetAllApis200ResponseInner> getApiDetailsWithHttpInfo(
    String serviceName,
    String apiName,
    String apiVersion
  ) {
    return apiIndexApi.getApiDetailsWithHttpInfo(
      serviceName,
      apiName,
      apiVersion
    );
  }
}
