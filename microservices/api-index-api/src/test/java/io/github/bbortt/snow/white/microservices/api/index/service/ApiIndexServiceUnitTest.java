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

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesCon;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.microservices.api.index.domain.model.ApiReference;
import io.github.bbortt.snow.white.microservices.api.index.domain.repository.ApiReferenceRepository;
import io.github.bbortt.snow.white.microservices.api.index.service.exception.ApiAlreadyIndexedException;
import io.github.bbortt.snow.white.microservices.api.index.service.exception.InvalidReleaseWithContentException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith({ MockitoExtension.class })
class ApiIndexServiceUnitTest {

  @Mock
  private ApiReferenceRepository apiReferenceRepositoryMock;

  @InjectMocks
  private ApiIndexService fixture;

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
    @VerifiesCon(
      ConTraceables.CON_007_STABLE_API_REFERENCE_IS_IMMUTABLE_ONCE_INDEXED
    )
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
    @VerifiesCon(
      ConTraceables.CON_007_STABLE_API_REFERENCE_IS_IMMUTABLE_ONCE_INDEXED
    )
    void shouldThrowException_whenApiReferenceAlreadyExists_andIncomingIsPrerelease() {
      var incomingPrerelease = apiReference
        .withPrerelease(true)
        .withPrereleaseContent("spec: content");

      doReturn(Optional.of(mock(ApiReference.class)))
        .when(apiReferenceRepositoryMock)
        .findById(
          ApiReference.ApiReferenceId.builder()
            .otelServiceName(incomingPrerelease.getOtelServiceName())
            .apiName(incomingPrerelease.getApiName())
            .apiVersion(incomingPrerelease.getApiVersion())
            .build()
        );

      assertThatThrownBy(() ->
        fixture.persist(incomingPrerelease)
      ).isInstanceOf(ApiAlreadyIndexedException.class);

      verify(apiReferenceRepositoryMock, never()).save(any(ApiReference.class));
    }

    @Test
    @VerifiesCon({
      ConTraceables.CON_007_STABLE_API_REFERENCE_IS_IMMUTABLE_ONCE_INDEXED,
      ConTraceables.CON_008_STABLE_SUBMISSION_CANNOT_CARRY_PRERELEASE_CONTENT,
    })
    void shouldThrowAlreadyIndexedException_whenExistingIsStable_evenIfIncomingContentIsInvalid() {
      var invalidStableReference = apiReference
        .withPrerelease(false)
        .withPrereleaseContent("spec: content");

      doReturn(Optional.of(mock(ApiReference.class)))
        .when(apiReferenceRepositoryMock)
        .findById(
          ApiReference.ApiReferenceId.builder()
            .otelServiceName(invalidStableReference.getOtelServiceName())
            .apiName(invalidStableReference.getApiName())
            .apiVersion(invalidStableReference.getApiVersion())
            .build()
        );

      assertThatThrownBy(() ->
        fixture.persist(invalidStableReference)
      ).isInstanceOf(ApiAlreadyIndexedException.class);

      verify(apiReferenceRepositoryMock, never()).save(any(ApiReference.class));
    }

    @Test
    @VerifiesSw(
      SwTraceables.SW_026_PRERELEASE_RESUBMISSION_REPLACES_THE_PRIOR_ENTRY
    )
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
    @VerifiesSw(
      SwTraceables.SW_026_PRERELEASE_RESUBMISSION_REPLACES_THE_PRIOR_ENTRY
    )
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
    @VerifiesCon(
      ConTraceables.CON_008_STABLE_SUBMISSION_CANNOT_CARRY_PRERELEASE_CONTENT
    )
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

    @Test
    @VerifiesCon(
      ConTraceables.CON_008_STABLE_SUBMISSION_CANNOT_CARRY_PRERELEASE_CONTENT
    )
    void shouldThrowException_whenReleaseContainsContent_andExistingIsPrerelease() {
      var existingPrerelease = mock(ApiReference.class);
      doReturn(true).when(existingPrerelease).isPrerelease();

      var invalidStableReference = apiReference
        .withPrerelease(false)
        .withPrereleaseContent("spec: content");

      var id = ApiReference.ApiReferenceId.builder()
        .otelServiceName(invalidStableReference.getOtelServiceName())
        .apiName(invalidStableReference.getApiName())
        .apiVersion(invalidStableReference.getApiVersion())
        .build();

      doReturn(Optional.of(existingPrerelease))
        .when(apiReferenceRepositoryMock)
        .findById(id);

      assertThatThrownBy(() ->
        fixture.persist(invalidStableReference)
      ).isInstanceOf(InvalidReleaseWithContentException.class);

      verify(apiReferenceRepositoryMock, never()).deleteById(id);
      verify(apiReferenceRepositoryMock, never()).save(any(ApiReference.class));
    }
  }

  @Nested
  class FindAllServiceNamesTest {

    @Test
    void shouldReturnServiceNamesFromRepository() {
      var serviceNames = List.of("service-a", "service-b");
      doReturn(serviceNames)
        .when(apiReferenceRepositoryMock)
        .findDistinctServiceNames();

      var result = fixture.findAllServiceNames();

      assertThat(result).isSameAs(serviceNames);
    }
  }

  @Nested
  class FindAllApiNamesTest {

    @Test
    void shouldReturnAllApiNames_whenServiceNameIsNull() {
      var apiNames = List.of("api-a", "api-b");
      doReturn(apiNames)
        .when(apiReferenceRepositoryMock)
        .findDistinctApiNames();

      var result = fixture.findAllApiNames(null);

      assertThat(result).isSameAs(apiNames);
    }

    @Test
    void shouldReturnApiNamesFilteredByServiceName_whenServiceNameIsProvided() {
      var apiNames = List.of("api-a");
      doReturn(apiNames)
        .when(apiReferenceRepositoryMock)
        .findDistinctApiNamesByOtelServiceName("my-service");

      var result = fixture.findAllApiNames("my-service");

      assertThat(result).isSameAs(apiNames);
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
