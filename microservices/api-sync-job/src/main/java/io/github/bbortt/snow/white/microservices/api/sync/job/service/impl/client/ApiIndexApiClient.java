/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl.client;

import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.http.ResponseEntity.ok;

import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.apiindexapi.api.ApiIndexApi;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.apiindexapi.dto.GetAllApis200ResponseInner;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
@Component
public class ApiIndexApiClient {

  private ApiIndexApi apiIndexApi;

  @Autowired
  public void setApiIndexApi(ApiIndexApi apiIndexApi) {
    this.apiIndexApi = apiIndexApi;
  }

  @Retryable(
    retryFor = {
      HttpServerErrorException.class,
      ResourceAccessException.class,
    },
    backoff = @Backoff(delay = 200, multiplier = 2)
  )
  public ResponseEntity<Void> checkApiExistsWithHttpInfo(
    @NonNull String otelServiceName,
    @NonNull String apiName,
    @NonNull String apiVersion,
    @Nullable Boolean includePrereleases
  ) {
    return apiIndexApi.checkApiExistsWithHttpInfo(
      otelServiceName,
      apiName,
      apiVersion,
      includePrereleases
    );
  }

  @Recover
  ResponseEntity<Void> recoverCheckApiExistsWithHttpInfo(
    Exception e,
    @NonNull String otelServiceName,
    @NonNull String apiName,
    @NonNull String apiVersion,
    @Nullable Boolean includePrereleases
  ) {
    logger.debug("Failed to check if API exists - recovering!", e);
    return notFound().build();
  }

  @Retryable(
    retryFor = {
      HttpServerErrorException.class,
      ResourceAccessException.class,
    },
    backoff = @Backoff(delay = 200, multiplier = 2)
  )
  public ResponseEntity<Void> ingestApiWithHttpInfo(
    @NonNull GetAllApis200ResponseInner getAllApis200ResponseInner
  ) {
    return apiIndexApi.ingestApiWithHttpInfo(getAllApis200ResponseInner);
  }

  @Recover
  ResponseEntity<Void> recoverIngestApiWithHttpInfo(
    Exception e,
    GetAllApis200ResponseInner getAllApis200ResponseInner
  ) {
    logger.error("Failed to publish API information!", e);
    return ok().build();
  }
}
