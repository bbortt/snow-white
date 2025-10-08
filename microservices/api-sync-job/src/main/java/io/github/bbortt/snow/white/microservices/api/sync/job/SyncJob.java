/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job;

import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.LOADED;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.ApiCatalogService;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.CachingService;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncJob {

  private final List<ApiCatalogService> apiCatalogServices;
  private final CachingService cachingService;
  private final AsyncTaskExecutor taskExecutor;

  void syncCatalog() {
    loadApiCatalogFromServices()
      .thenCompose(apiCatalogIndices -> {
        long apiInformationCount = apiCatalogIndices
          .stream()
          .map(ApiCatalogIndex::apiInformation)
          .flatMap(Collection::stream)
          .count();
        logger.info(
          "Validating {} APIs loaded from index",
          apiInformationCount
        );

        var validationFutures = apiCatalogIndices
          .stream()
          .map(ApiCatalogIndex::validateApiInformation)
          .flatMap(Collection::stream)
          .toList();

        return CompletableFuture.allOf(
          validationFutures.toArray(new CompletableFuture[0])
        ).thenApply(v -> {
          var validApis = validationFutures
            .stream()
            .map(CompletableFuture::join)
            .filter(this::publishLoadedApi)
            .toList();
          logger.info(
            "Updated {} of {} valid APIs",
            validApis.size(),
            apiInformationCount
          );
          return validApis;
        });
      })
      .join();
  }

  private @NonNull CompletableFuture<
    List<ApiCatalogIndex>
  > loadApiCatalogFromServices() {
    var futures = apiCatalogServices
      .stream()
      .map(apiCatalogService ->
        apiCatalogService
          .fetchApiIndex()
          .thenApply(apiInformation ->
            new ApiCatalogIndex(apiCatalogService, apiInformation, taskExecutor)
          )
      )
      .toList();

    return CompletableFuture.allOf(
      futures.toArray(new CompletableFuture[0])
    ).thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
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

    if (!cachingService.apiInformationIndexed(apiInformation)) {
      cachingService.publishApiInformation(apiInformation);
    }

    return true;
  }
}
