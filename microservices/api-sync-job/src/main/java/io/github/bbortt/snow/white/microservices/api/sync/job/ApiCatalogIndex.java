package io.github.bbortt.snow.white.microservices.api.sync.job;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.ApiCatalogService;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.springframework.core.task.AsyncTaskExecutor;

public record ApiCatalogIndex(
  ApiCatalogService apiCatalogService,
  Set<ApiInformation> apiInformation,
  AsyncTaskExecutor taskExecutor
) {
  public List<CompletableFuture<ApiInformation>> validateApiInformation() {
    return apiInformation
      .stream()
      .map(indexedApi ->
        CompletableFuture.supplyAsync(
          () -> apiCatalogService.validateApiInformation(indexedApi),
          taskExecutor
        )
      )
      .toList();
  }
}
