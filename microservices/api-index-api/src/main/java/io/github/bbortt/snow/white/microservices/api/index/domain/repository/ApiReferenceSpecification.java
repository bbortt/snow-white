/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.domain.repository;

import static lombok.AccessLevel.PRIVATE;

import io.github.bbortt.snow.white.microservices.api.index.domain.model.ApiReference;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = PRIVATE)
public final class ApiReferenceSpecification {

  public static Specification<ApiReference> from(
    @Nullable String serviceName,
    @Nullable String apiName
  ) {
    Specification<ApiReference> spec = (root, query, criteriaBuilder) ->
      criteriaBuilder.conjunction();
    if (serviceName != null) {
      spec = spec.and((root, query, criteriaBuilder) ->
        criteriaBuilder.like(
          criteriaBuilder.lower(root.get("otelServiceName")),
          "%" + serviceName.toLowerCase() + "%"
        )
      );
    }
    if (apiName != null) {
      spec = spec.and((root, query, criteriaBuilder) ->
        criteriaBuilder.like(
          criteriaBuilder.lower(root.get("apiName")),
          "%" + apiName.toLowerCase() + "%"
        )
      );
    }
    return spec;
  }
}
