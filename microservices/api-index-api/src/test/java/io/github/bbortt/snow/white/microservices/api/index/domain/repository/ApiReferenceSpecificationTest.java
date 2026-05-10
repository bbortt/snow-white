/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.bbortt.snow.white.microservices.api.index.domain.model.ApiReference;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiReferenceSpecificationTest {

  @Mock
  private Root<ApiReference> rootMock;

  @Mock
  private CriteriaQuery<?> criteriaQueryMock;

  @Mock
  private CriteriaBuilder criteriaBuilderMock;

  @Nested
  class FromTest {

    @Test
    void shouldReturnConjunction_whenBothFiltersAreNull() {
      var conjunction = mock(Predicate.class);
      doReturn(conjunction).when(criteriaBuilderMock).conjunction();

      var result = ApiReferenceSpecification.from(null, null).toPredicate(
        rootMock,
        criteriaQueryMock,
        criteriaBuilderMock
      );

      assertThat(result).isSameAs(conjunction);
      verifyNoInteractions(rootMock);
    }

    @Test
    void shouldFilterOnlyByServiceName_whenApiNameFilterIsAbsent() {
      doReturn(mock(Predicate.class)).when(criteriaBuilderMock).conjunction();

      Path<Object> serviceNamePath = mock();
      doReturn(serviceNamePath).when(rootMock).get("otelServiceName");

      ApiReferenceSpecification.from("my-service", null).toPredicate(
        rootMock,
        criteriaQueryMock,
        criteriaBuilderMock
      );

      verify(criteriaBuilderMock).equal(serviceNamePath, "my-service");
      verify(rootMock, never()).get("apiName");
    }

    @Test
    void shouldFilterOnlyByApiName_whenServiceNameFilterIsAbsent() {
      doReturn(mock(Predicate.class)).when(criteriaBuilderMock).conjunction();

      Path<Object> apiNamePath = mock();
      doReturn(apiNamePath).when(rootMock).get("apiName");

      ApiReferenceSpecification.from(null, "my-api").toPredicate(
        rootMock,
        criteriaQueryMock,
        criteriaBuilderMock
      );

      verify(criteriaBuilderMock).equal(apiNamePath, "my-api");
      verify(rootMock, never()).get("otelServiceName");
    }

    @Test
    void shouldFilterByServiceNameAndApiName_whenBothFiltersAreProvided() {
      doReturn(mock(Predicate.class)).when(criteriaBuilderMock).conjunction();

      Path<Object> serviceNamePath = mock();
      doReturn(serviceNamePath).when(rootMock).get("otelServiceName");

      Path<Object> apiNamePath = mock();
      doReturn(apiNamePath).when(rootMock).get("apiName");

      ApiReferenceSpecification.from("my-service", "my-api").toPredicate(
        rootMock,
        criteriaQueryMock,
        criteriaBuilderMock
      );

      verify(criteriaBuilderMock).equal(serviceNamePath, "my-service");
      verify(criteriaBuilderMock).equal(apiNamePath, "my-api");
    }
  }
}
