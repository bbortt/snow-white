/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.service;

import static io.github.bbortt.snow.white.microservices.api.index.api.rest.dto.GetAllApis200ResponseInner.ApiTypeEnum.UNSPECIFIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.api.index.domain.model.ApiReference;
import io.github.bbortt.snow.white.microservices.api.index.domain.repository.ApiReferenceRepository;
import io.github.bbortt.snow.white.microservices.api.index.service.exception.ApiAlreadyIndexedException;
import java.util.List;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith({ MockitoExtension.class })
class ApiIndexServiceTest {

  @Mock
  private ApiReferenceRepository apiReferenceRepositoryMock;

  private ApiIndexService fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiIndexService(apiReferenceRepositoryMock);
  }

  @Nested
  class PersistTest {

    private ApiReference apiReference;

    @BeforeEach
    void beforeEachSetup() {
      apiReference = ApiReference.builder()
        .otelServiceName("otelServiceName")
        .apiName("apiName")
        .apiVersion("apiVersion")
        .sourceUrl("sourceUrl")
        .apiType(UNSPECIFIED)
        .build();
    }

    @Test
    void shouldSaveReferencedApi() throws ApiAlreadyIndexedException {
      doReturn(false)
        .when(apiReferenceRepositoryMock)
        .existsById(
          ApiReference.ApiReferenceId.builder()
            .otelServiceName(apiReference.getOtelServiceName())
            .apiName(apiReference.getApiName())
            .apiVersion(apiReference.getApiVersion())
            .build()
        );

      fixture.persist(apiReference);

      verify(apiReferenceRepositoryMock).save(apiReference);
    }

    @Test
    void shouldThrowException_whenApiReferenceAlreadyExists() {
      doReturn(true)
        .when(apiReferenceRepositoryMock)
        .existsById(
          ApiReference.ApiReferenceId.builder()
            .otelServiceName(apiReference.getOtelServiceName())
            .apiName(apiReference.getApiName())
            .apiVersion(apiReference.getApiVersion())
            .build()
        );

      assertThatThrownBy(() -> fixture.persist(apiReference)).isInstanceOf(
        ApiAlreadyIndexedException.class
      );

      verify(apiReferenceRepositoryMock, never()).save(any(ApiReference.class));
    }
  }

  @Nested
  class FindAllIngestedApisTest {

    @Test
    void shouldPassSpecificationToRepository() {
      var pageable = Pageable.unpaged();
      Page<@NonNull ApiReference> page = new PageImpl<>(List.of());

      doReturn(page)
        .when(apiReferenceRepositoryMock)
        .findAll(any(Specification.class), any(Pageable.class));

      var result = fixture.findAllIngestedApis(
        "my-service",
        "my-api",
        pageable
      );

      assertThat(result).isSameAs(page);
      verify(apiReferenceRepositoryMock).findAll(
        any(Specification.class),
        any(Pageable.class)
      );
    }

    @Test
    void shouldAcceptNullFilters() {
      var pageable = Pageable.unpaged();
      Page<@NonNull ApiReference> page = new PageImpl<>(List.of());

      doReturn(page)
        .when(apiReferenceRepositoryMock)
        .findAll(any(Specification.class), any(Pageable.class));

      var result = fixture.findAllIngestedApis(null, null, pageable);

      assertThat(result).isSameAs(page);
    }
  }

  @Nested
  class HasApiByInformationBeenIndexedTest {

    public static Stream<Boolean> shouldCheckIfApiExists() {
      return Stream.of(true, false);
    }

    @MethodSource
    @ParameterizedTest
    void shouldCheckIfApiExists(Boolean apiExists) {
      String otelServiceName = "otelServiceName";
      String apiName = "apiName";
      String apiVersion = "apiVersion";

      doReturn(apiExists)
        .when(apiReferenceRepositoryMock)
        .existsByOtelServiceNameEqualsAndApiNameEqualsAndApiVersionEquals(
          otelServiceName,
          apiName,
          apiVersion
        );

      boolean apiIndexed = fixture.hasApiByInformationBeenIndexed(
        otelServiceName,
        apiName,
        apiVersion
      );

      assertThat(apiIndexed).isEqualTo(apiExists);
    }
  }
}
