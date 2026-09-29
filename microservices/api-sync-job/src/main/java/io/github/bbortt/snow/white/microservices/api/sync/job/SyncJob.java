/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job;

import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.LOADED;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus;
import io.github.bbortt.snow.white.microservices.api.sync.job.processing.ApiSyncProcessor;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.ApiCatalogService;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.CachingService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SyncJob {

  private final List<ApiCatalogService> apiCatalogServices;
  private final ApiSyncProcessor apiSyncProcessor;
  private final CachingService cachingService;

  public SyncJob(
    List<ApiCatalogService> apiCatalogServices,
    ApiSyncProcessor apiSyncProcessor,
    CachingService cachingService
  ) {
    this.apiCatalogServices = apiCatalogServices;
    this.apiSyncProcessor = apiSyncProcessor;
    this.cachingService = cachingService;
  }

  void syncCatalog() throws InterruptedException {
    List<Supplier<@Nullable ApiInformation>> suppliers = apiCatalogServices
      .stream()
      .map(ApiCatalogService::getApiSpecificationLoaders)
      .flatMap(Collection::stream)
      .toList();

    Map<ApiLoadStatus, Long> apiLoadStatusCounts = apiSyncProcessor.process(
      suppliers,
      this::publishLoadedApi
    );

    logger.info(
      "Successfully synchronized {} api specifications",
      apiLoadStatusCounts
    );
  }

  /**
   * Answers whether the specification reached the index, which is what the
   * processor tallies as {@code PUBLISHED}. A publish the index could not
   * accept - deferred to the next cycle, or refused outright - answers
   * {@code false}, leaving the specification counted under the status it
   * actually reached.
   */
  @RealizesSw(SwTraceables.SW_035_INDEX_OUTAGE_DEFERS_TO_NEXT_CYCLE)
  private boolean publishLoadedApi(@Nullable ApiInformation apiInformation) {
    if (
      Objects.nonNull(apiInformation) &&
      LOADED.equals(apiInformation.getLoadStatus()) &&
      !cachingService.apiInformationIndexed(apiInformation)
    ) {
      try {
        return cachingService.publishApiInformation(apiInformation);
      } catch (Exception e) {
        logger.warn("Failed to publish API information!", e);
        return false;
      }
    }

    return false;
  }
}
