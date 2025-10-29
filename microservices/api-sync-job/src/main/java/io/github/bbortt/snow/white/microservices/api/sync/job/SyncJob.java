/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job;

import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.LOADED;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.ApiCatalogService;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.CachingService;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncJob {

  private final List<ApiCatalogService> apiCatalogServices;
  private final CachingService cachingService;

  private static long countTotalApiInformation(
    List<ApiCatalogIndex> apiCatalogIndices
  ) {
    var apiInformationCount = apiCatalogIndices
      .parallelStream()
      .map(ApiCatalogIndex::apiInformation)
      .flatMap(Collection::parallelStream)
      .count();
    logger.info("Validating {} APIs loaded from index", apiInformationCount);
    return apiInformationCount;
  }

  void syncCatalog() {
    var apiCatalogIndices = loadApiCatalogFromServices();
    var apiInformationCount = countTotalApiInformation(apiCatalogIndices);

    var validApis = apiCatalogIndices
      .parallelStream()
      .map(ApiCatalogIndex::validateApiInformation)
      .flatMap(Collection::parallelStream)
      .map(CompletableFuture::join)
      .filter(this::publishLoadedApi)
      .toList();

    logger.info(
      "Updated {} of {} valid APIs",
      validApis.size(),
      apiInformationCount
    );
  }

  private @NonNull List<ApiCatalogIndex> loadApiCatalogFromServices() {
    return apiCatalogServices
      .parallelStream()
      .map(apiCatalogService ->
        new ApiCatalogIndex(
          apiCatalogService,
          apiCatalogService.fetchApiIndex()
        )
      )
      .toList();
  }

  private boolean publishLoadedApi(ApiInformation apiInformation) {
    if (!LOADED.equals(apiInformation.getLoadStatus())) {
      logger.warn(
        "Failed to load API '{}', status is '{}'",
        apiInformation.getTitle(),
        apiInformation.getLoadStatus()
      );

      return false;
    }

    cachingService.publishApiInformation(apiInformation);

    return true;
  }

  private record ApiCatalogIndex(
    ApiCatalogService apiCatalogService,
    Set<ApiInformation> apiInformation
  ) {
    public List<CompletableFuture<ApiInformation>> validateApiInformation() {
      return apiInformation
        .parallelStream()
        .map(indexedApi ->
          supplyAsync(() ->
            apiCatalogService.validateApiInformation(indexedApi)
          )
        )
        .toList();
    }
  }
}
