/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job;

import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.LOADED;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.PUBLISHED;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.PUBLISH_DEFERRED;

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
   * Answers with the status the specification actually reached, which is what
   * the processor tallies. Only a specification the index accepted counts as
   * {@code PUBLISHED}; one whose ingestion the index could not accept - refused
   * outright, or exhausted against an outage - counts as
   * {@code PUBLISH_DEFERRED}, which is distinct from the {@code LOADED} of a
   * specification the index already held. A cycle that published nothing
   * because the index was down therefore does not read like a cycle that had
   * nothing to publish.
   */
  @RealizesSw(SwTraceables.SW_035_INDEX_OUTAGE_DEFERS_TO_NEXT_CYCLE)
  private ApiInformation publishLoadedApi(ApiInformation apiInformation) {
    if (!LOADED.equals(apiInformation.getLoadStatus())) {
      return apiInformation;
    }

    try {
      if (cachingService.apiInformationIndexed(apiInformation)) {
        return apiInformation;
      }

      return apiInformation.withLoadStatus(
        cachingService.publishApiInformation(apiInformation)
          ? PUBLISHED
          : PUBLISH_DEFERRED
      );
    } catch (Exception e) {
      logger.warn("Failed to publish API information!", e);
      return apiInformation.withLoadStatus(PUBLISH_DEFERRED);
    }
  }
}
