package io.github.bbortt.snow.white.microservices.api.sync.job.service;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.Api;
import java.util.Set;

public interface ApiCatalogService {
  Set<Api> fetchApiIndex();

  Api validateApiInformationFromIndex(Api api);
}
