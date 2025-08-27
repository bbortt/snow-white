/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import io.github.bbortt.snow.white.commons.redis.ApiEndpointEntry;
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
        apiInformation.getSourceUrl(),
        apiInformation.getApiType()
      )
    );
  }
}
