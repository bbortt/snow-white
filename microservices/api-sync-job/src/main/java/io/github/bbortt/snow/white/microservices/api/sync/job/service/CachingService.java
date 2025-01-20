package io.github.bbortt.snow.white.microservices.api.sync.job.service;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.Api;

public interface CachingService {
  void publishApiInformation(Api api);
}
