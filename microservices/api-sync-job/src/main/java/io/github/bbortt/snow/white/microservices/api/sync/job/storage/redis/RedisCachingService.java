package io.github.bbortt.snow.white.microservices.api.sync.job.storage.redis;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.Api;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.CachingService;
import org.springframework.stereotype.Service;

@Service
public class RedisCachingService implements CachingService {

  private final ApiEndpointRepository repository;

  public RedisCachingService(ApiEndpointRepository repository) {
    this.repository = repository;
  }

  @Override
  public void publishApiInformation(Api api) {
    repository.save(
      new ApiEndpointEntry(
        api.getServiceName(),
        api.getName(),
        api.getVersion(),
        api.getSourceUrl()
      )
    );
  }
}
