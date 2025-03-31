package io.github.bbortt.snow.white.microservices.api.sync.job;

import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.LOADED;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.ApiCatalogService;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.CachingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncJob {

  private final ApiCatalogService apiCatalogService;
  private final CachingService cachingService;

  void queryAndSafeApiCatalogArtifacts() {
    var apis = apiCatalogService.fetchApiIndex();

    logger.info("Validating {} APIs loaded from index", apis.size());

    var validApis = apis
      .stream()
      .map(apiCatalogService::validateApiInformationFromIndex)
      .filter(this::publishLoadedApi)
      .toList();

    logger.info("Updated {} valid APIs", validApis.size());
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
}
