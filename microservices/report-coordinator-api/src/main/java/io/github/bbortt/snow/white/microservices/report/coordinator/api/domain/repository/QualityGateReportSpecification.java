/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository;

import static jakarta.persistence.criteria.JoinType.INNER;
import static java.util.Objects.nonNull;
import static lombok.AccessLevel.PRIVATE;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = PRIVATE)
public final class QualityGateReportSpecification {

  public static Specification<QualityGateReport> from(
    @Nullable String serviceName,
    @Nullable String apiName,
    @Nullable String apiVersion
  ) {
    return (root, query, criteriaBuilder) -> {
      if (serviceName == null && apiName == null && apiVersion == null) {
        return criteriaBuilder.conjunction();
      }

      query.distinct(true);
      var apiTests = root.join("apiTests", INNER);

      var predicates = new ArrayList<Predicate>();
      if (nonNull(serviceName)) {
        predicates.add(
          criteriaBuilder.equal(apiTests.get("serviceName"), serviceName)
        );
      }
      if (nonNull(apiName)) {
        predicates.add(criteriaBuilder.equal(apiTests.get("apiName"), apiName));
      }
      if (nonNull(apiVersion)) {
        predicates.add(
          criteriaBuilder.equal(apiTests.get("apiVersion"), apiVersion)
        );
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
