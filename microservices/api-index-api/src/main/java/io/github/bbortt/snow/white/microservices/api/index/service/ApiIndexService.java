/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.service;

import io.github.bbortt.snow.white.microservices.api.index.domain.model.ApiReference;
import io.github.bbortt.snow.white.microservices.api.index.domain.repository.ApiReferenceRepository;
import io.github.bbortt.snow.white.microservices.api.index.domain.repository.ApiReferenceSpecification;
import io.github.bbortt.snow.white.microservices.api.index.service.exception.ApiAlreadyIndexedException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApiIndexService {

  private final ApiReferenceRepository apiReferenceRepository;

  public void persist(ApiReference apiReference)
    throws ApiAlreadyIndexedException {
    if (
      apiReferenceRepository.existsById(
        ApiReference.ApiReferenceId.builder()
          .otelServiceName(apiReference.getOtelServiceName())
          .apiName(apiReference.getApiName())
          .apiVersion(apiReference.getApiVersion())
          .build()
      )
    ) {
      throw new ApiAlreadyIndexedException(apiReference);
    }

    apiReferenceRepository.save(apiReference);
  }

  public boolean hasApiByInformationBeenIndexed(
    String otelServiceName,
    String apiName,
    String apiVersion
  ) {
    return apiReferenceRepository.existsByOtelServiceNameEqualsAndApiNameEqualsAndApiVersionEquals(
      otelServiceName,
      apiName,
      apiVersion
    );
  }

  public Page<@NonNull ApiReference> findAllIngestedApis(
    @Nullable String serviceName,
    @Nullable String apiName,
    Pageable pageable
  ) {
    return apiReferenceRepository.findAll(
      ApiReferenceSpecification.from(serviceName, apiName),
      pageable
    );
  }

  public Optional<ApiReference> findIngestedApi(
    String otelServiceName,
    String apiName,
    String apiVersion
  ) {
    return apiReferenceRepository.findByOtelServiceNameEqualsAndApiNameEqualsAndApiVersionEquals(
      otelServiceName,
      apiName,
      apiVersion
    );
  }
}
