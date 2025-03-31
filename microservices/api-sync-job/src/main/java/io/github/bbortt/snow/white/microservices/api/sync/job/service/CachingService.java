package io.github.bbortt.snow.white.microservices.api.sync.job.service;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;

public interface CachingService {
  void publishApiInformation(ApiInformation apiInformation);
}
