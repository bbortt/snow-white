/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.api.redis;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ApiEndpointRepository
  extends CrudRepository<ApiEndpointEntry, String> {
  Optional<
    ApiEndpointEntry
  > findByOtelServiceNameEqualsAndApiNameEqualsAndApiVersionEquals(
    @Param("otelServiceName") String otelServiceName,
    @Param("apiName") String apiName,
    @Param("apiVersion") String apiVersion
  );
}
