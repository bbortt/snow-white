package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl.redis;

import io.github.bbortt.snow.white.microservices.api.sync.job.api.redis.ApiEndpointEntry;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.redis.ApiEndpointRepository;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.CachingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisCachingService implements CachingService {

  private final ApiEndpointRepository repository;

  @Override
  public void publishApiInformation(ApiInformation apiInformation) {
    repository.save(
      new ApiEndpointEntry(
        apiInformation.getServiceName(),
        apiInformation.getName(),
        apiInformation.getVersion(),
        apiInformation.getSourceUrl()
      )
    );
  }
}
