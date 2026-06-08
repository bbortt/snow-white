/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl;

import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.client.apiindexapi.api.ApiIndexApi;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.client.apiindexapi.dto.GetAllApis200ResponseInner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Component
class ApiIndexApiClient {

  private ApiIndexApi apiIndexApi;

  @Autowired
  public void setApiIndexApi(ApiIndexApi apiIndexApi) {
    this.apiIndexApi = apiIndexApi;
  }

  @Retryable(
    retryFor = {
      RestClientResponseException.class,
      ResourceAccessException.class,
    },
    maxAttempts = 3,
    backoff = @Backoff(delay = 200, multiplier = 2)
  )
  ResponseEntity<GetAllApis200ResponseInner> getApiDetailsWithHttpInfo(
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
