/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static java.lang.Boolean.FALSE;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.OK;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformationMapper;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.CachingService;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.impl.client.ApiIndexApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiIndexCachingService implements CachingService {

  private final ApiIndexApiClient apiIndexApiClient;
  private final ApiInformationMapper apiInformationMapper;

  @Override
  public boolean apiInformationIndexed(ApiInformation apiInformation) {
    return apiIndexApiClient
      .checkApiExistsWithHttpInfo(
        apiInformation.getServiceName(),
        apiInformation.getName(),
        apiInformation.getVersion(),
        FALSE
      )
      .getStatusCode()
      .equals(OK);
  }

  @Override
  public void publishApiInformation(ApiInformation apiInformation) {
    if (
      apiIndexApiClient
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
}
