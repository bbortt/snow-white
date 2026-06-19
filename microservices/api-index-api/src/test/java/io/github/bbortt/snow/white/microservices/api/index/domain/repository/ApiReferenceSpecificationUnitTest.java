/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.github.bbortt.snow.white.microservices.api.index.domain.model.ApiReference;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class ApiReferenceSpecificationUnitTest {

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

      Path<String> serviceNamePath = mock();
      doReturn(serviceNamePath).when(rootMock).get("otelServiceName");

      Expression<String> lowerServiceNamePath = mock();
      doReturn(lowerServiceNamePath)
        .when(criteriaBuilderMock)
        .lower(serviceNamePath);

      ApiReferenceSpecification.from("My-Service", null).toPredicate(
        rootMock,
        criteriaQueryMock,
        criteriaBuilderMock
      );

      verify(criteriaBuilderMock).like(lowerServiceNamePath, "my-service%");
      verify(rootMock, never()).get("apiName");
    }

    @Test
    void shouldFilterOnlyByApiName_whenServiceNameFilterIsAbsent() {
      doReturn(mock(Predicate.class)).when(criteriaBuilderMock).conjunction();

      Path<String> apiNamePath = mock();
      doReturn(apiNamePath).when(rootMock).get("apiName");

      Expression<String> lowerApiNamePath = mock();
      doReturn(lowerApiNamePath).when(criteriaBuilderMock).lower(apiNamePath);

      ApiReferenceSpecification.from(null, "My-Api").toPredicate(
        rootMock,
        criteriaQueryMock,
        criteriaBuilderMock
      );

      verify(criteriaBuilderMock).like(lowerApiNamePath, "my-api%");
      verify(rootMock, never()).get("otelServiceName");
    }

    @Test
    void shouldFilterByServiceNameAndApiName_whenBothFiltersAreProvided() {
      doReturn(mock(Predicate.class)).when(criteriaBuilderMock).conjunction();

      Path<String> serviceNamePath = mock();
      doReturn(serviceNamePath).when(rootMock).get("otelServiceName");

      Expression<String> lowerServiceNamePath = mock();
      doReturn(lowerServiceNamePath)
        .when(criteriaBuilderMock)
        .lower(serviceNamePath);

      Path<String> apiNamePath = mock();
      doReturn(apiNamePath).when(rootMock).get("apiName");

      Expression<String> lowerApiNamePath = mock();
      doReturn(lowerApiNamePath).when(criteriaBuilderMock).lower(apiNamePath);

      ApiReferenceSpecification.from("my-service", "my-api").toPredicate(
        rootMock,
        criteriaQueryMock,
        criteriaBuilderMock
      );

      verify(criteriaBuilderMock).like(lowerServiceNamePath, "my-service%");
      verify(criteriaBuilderMock).like(lowerApiNamePath, "my-api%");
    }
  }
}
