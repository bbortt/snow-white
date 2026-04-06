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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.api.index.domain.model.ApiReference;
import io.github.bbortt.snow.white.microservices.api.index.domain.repository.ApiReferenceRepository;
import io.github.bbortt.snow.white.microservices.api.index.service.exception.ApiAlreadyIndexedException;
import io.github.bbortt.snow.white.microservices.api.index.service.exception.InvalidReleaseWithContentException;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void shouldSaveReferencedApi()
      throws ApiAlreadyIndexedException, InvalidReleaseWithContentException {
      doReturn(Optional.empty())
        .when(apiReferenceRepositoryMock)
        .findById(
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
      doReturn(Optional.of(mock(ApiReference.class)))
        .when(apiReferenceRepositoryMock)
        .findById(
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

    @Test
    void shouldReplaceExistingPrerelease_whenPrereleaseIsReUploaded()
      throws ApiAlreadyIndexedException, InvalidReleaseWithContentException {
      var prereleaseReference = apiReference
        .withPrerelease(true)
        .withPrereleaseContent("spec: content");

      var id = ApiReference.ApiReferenceId.builder()
        .otelServiceName(prereleaseReference.getOtelServiceName())
        .apiName(prereleaseReference.getApiName())
        .apiVersion(prereleaseReference.getApiVersion())
        .build();

      doReturn(Optional.of(prereleaseReference))
        .when(apiReferenceRepositoryMock)
        .findById(id);

      fixture.persist(prereleaseReference);

      var order = inOrder(apiReferenceRepositoryMock);
      order.verify(apiReferenceRepositoryMock).deleteById(id);
      order.verify(apiReferenceRepositoryMock).save(prereleaseReference);
    }

    @Test
    void shouldReplaceExistingPrerelease_whenReleaseIsReUploaded()
      throws ApiAlreadyIndexedException, InvalidReleaseWithContentException {
      var prereleaseReference = apiReference
        .withPrerelease(true)
        .withPrereleaseContent("spec: content");

      var id = ApiReference.ApiReferenceId.builder()
        .otelServiceName(prereleaseReference.getOtelServiceName())
        .apiName(prereleaseReference.getApiName())
        .apiVersion(prereleaseReference.getApiVersion())
        .build();

      doReturn(Optional.of(prereleaseReference))
        .when(apiReferenceRepositoryMock)
        .findById(id);

      fixture.persist(apiReference);

      var order = inOrder(apiReferenceRepositoryMock);
      order.verify(apiReferenceRepositoryMock).deleteById(id);
      order.verify(apiReferenceRepositoryMock).save(apiReference);
    }

    @Test
    void shouldThrowException_whenReleaseContainsContent() {
      var prereleaseReference = apiReference
        .withPrerelease(false)
        .withPrereleaseContent("spec: content");

      var id = ApiReference.ApiReferenceId.builder()
        .otelServiceName(prereleaseReference.getOtelServiceName())
        .apiName(prereleaseReference.getApiName())
        .apiVersion(prereleaseReference.getApiVersion())
        .build();

      doReturn(Optional.empty()).when(apiReferenceRepositoryMock).findById(id);

      assertThatThrownBy(() ->
        fixture.persist(prereleaseReference)
      ).isInstanceOf(InvalidReleaseWithContentException.class);
    }
  }

  @Nested
  class HasApiByInformationBeenIndexedTest {

    private final String otelServiceName = "otelServiceName";
    private final String apiName = "apiName";
    private final String apiVersion = "apiVersion";

    public static Stream<Boolean> shouldCheckIfStableApiExists() {
      return Stream.of(true, false);
    }

    @MethodSource
    @ParameterizedTest
    void shouldCheckIfStableApiExists(Boolean apiExists) {
      doReturn(apiExists)
        .when(apiReferenceRepositoryMock)
        .existsByOtelServiceNameEqualsAndApiNameEqualsAndApiVersionEqualsAndPrereleaseIsFalse(
          otelServiceName,
          apiName,
          apiVersion
        );

      boolean apiIndexed = fixture.hasApiByInformationBeenIndexed(
        otelServiceName,
        apiName,
        apiVersion,
        false
      );

      assertThat(apiIndexed).isEqualTo(apiExists);
    }

    public static Stream<Boolean> shouldCheckIfApiExistsIncludingPrereleases() {
      return Stream.of(true, false);
    }

    @MethodSource
    @ParameterizedTest
    void shouldCheckIfApiExistsIncludingPrereleases(Boolean apiExists) {
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
        apiVersion,
        true
      );

      assertThat(apiIndexed).isEqualTo(apiExists);
    }
  }
}
