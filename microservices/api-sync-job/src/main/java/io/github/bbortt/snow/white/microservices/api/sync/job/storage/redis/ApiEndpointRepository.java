package io.github.bbortt.snow.white.microservices.api.sync.job.storage.redis;

import org.springframework.data.repository.CrudRepository;

public interface ApiEndpointRepository
  extends CrudRepository<ApiEndpointEntry, String> {}
