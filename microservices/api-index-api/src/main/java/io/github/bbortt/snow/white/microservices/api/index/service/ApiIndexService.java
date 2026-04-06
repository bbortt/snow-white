/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.service;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.Validate.notBlank;

import io.github.bbortt.snow.white.microservices.api.index.domain.model.ApiReference;
import io.github.bbortt.snow.white.microservices.api.index.domain.repository.ApiReferenceRepository;
import io.github.bbortt.snow.white.microservices.api.index.service.exception.ApiAlreadyIndexedException;
import io.github.bbortt.snow.white.microservices.api.index.service.exception.InvalidReleaseWithContentException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApiIndexService {

  private final ApiReferenceRepository apiReferenceRepository;

  public void persist(ApiReference apiReference)
    throws ApiAlreadyIndexedException, InvalidReleaseWithContentException {
    var id = ApiReference.ApiReferenceId.builder()
      .otelServiceName(apiReference.getOtelServiceName())
      .apiName(apiReference.getApiName())
      .apiVersion(apiReference.getApiVersion())
      .build();

    var existingApiReference = apiReferenceRepository.findById(id);
    if (
      existingApiReference.isPresent() &&
      !existingApiReference.get().isPrerelease()
    ) {
      throw new ApiAlreadyIndexedException(apiReference);
    } else if (
      !apiReference.isPrerelease() &&
      isNotBlank(apiReference.getPrereleaseContent())
    ) {
      throw new InvalidReleaseWithContentException();
    } else if (
      existingApiReference.isPresent() &&
      existingApiReference.get().isPrerelease()
    ) {
      apiReferenceRepository.deleteById(id);
    }

    apiReferenceRepository.save(apiReference);
  }

  public boolean hasApiByInformationBeenIndexed(
    String otelServiceName,
    String apiName,
    String apiVersion,
    boolean includePrereleases
  ) {
    if (includePrereleases) {
      return apiReferenceRepository.existsByOtelServiceNameEqualsAndApiNameEqualsAndApiVersionEquals(
        otelServiceName,
        apiName,
        apiVersion
      );
    }

    return apiReferenceRepository.existsByOtelServiceNameEqualsAndApiNameEqualsAndApiVersionEqualsAndPrereleaseIsFalse(
      otelServiceName,
      apiName,
      apiVersion
    );
  }

  public Page<@NonNull ApiReference> findAllIngestedApis(Pageable pageable) {
    return apiReferenceRepository.findAll(pageable);
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
