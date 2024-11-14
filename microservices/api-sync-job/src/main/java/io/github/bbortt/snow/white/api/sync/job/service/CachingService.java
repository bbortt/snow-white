package io.github.bbortt.snow.white.api.sync.job.service;

import io.github.bbortt.snow.white.api.sync.job.domain.Api;

public interface CachingService {
  void publishApiInformation(Api api);
}
