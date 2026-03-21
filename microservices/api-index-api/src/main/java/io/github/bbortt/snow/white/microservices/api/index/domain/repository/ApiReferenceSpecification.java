/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.domain.repository;

import io.github.bbortt.snow.white.microservices.api.index.domain.model.ApiReference;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

public final class ApiReferenceSpecification {

  private ApiReferenceSpecification() {}

  public static Specification<ApiReference> from(
    @Nullable String serviceName,
    @Nullable String apiName
  ) {
    Specification<ApiReference> spec = Specification.where(null);
    if (serviceName != null) {
      spec = spec.and((root, query, cb) ->
        cb.equal(root.get("otelServiceName"), serviceName)
      );
    }
    if (apiName != null) {
      spec = spec.and((root, query, cb) ->
        cb.equal(root.get("apiName"), apiName)
      );
    }
    return spec;
  }
}
