/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.api.rest;

import static io.github.bbortt.snow.white.commons.web.PaginationUtils.HEADER_X_TOTAL_COUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import io.github.bbortt.snow.white.microservices.api.index.api.mapper.ApiReferenceMapper;
import io.github.bbortt.snow.white.microservices.api.index.api.rest.dto.GetAllApis200ResponseInner;
import io.github.bbortt.snow.white.microservices.api.index.api.rest.dto.GetAllApis500Response;
import io.github.bbortt.snow.white.microservices.api.index.domain.model.ApiReference;
import io.github.bbortt.snow.white.microservices.api.index.service.ApiIndexService;
import io.github.bbortt.snow.white.microservices.api.index.service.exception.ApiAlreadyIndexedException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ApiIndexResourceTest {

  public static final String SERVICE_NAME = "svc";
  public static final String API_NAME = "api";
  public static final String API_VERSION = "v1";

  @Mock
  private ApiIndexService apiIndexServiceMock;

  @Mock
  private ApiReferenceMapper apiReferenceMapperMock;

  private ApiIndexResource fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiIndexResource(apiIndexServiceMock, apiReferenceMapperMock);
  }

  @Nested
  class IngestApi {

    private GetAllApis200ResponseInner dto;

    @BeforeEach
    void beforeEachSetup() {
      dto = GetAllApis200ResponseInner.builder()
        .serviceName(SERVICE_NAME)
        .apiName(API_NAME)
        .apiVersion(API_VERSION)
        .build();
    }

    @Test
    void shouldPersistApi_whenItDoesNotExistYet()
      throws ApiAlreadyIndexedException {
      var domain = mock(ApiReference.class);
      doReturn(domain).when(apiReferenceMapperMock).fromDto(dto);

      ResponseEntity<@NonNull Void> response = fixture.ingestApi(dto);

      verify(apiIndexServiceMock).persist(domain);
      assertThat(response.getStatusCode()).isEqualTo(CREATED);
      assertThat(response.getBody()).isNull();
    }

    @Test
    void shouldNotPersistApi_whenItHasAlreadyBeenIngested()
      throws ApiAlreadyIndexedException {
      var domain = mock(ApiReference.class);
      doReturn(domain).when(apiReferenceMapperMock).fromDto(dto);

      doThrow(mock(ApiAlreadyIndexedException.class))
        .when(apiIndexServiceMock)
        .persist(domain);

      ResponseEntity<@NonNull Void> response = fixture.ingestApi(dto);

      assertThat(response.getStatusCode()).isEqualTo(CONFLICT);
      assertThat(response.getBody()).isNull();
    }
  }

  @Nested
  class CheckApiExists {

    public static Stream<Arguments> shouldIndicateApiExistenceWithHttpStatus() {
      return Stream.of(arguments(true, OK), arguments(false, NOT_FOUND));
    }

    @MethodSource
    @ParameterizedTest
    void shouldIndicateApiExistenceWithHttpStatus(
      Boolean apiExists,
      HttpStatus responseStatus
    ) {
      doReturn(apiExists)
        .when(apiIndexServiceMock)
        .hasApiByInformationBeenIndexed("svc", "api", "v1");

      ResponseEntity<@NonNull Void> response = fixture.checkApiExists(
        "svc",
        "api",
        "v1"
      );

      assertThat(response).satisfies(
        r -> assertThat(r.getStatusCode()).isEqualTo(responseStatus),
        r -> assertThat(r.getBody()).isNull()
      );
    }
  }

  @Nested
  class GetAllApis {

    @Test
    void shouldFetchAllEntities() {
      var page = 0;
      var size = 10;
      var sort = "apiVersion,asc";

      var domain1 = mock(ApiReference.class);
      var domain2 = mock(ApiReference.class);

      Page<@NonNull ApiReference> referencedApis = new PageImpl<>(
        List.of(domain1, domain2)
      );

      ArgumentCaptor<Pageable> pageableArgumentCaptor = captor();
      doReturn(referencedApis)
        .when(apiIndexServiceMock)
        .findAllIngestedApis(pageableArgumentCaptor.capture());

      var dto1 = mock(GetAllApis200ResponseInner.class);
      doReturn(dto1).when(apiReferenceMapperMock).toDto(domain1);

      var dto2 = mock(GetAllApis200ResponseInner.class);
      doReturn(dto2).when(apiReferenceMapperMock).toDto(domain2);

      ResponseEntity<@NonNull List<GetAllApis200ResponseInner>> response =
        fixture.getAllApis(page, size, sort);

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(OK),
          r -> assertThat(r.getBody()).containsExactly(dto1, dto2),
          r ->
            assertThat(r.getHeaders().toSingleValueMap())
              .hasSize(1)
              .containsEntry(HEADER_X_TOTAL_COUNT, "2")
        );

      assertThat(pageableArgumentCaptor.getValue())
        .isNotNull()
        .satisfies(
          p -> assertThat(p.getPageNumber()).isEqualTo(page),
          p -> assertThat(p.getOffset()).isEqualTo(page),
          p -> assertThat(p.getPageSize()).isEqualTo(size),
          p ->
            assertThat(p.getSort()).isEqualTo(
              Sort.by(Sort.Direction.ASC, "apiVersion")
            )
        );
    }

    @Test
    void shouldHandleEmptyPageOfReferencedApis() {
      var page = 1;
      var size = 10;
      var sort = "apiVersion,desc";

      Page<@NonNull ApiReference> referencedApis = mock();

      ArgumentCaptor<Pageable> pageableArgumentCaptor = captor();
      doReturn(referencedApis)
        .when(apiIndexServiceMock)
        .findAllIngestedApis(pageableArgumentCaptor.capture());

      ResponseEntity<@NonNull List<GetAllApis200ResponseInner>> response =
        fixture.getAllApis(page, size, sort);

      verifyNoInteractions(apiReferenceMapperMock);

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(OK),
          r -> assertThat(r.getBody()).isEmpty(),
          r ->
            assertThat(r.getHeaders().toSingleValueMap())
              .hasSize(1)
              .containsEntry(HEADER_X_TOTAL_COUNT, "0")
        );

      assertThat(pageableArgumentCaptor.getValue())
        .isNotNull()
        .satisfies(
          p -> assertThat(p.getPageNumber()).isEqualTo(page),
          p -> assertThat(p.getOffset()).isEqualTo(page * size),
          p -> assertThat(p.getPageSize()).isEqualTo(size),
          p ->
            assertThat(p.getSort()).isEqualTo(
              Sort.by(Sort.Direction.DESC, "apiVersion")
            )
        );
    }
  }

  @Nested
  class GetApiDetails {

    @Test
    void shouldFetchApiDetails() {
      var domain = mock(ApiReference.class);
      var dto = mock(GetAllApis200ResponseInner.class);

      doReturn(Optional.of(domain))
        .when(apiIndexServiceMock)
        .findIngestedApi(SERVICE_NAME, API_NAME, API_VERSION);
      doReturn(dto).when(apiReferenceMapperMock).toDto(domain);

      ResponseEntity<?> response = fixture.getApiDetails(
        SERVICE_NAME,
        API_NAME,
        API_VERSION
      );

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isSameAs(dto);
    }

    @Test
    void shouldReturnNotFoundResponse_whenEntityDoesNotExist() {
      doReturn(Optional.empty())
        .when(apiIndexServiceMock)
        .findIngestedApi(SERVICE_NAME, API_NAME, API_VERSION);

      ResponseEntity<?> response = fixture.getApiDetails(
        SERVICE_NAME,
        API_NAME,
        API_VERSION
      );

      assertThat(response).satisfies(
        r -> assertThat(r.getStatusCode()).isEqualTo(NOT_FOUND),
        r ->
          assertThat(r.getBody())
            .asInstanceOf(type(GetAllApis500Response.class))
            .satisfies(
              e ->
                assertThat(e.getCode()).isEqualTo(NOT_FOUND.getReasonPhrase()),
              e ->
                assertThat(e.getMessage()).isEqualTo(
                  "No API specification exists for the given service name, API name, and version."
                )
            )
      );
    }
  }
}
