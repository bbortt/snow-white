/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.api.rest.resource;

import static io.github.bbortt.snow.white.commons.web.PaginationUtils.HEADER_X_TOTAL_COUNT;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_YAML;
import static org.springframework.http.MediaType.TEXT_PLAIN;

import io.github.bbortt.snow.white.microservices.api.index.api.mapper.ApiReferenceMapper;
import io.github.bbortt.snow.white.microservices.api.index.api.rest.dto.GetAllApis200ResponseInner;
import io.github.bbortt.snow.white.microservices.api.index.api.rest.dto.GetAllApis500Response;
import io.github.bbortt.snow.white.microservices.api.index.domain.model.ApiReference;
import io.github.bbortt.snow.white.microservices.api.index.service.ApiIndexService;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith({ MockitoExtension.class })
class ApiIndexResourceUnitTest {

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
  class IngestApiTest {

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
      throws ApiAlreadyIndexedException, InvalidReleaseWithContentException {
      var domain = mock(ApiReference.class);
      doReturn(domain).when(apiReferenceMapperMock).fromDto(dto);

      ResponseEntity<@NonNull Void> response = fixture.ingestApi(dto);

      verify(apiIndexServiceMock).persist(domain);
      assertThat(response.getStatusCode()).isEqualTo(CREATED);
      assertThat(response.getBody()).isNull();
    }

    @Test
    void shouldNotPersistApi_whenItHasAlreadyBeenIngested()
      throws ApiAlreadyIndexedException, InvalidReleaseWithContentException {
      var domain = mock(ApiReference.class);
      doReturn(domain).when(apiReferenceMapperMock).fromDto(dto);

      doThrow(mock(ApiAlreadyIndexedException.class))
        .when(apiIndexServiceMock)
        .persist(domain);

      ResponseEntity<@NonNull Void> response = fixture.ingestApi(dto);

      assertThat(response.getStatusCode()).isEqualTo(CONFLICT);
      assertThat(response.getBody()).isNull();
    }

    @Test
    void shouldNotPersistApi_whenTheReceivedApiInformationIsInvalid()
      throws ApiAlreadyIndexedException, InvalidReleaseWithContentException {
      var domain = mock(ApiReference.class);
      doReturn(domain).when(apiReferenceMapperMock).fromDto(dto);

      var exception = new InvalidReleaseWithContentException();
      doThrow(exception).when(apiIndexServiceMock).persist(domain);

      ResponseEntity<@NonNull GetAllApis500Response> response =
        fixture.ingestApi(dto);

      assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
      assertThat(response.getBody()).isEqualTo(
        GetAllApis500Response.builder()
          .code(BAD_REQUEST.getReasonPhrase())
          .message(exception.getMessage())
          .build()
      );
    }
  }

  @Nested
  class CheckApiExistsTest {

    public static Stream<Boolean> shouldIncludePrereleases_whenRequested() {
      return Stream.of(TRUE, FALSE);
    }

    @MethodSource
    @ParameterizedTest
    void shouldIncludePrereleases_whenRequested(Boolean includePrereleases) {
      doReturn(true)
        .when(apiIndexServiceMock)
        .hasApiByInformationBeenIndexed("svc", "api", "v1", includePrereleases);

      ResponseEntity response = fixture.checkApiExists(
        "svc",
        "api",
        "v1",
        includePrereleases
      );

      assertThat(response).satisfies(
        r -> assertThat(r.getStatusCode()).isEqualTo(OK),
        r -> assertThat(r.getBody()).isNull()
      );
    }

    @Test
    void shouldReturnHttpNotFoundResponse_whenApiHasNotBeenIndexed() {
      doReturn(false)
        .when(apiIndexServiceMock)
        .hasApiByInformationBeenIndexed("svc", "api", "v1", false);

      ResponseEntity response = fixture.checkApiExists(
        "svc",
        "api",
        "v1",
        false
      );

      assertThat(response).satisfies(
        r -> assertThat(r.getStatusCode()).isEqualTo(NOT_FOUND),
        r ->
          assertThat(r.getBody())
            .asInstanceOf(type(GetAllApis500Response.class))
            .satisfies(
              body ->
                assertThat(body.getCode()).isEqualTo(
                  NOT_FOUND.getReasonPhrase()
                ),
              body ->
                assertThat(body.getMessage()).isEqualTo(
                  "No API specification exists for the given service name, API name, and version."
                )
            )
      );
    }
  }

  @Nested
  class GetAllServiceNamesTest {

    @Test
    void shouldReturnServiceNames() {
      var serviceNames = List.of("service-a", "service-b");
      doReturn(serviceNames).when(apiIndexServiceMock).findAllServiceNames();

      ResponseEntity<List<String>> response = fixture.getAllServiceNames();

      assertThat(response).satisfies(
        r -> assertThat(r.getStatusCode()).isEqualTo(OK),
        r ->
          assertThat(r.getBody()).containsExactlyInAnyOrder(
            serviceNames.toArray(String[]::new)
          )
      );
    }
  }

  @Nested
  class GetAllApiNamesTest {

    @Test
    void shouldReturnAllApiNames_whenServiceNameIsNull() {
      var apiNames = List.of("api-a", "api-b");
      doReturn(apiNames).when(apiIndexServiceMock).findAllApiNames(null);

      ResponseEntity<List<String>> response = fixture.getAllApiNames(null);

      assertThat(response).satisfies(
        r -> assertThat(r.getStatusCode()).isEqualTo(OK),
        r ->
          assertThat(r.getBody()).containsExactlyInAnyOrder(
            apiNames.toArray(String[]::new)
          )
      );
    }

    @Test
    void shouldReturnFilteredApiNames_whenServiceNameIsProvided() {
      var apiNames = singletonList("api-a");
      doReturn(apiNames)
        .when(apiIndexServiceMock)
        .findAllApiNames("my-service");

      ResponseEntity<List<String>> response = fixture.getAllApiNames(
        "my-service"
      );

      assertThat(response).satisfies(
        r -> assertThat(r.getStatusCode()).isEqualTo(OK),
        r ->
          assertThat(r.getBody()).containsExactlyInAnyOrder(
            apiNames.toArray(String[]::new)
          )
      );
    }
  }

  @Nested
  class GetAllApisTest {

    @Test
    void shouldFetchAllEntities() {
      var page = 0;
      var size = 10;
      var sort = "apiVersion,asc";

      var serviceName = "serviceName";
      var apiName = "apiName";

      var domain1 = mock(ApiReference.class);
      var domain2 = mock(ApiReference.class);

      Page<@NonNull ApiReference> referencedApis = new PageImpl<>(
        List.of(domain1, domain2)
      );

      ArgumentCaptor<Pageable> pageableArgumentCaptor = captor();
      doReturn(referencedApis)
        .when(apiIndexServiceMock)
        .findAllIngestedApis(
          eq(serviceName),
          eq(apiName),
          pageableArgumentCaptor.capture()
        );

      var dto1 = mock(GetAllApis200ResponseInner.class);
      doReturn(dto1).when(apiReferenceMapperMock).toDto(domain1);

      var dto2 = mock(GetAllApis200ResponseInner.class);
      doReturn(dto2).when(apiReferenceMapperMock).toDto(domain2);

      ResponseEntity<@NonNull List<GetAllApis200ResponseInner>> response =
        fixture.getAllApis(page, size, sort, serviceName, apiName);

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
        .findAllIngestedApis(
          isNull(),
          isNull(),
          pageableArgumentCaptor.capture()
        );

      ResponseEntity<@NonNull List<GetAllApis200ResponseInner>> response =
        fixture.getAllApis(page, size, sort, null, null);

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
  class GetApiDetailsTest {

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

  @Nested
  class GetRawApiContentTest {

    @Test
    void shouldReturnNotFound_whenApiDoesNotExist() {
      doReturn(Optional.empty())
        .when(apiIndexServiceMock)
        .findIngestedApi(SERVICE_NAME, API_NAME, API_VERSION);

      ResponseEntity<?> response = fixture.getRawApiContent(
        SERVICE_NAME,
        API_NAME,
        API_VERSION
      );

      assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
      assertThat(response.getBody())
        .asInstanceOf(type(GetAllApis500Response.class))
        .satisfies(
          body ->
            assertThat(body.getCode()).isEqualTo(NOT_FOUND.getReasonPhrase()),
          body ->
            assertThat(body.getMessage()).isEqualTo(
              "The API specification does not exist."
            )
        );
    }

    @Test
    void shouldReturnNotFound_whenApiIsNotPrerelease() {
      var domain = mock(ApiReference.class);
      doReturn(false).when(domain).isPrerelease();
      doReturn(Optional.of(domain))
        .when(apiIndexServiceMock)
        .findIngestedApi(SERVICE_NAME, API_NAME, API_VERSION);

      ResponseEntity<?> response = fixture.getRawApiContent(
        SERVICE_NAME,
        API_NAME,
        API_VERSION
      );

      assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
      assertThat(response.getBody())
        .asInstanceOf(type(GetAllApis500Response.class))
        .satisfies(
          body ->
            assertThat(body.getCode()).isEqualTo(NOT_FOUND.getReasonPhrase()),
          body ->
            assertThat(body.getMessage()).isEqualTo(
              "The API specification does not exist."
            )
        );
    }

    @Test
    void shouldReturnNotFound_whenPrereleaseContentIsNull() {
      var domain = mock(ApiReference.class);
      doReturn(true).when(domain).isPrerelease();
      doReturn(null).when(domain).getPrereleaseContent();
      doReturn(Optional.of(domain))
        .when(apiIndexServiceMock)
        .findIngestedApi(SERVICE_NAME, API_NAME, API_VERSION);

      ResponseEntity<?> response = fixture.getRawApiContent(
        SERVICE_NAME,
        API_NAME,
        API_VERSION
      );

      assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
      assertThat(response.getBody())
        .asInstanceOf(type(GetAllApis500Response.class))
        .satisfies(
          body ->
            assertThat(body.getCode()).isEqualTo(NOT_FOUND.getReasonPhrase()),
          body ->
            assertThat(body.getMessage()).isEqualTo(
              "The API specification is not a prerelease."
            )
        );
    }

    @Test
    void shouldReturnYamlContent_whenPrereleaseContentIsOpenApiSpec() {
      var openApiContent = """
        openapi: 3.1.2
        info:
          title: Test API
        """;

      var domain = mock(ApiReference.class);
      doReturn(true).when(domain).isPrerelease();
      doReturn(openApiContent).when(domain).getPrereleaseContent();
      doReturn(Optional.of(domain))
        .when(apiIndexServiceMock)
        .findIngestedApi(SERVICE_NAME, API_NAME, API_VERSION);

      ResponseEntity<?> response = fixture.getRawApiContent(
        SERVICE_NAME,
        API_NAME,
        API_VERSION
      );

      assertThat(response.getStatusCode()).isEqualTo(OK);
      assertThat(response.getHeaders().getContentType()).isEqualTo(
        APPLICATION_YAML
      );
      assertThat(response.getBody()).isEqualTo(openApiContent);
    }

    @Test
    void shouldReturnPlainTextContent_whenPrereleaseContentIsNotOpenApiSpec() {
      var plainContent = "some raw content that is not an openapi spec";

      var domain = mock(ApiReference.class);
      doReturn(true).when(domain).isPrerelease();
      doReturn(plainContent).when(domain).getPrereleaseContent();
      doReturn(Optional.of(domain))
        .when(apiIndexServiceMock)
        .findIngestedApi(SERVICE_NAME, API_NAME, API_VERSION);

      ResponseEntity<?> response = fixture.getRawApiContent(
        SERVICE_NAME,
        API_NAME,
        API_VERSION
      );

      assertThat(response.getStatusCode()).isEqualTo(OK);
      assertThat(response.getHeaders().getContentType()).isEqualTo(TEXT_PLAIN);
      assertThat(response.getBody()).isEqualTo(plainContent);
    }
  }
}
