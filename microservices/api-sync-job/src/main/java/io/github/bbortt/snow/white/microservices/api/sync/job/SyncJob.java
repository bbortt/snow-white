/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job;

import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.LOADED;
import static org.springframework.util.CollectionUtils.isEmpty;

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

  private boolean publishLoadedApi(@Nullable ApiInformation apiInformation) {
    if (
      Objects.nonNull(apiInformation) &&
      LOADED.equals(apiInformation.getLoadStatus()) &&
      !cachingService.apiInformationIndexed(apiInformation)
    ) {
      try {
        cachingService.publishApiInformation(apiInformation);
      } catch (Exception e) {
        logger.warn("Failed to publish API information!", e);
        return false;
      }

      return true;
    }

    return false;
  }
}
