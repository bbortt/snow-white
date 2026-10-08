/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static java.lang.Boolean.FALSE;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.OK;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
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

  /**
   * The existence check is asked with prereleases excluded, so an identity held
   * only by a prerelease reads as absent and the stable specification takes it
   * over on the first cycle after publication.
   */
  @Override
  @RealizesSw(SwTraceables.SW_033_SYNC_SKIPS_STABLE_SUPERSEDES_PRERELEASE)
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
  @RealizesSw(SwTraceables.SW_035_INDEX_OUTAGE_DEFERS_TO_NEXT_CYCLE)
  public boolean publishApiInformation(ApiInformation apiInformation) {
    var statusCode = apiIndexApiClient
      .ingestApiWithHttpInfo(apiInformationMapper.toDto(apiInformation))
      .getStatusCode();

    if (statusCode.equals(CONFLICT)) {
      logger.warn(
        "API information '{}' already indexed - this should have been checked beforehand!",
        apiInformation
      );

      // The index holds it, which is all the caller asked about.
      return true;
    }

    return statusCode.is2xxSuccessful();
  }
}
