/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job;

import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.LOADED;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.nonNull;
import static java.util.concurrent.StructuredTaskScope.Joiner.allSuccessfulOrThrow;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.ApiCatalogService;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.CachingService;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.StructuredTaskScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncJob {

  private final Semaphore rateLimiter = new Semaphore(1);

  private final List<ApiCatalogService> apiCatalogServices;
  private final CachingService cachingService;

  void syncCatalog() throws InterruptedException {
    try (var scope = StructuredTaskScope.open(allSuccessfulOrThrow())) {
      apiCatalogServices
        .stream()
        .map(ApiCatalogService::getApiSpecificationLoaders)
        .flatMap(Collection::stream)
        .forEach(supplier ->
          scope.fork(() -> {
            rateLimiter.acquire();
            try {
              var apiInformation = supplier.get();
              return publishLoadedApi(apiInformation);
            } finally {
              rateLimiter.release();
            }
          })
        );

      var successfullyPublishedSpecifications = scope
        .join()
        .map(StructuredTaskScope.Subtask::get)
        .filter(TRUE::equals)
        .count();

      logger.info(
        "Successfully synchronized {} api specifications",
        successfullyPublishedSpecifications
      );
    }
  }

  private boolean publishLoadedApi(@Nullable ApiInformation apiInformation) {
    if (
      nonNull(apiInformation) &&
      LOADED.equals(apiInformation.getLoadStatus()) &&
      !cachingService.apiInformationIndexed(apiInformation)
    ) {
      cachingService.publishApiInformation(apiInformation);
      return true;
    }

    return false;
  }
}
