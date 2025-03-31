package io.github.bbortt.snow.white.microservices.api.sync.job.service;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import java.util.Set;

public interface ApiCatalogService {
  Set<ApiInformation> fetchApiIndex();

  ApiInformation validateApiInformationFromIndex(ApiInformation apiInformation);
}
