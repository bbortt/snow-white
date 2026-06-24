/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository;

import static jakarta.persistence.criteria.JoinType.INNER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class QualityGateReportSpecificationUnitTest {

  @Mock
  private Root<QualityGateReport> rootMock;

  @Mock
  private CriteriaQuery<?> criteriaQueryMock;

  @Mock
  private CriteriaBuilder criteriaBuilderMock;

  @Nested
  class FromTest {

    @Test
    void shouldReturnConjunction_whenAllFiltersAreNull() {
      var conjunction = mock(Predicate.class);
      doReturn(conjunction).when(criteriaBuilderMock).conjunction();

      var result = QualityGateReportSpecification.from(
        null,
        null,
        null
      ).toPredicate(rootMock, criteriaQueryMock, criteriaBuilderMock);

      assertThat(result).isSameAs(conjunction);
      verifyNoInteractions(rootMock);
      verify(criteriaQueryMock, never()).distinct(true);
    }

    @Test
    void shouldJoinAndFilterByServiceName_whenOnlyServiceNameIsProvided() {
      Join<QualityGateReport, ApiTest> joinMock = mock();
      doReturn(joinMock).when(rootMock).join("apiTests", INNER);

      Path<String> serviceNamePath = mock();
      doReturn(serviceNamePath).when(joinMock).get("serviceName");

      var predicate = mock(Predicate.class);
      doReturn(predicate)
        .when(criteriaBuilderMock)
        .equal(serviceNamePath, "my-service");
      doReturn(predicate).when(criteriaBuilderMock).and(predicate);

      QualityGateReportSpecification.from("my-service", null, null).toPredicate(
        rootMock,
        criteriaQueryMock,
        criteriaBuilderMock
      );

      verify(criteriaQueryMock).distinct(true);
      verify(rootMock).join("apiTests", INNER);
      verify(criteriaBuilderMock).equal(serviceNamePath, "my-service");
      verify(joinMock, never()).get("apiName");
      verify(joinMock, never()).get("apiVersion");
    }

    @Test
    void shouldJoinAndFilterByApiName_whenOnlyApiNameIsProvided() {
      Join<QualityGateReport, ApiTest> joinMock = mock();
      doReturn(joinMock).when(rootMock).join("apiTests", INNER);

      Path<String> apiNamePath = mock();
      doReturn(apiNamePath).when(joinMock).get("apiName");

      var predicate = mock(Predicate.class);
      doReturn(predicate)
        .when(criteriaBuilderMock)
        .equal(apiNamePath, "my-api");
      doReturn(predicate).when(criteriaBuilderMock).and(predicate);

      QualityGateReportSpecification.from(null, "my-api", null).toPredicate(
        rootMock,
        criteriaQueryMock,
        criteriaBuilderMock
      );

      verify(criteriaQueryMock).distinct(true);
      verify(criteriaBuilderMock).equal(apiNamePath, "my-api");
      verify(joinMock, never()).get("serviceName");
      verify(joinMock, never()).get("apiVersion");
    }

    @Test
    void shouldJoinAndFilterByApiVersion_whenOnlyApiVersionIsProvided() {
      Join<QualityGateReport, ApiTest> joinMock = mock();
      doReturn(joinMock).when(rootMock).join("apiTests", INNER);

      Path<String> apiVersionPath = mock();
      doReturn(apiVersionPath).when(joinMock).get("apiVersion");

      var predicate = mock(Predicate.class);
      doReturn(predicate).when(criteriaBuilderMock).equal(apiVersionPath, "v1");
      doReturn(predicate).when(criteriaBuilderMock).and(predicate);

      QualityGateReportSpecification.from(null, null, "v1").toPredicate(
        rootMock,
        criteriaQueryMock,
        criteriaBuilderMock
      );

      verify(criteriaQueryMock).distinct(true);
      verify(criteriaBuilderMock).equal(apiVersionPath, "v1");
      verify(joinMock, never()).get("serviceName");
      verify(joinMock, never()).get("apiName");
    }

    @Test
    void shouldJoinOnceAndApplyAllPredicates_whenAllFiltersAreProvided() {
      Join<QualityGateReport, ApiTest> joinMock = mock();
      doReturn(joinMock).when(rootMock).join("apiTests", INNER);

      Path<String> serviceNamePath = mock();
      doReturn(serviceNamePath).when(joinMock).get("serviceName");
      Path<String> apiNamePath = mock();
      doReturn(apiNamePath).when(joinMock).get("apiName");
      Path<String> apiVersionPath = mock();
      doReturn(apiVersionPath).when(joinMock).get("apiVersion");

      var p1 = mock(Predicate.class);
      doReturn(p1).when(criteriaBuilderMock).equal(serviceNamePath, "svc");
      var p2 = mock(Predicate.class);
      doReturn(p2).when(criteriaBuilderMock).equal(apiNamePath, "api");
      var p3 = mock(Predicate.class);
      doReturn(p3).when(criteriaBuilderMock).equal(apiVersionPath, "v1");

      QualityGateReportSpecification.from("svc", "api", "v1").toPredicate(
        rootMock,
        criteriaQueryMock,
        criteriaBuilderMock
      );

      verify(criteriaQueryMock).distinct(true);
      verify(rootMock).join("apiTests", INNER);
      verify(criteriaBuilderMock).and(p1, p2, p3);
    }
  }
}
