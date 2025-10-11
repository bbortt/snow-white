/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.resource;

import static io.github.bbortt.snow.white.commons.web.PaginationUtils.HEADER_X_TOTAL_COUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.Error;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.mapper.OpenApiCriterionDoesNotExistException;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.mapper.QualityGateConfigurationMapper;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.QualityGateService;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.ConfigurationDoesNotExistException;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.ConfigurationNameAlreadyExistsException;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.UnmodifiableConfigurationException;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@ExtendWith({ MockitoExtension.class })
class QualityGateResourceTest {

  @Mock
  private QualityGateConfigurationMapper qualityGateConfigurationMapperMock;

  @Mock
  private QualityGateService qualityGateServiceMock;

  private QualityGateResource fixture;

  private static void verifyResponseIsHttpNotFoundWithMessage(
    ResponseEntity response,
    String name
  ) {
    assertThat(response)
      .isNotNull()
      .satisfies(
        r -> assertThat(r.getStatusCode()).isEqualTo(NOT_FOUND),
        r ->
          assertThat(r.getBody())
            .asInstanceOf(type(Error.class))
            .satisfies(
              e ->
                assertThat(e.getCode()).isEqualTo(NOT_FOUND.getReasonPhrase()),
              e ->
                assertThat(e.getMessage()).isEqualTo(
                  "Quality-Gate configuration '%s' does not exist!",
                  name
                )
            )
      );
  }

  private static void verifyResponseIsHttpBadRequestWithMessage(
    ResponseEntity response,
    String name
  ) {
    assertThat(response)
      .isNotNull()
      .satisfies(
        r -> assertThat(r.getStatusCode()).isEqualTo(BAD_REQUEST),
        r ->
          assertThat(r.getBody())
            .asInstanceOf(type(Error.class))
            .satisfies(
              e ->
                assertThat(e.getCode()).isEqualTo(
                  BAD_REQUEST.getReasonPhrase()
                ),
              e ->
                assertThat(e.getMessage()).isEqualTo(
                  "The Quality-Gate configuration '%s' is not modifiable!",
                  name
                )
            )
      );
  }

  @BeforeEach
  void beforeEachSetup() {
    fixture = new QualityGateResource(
      qualityGateConfigurationMapperMock,
      qualityGateServiceMock
    );
  }

  @Nested
  class CreateQualityGate {

    private QualityGateConfig qualityGateConfig;

    @BeforeEach
    void beforeEachSetup() {
      qualityGateConfig = QualityGateConfig.builder()
        .name("TestQualityGate")
        .build();
    }

    @Test
    void shouldReturnCreatedResponse()
      throws ConfigurationNameAlreadyExistsException {
      var qualityGateConfiguration = new QualityGateConfiguration();
      doReturn(qualityGateConfiguration)
        .when(qualityGateConfigurationMapperMock)
        .toEntity(qualityGateConfig);

      var persistedQualityGateConfiguration = new QualityGateConfiguration();
      doReturn(persistedQualityGateConfiguration)
        .when(qualityGateServiceMock)
        .persist(qualityGateConfiguration);

      var responseEntity = new QualityGateConfig();
      doReturn(responseEntity)
        .when(qualityGateConfigurationMapperMock)
        .toDto(persistedQualityGateConfiguration);

      var response = fixture.createQualityGate(qualityGateConfig);

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(CREATED),
          r ->
            assertThat(r.getBody())
              .asInstanceOf(type(QualityGateConfig.class))
              .isEqualTo(responseEntity)
        );
    }

    @Test
    void shouldReturnConflictResponse_whenConfigurationAlreadyExists()
      throws ConfigurationNameAlreadyExistsException {
      var qualityGateConfiguration = QualityGateConfiguration.builder()
        .name(qualityGateConfig.getName())
        .build();

      doReturn(qualityGateConfiguration)
        .when(qualityGateConfigurationMapperMock)
        .toEntity(qualityGateConfig);
      doThrow(
        new ConfigurationNameAlreadyExistsException(
          qualityGateConfiguration.getName()
        )
      )
        .when(qualityGateServiceMock)
        .persist(qualityGateConfiguration);

      var response = fixture.createQualityGate(qualityGateConfig);

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(CONFLICT),
          r ->
            assertThat(r.getBody())
              .asInstanceOf(type(Error.class))
              .satisfies(
                e -> assertThat(e.getCode()).isEqualTo("Conflict"),
                e ->
                  assertThat(e.getMessage()).isEqualTo(
                    "Quality-Gate configuration '%s' does already exist!",
                    qualityGateConfig.getName()
                  )
              )
        );
    }

    @Test
    void createdResponseShouldContainCorrectLocationUri() {
      var qualityGateConfiguration = new QualityGateConfiguration();
      doReturn(qualityGateConfiguration)
        .when(qualityGateConfigurationMapperMock)
        .toEntity(qualityGateConfig);

      var response = fixture.createQualityGate(qualityGateConfig);

      assertThat(response.getHeaders().getLocation()).isEqualTo(
        URI.create("/api/rest/v1/quality-gates/TestQualityGate")
      );
    }
  }

  @Nested
  class DeleteQualityGate {

    @Test
    void shouldDeleteConfiguration()
      throws ConfigurationDoesNotExistException, UnmodifiableConfigurationException {
      var name = "TestQualityGate";

      var response = fixture.deleteQualityGate(name);

      assertThat(response)
        .isNotNull()
        .extracting(ResponseEntity::getStatusCode)
        .isEqualTo(NO_CONTENT);

      verify(qualityGateServiceMock).deleteByName(name);
    }

    @Test
    void shouldReturnNotFoundResponse_whenConfigurationAlreadyExists()
      throws ConfigurationDoesNotExistException, UnmodifiableConfigurationException {
      var name = "NonExistingConfiguration";
      doThrow(new ConfigurationDoesNotExistException(name))
        .when(qualityGateServiceMock)
        .deleteByName(name);

      var response = fixture.deleteQualityGate(name);

      verifyResponseIsHttpNotFoundWithMessage(response, name);
    }

    @Test
    void shouldReturnNotFoundResponse_whenConfigurationIsUnmodifiable()
      throws ConfigurationDoesNotExistException, UnmodifiableConfigurationException {
      var name = "UnmodifiableConfiguration";
      doThrow(new UnmodifiableConfigurationException(name))
        .when(qualityGateServiceMock)
        .deleteByName(name);

      var response = fixture.deleteQualityGate(name);

      verifyResponseIsHttpBadRequestWithMessage(response, name);
    }
  }

  @Nested
  class GetAllQualityGates {

    @Test
    void shouldReturnListOfQualityGates() {
      var page = 0;
      var size = 10;
      var sort = "createdAt,desc";

      var qualityGateConfiguration1 = mock(QualityGateConfiguration.class);
      var qualityGateConfiguration2 = mock(QualityGateConfiguration.class);

      Page<QualityGateConfiguration> qualityGateConfigurationPage = mock();
      doReturn(2L).when(qualityGateConfigurationPage).getTotalElements();

      doReturn(qualityGateConfigurationPage)
        .when(qualityGateServiceMock)
        .findAllQualityGateConfigurations(any(Pageable.class));

      doReturn(Stream.of(qualityGateConfiguration1, qualityGateConfiguration2))
        .when(qualityGateConfigurationPage)
        .stream();

      var dto1 = mock(QualityGateConfig.class);
      doReturn(dto1)
        .when(qualityGateConfigurationMapperMock)
        .toDto(qualityGateConfiguration1);

      var dto2 = mock(QualityGateConfig.class);
      doReturn(dto2)
        .when(qualityGateConfigurationMapperMock)
        .toDto(qualityGateConfiguration2);

      ResponseEntity<List<QualityGateConfig>> response =
        fixture.getAllQualityGates(page, size, sort);

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(OK),
          r -> assertThat(r.getBody()).containsExactly(dto1, dto2),
          r ->
            assertThat(r.getHeaders().toSingleValueMap()).containsEntry(
              HEADER_X_TOTAL_COUNT,
              "2"
            )
        );
    }

    @Test
    void shouldHandleEmptyListOfQualityGates() {
      var page = 0;
      var size = 10;
      var sort = "createdAt,desc";

      Page<QualityGateConfiguration> qualityGateConfigurationPage = mock();
      doReturn(qualityGateConfigurationPage)
        .when(qualityGateServiceMock)
        .findAllQualityGateConfigurations(any(Pageable.class));

      doReturn(Stream.empty()).when(qualityGateConfigurationPage).stream();

      ResponseEntity<List<QualityGateConfig>> response =
        fixture.getAllQualityGates(page, size, sort);

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(OK),
          r -> assertThat(r.getBody()).isEmpty(),
          r ->
            assertThat(r.getHeaders().toSingleValueMap()).containsEntry(
              HEADER_X_TOTAL_COUNT,
              "0"
            )
        );
    }
  }

  @Nested
  class GetQualityGateByName {

    @Test
    void shouldReturnConfiguration() throws ConfigurationDoesNotExistException {
      var qualityGateConfiguration = new QualityGateConfiguration();
      doReturn(qualityGateConfiguration)
        .when(qualityGateServiceMock)
        .findByName(qualityGateConfiguration.getName());

      var qualityGateConfig = new QualityGateConfig();
      doReturn(qualityGateConfig)
        .when(qualityGateConfigurationMapperMock)
        .toDto(qualityGateConfiguration);

      var response = fixture.getQualityGateByName(
        qualityGateConfiguration.getName()
      );

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(OK),
          r -> assertThat(r.getBody()).isEqualTo(qualityGateConfig)
        );
    }

    @Test
    void shouldReturnNotFoundResponse_whenConfigurationDoesNotExists()
      throws ConfigurationDoesNotExistException {
      var name = "NonExistingConfiguration";
      doThrow(new ConfigurationDoesNotExistException(name))
        .when(qualityGateServiceMock)
        .findByName(name);

      var response = fixture.getQualityGateByName(name);

      verifyResponseIsHttpNotFoundWithMessage(response, name);
    }
  }

  @Nested
  class UpdateQualityGate {

    private static final String NAME = "TestQualityGate";

    private QualityGateConfig qualityGateConfig;

    @BeforeEach
    void beforeEachSetup() {
      qualityGateConfig = QualityGateConfig.builder().build();
    }

    @Test
    void shouldReturnUpdatedResponse()
      throws ConfigurationDoesNotExistException, UnmodifiableConfigurationException {
      var qualityGateConfiguration = new QualityGateConfiguration();
      doReturn(qualityGateConfiguration)
        .when(qualityGateServiceMock)
        .findByName(NAME);

      var qualityGateConfigurationWithUpdates = new QualityGateConfiguration();
      doReturn(qualityGateConfigurationWithUpdates)
        .when(qualityGateConfigurationMapperMock)
        .toEntity(qualityGateConfig);

      var persistedQualityGateConfiguration = new QualityGateConfiguration();
      doReturn(persistedQualityGateConfiguration)
        .when(qualityGateServiceMock)
        .update(qualityGateConfiguration);

      var responseEntity = new QualityGateConfig();
      doReturn(responseEntity)
        .when(qualityGateConfigurationMapperMock)
        .toDto(persistedQualityGateConfiguration);

      var response = fixture.updateQualityGate(NAME, qualityGateConfig);

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(OK),
          r ->
            assertThat(r.getBody())
              .asInstanceOf(type(QualityGateConfig.class))
              .isEqualTo(responseEntity)
        );
    }

    @Test
    void shouldReturnNotFoundResponse_whenConfigurationDoesNotExists()
      throws ConfigurationDoesNotExistException {
      doThrow(new ConfigurationDoesNotExistException(NAME))
        .when(qualityGateServiceMock)
        .findByName(NAME);

      var response = fixture.updateQualityGate(NAME, qualityGateConfig);

      verifyResponseIsHttpNotFoundWithMessage(response, NAME);

      verifyNoMoreInteractions(qualityGateServiceMock);
      verifyNoInteractions(qualityGateConfigurationMapperMock);
    }

    @Test
    void shouldReturnNotFoundResponse_whenConfigurationIsUnmodifiable()
      throws ConfigurationDoesNotExistException, UnmodifiableConfigurationException {
      var qualityGateConfiguration = new QualityGateConfiguration();
      doReturn(qualityGateConfiguration)
        .when(qualityGateServiceMock)
        .findByName(NAME);

      var qualityGateConfigurationWithUpdates = new QualityGateConfiguration();
      doReturn(qualityGateConfigurationWithUpdates)
        .when(qualityGateConfigurationMapperMock)
        .toEntity(qualityGateConfig);

      doThrow(new UnmodifiableConfigurationException(NAME))
        .when(qualityGateServiceMock)
        .update(qualityGateConfiguration);

      var response = fixture.updateQualityGate(NAME, qualityGateConfig);

      verifyResponseIsHttpBadRequestWithMessage(response, NAME);
    }

    @Test
    void shouldReturnBadRequestResponse_whenInvalidOpenApiCriterionSupplied()
      throws ConfigurationDoesNotExistException {
      var criteriaName = "criteriaName";
      doThrow(new OpenApiCriterionDoesNotExistException(criteriaName))
        .when(qualityGateServiceMock)
        .findByName(criteriaName);

      var response = fixture.updateQualityGate(criteriaName, qualityGateConfig);

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(BAD_REQUEST),
          r ->
            assertThat(r.getBody())
              .asInstanceOf(type(Error.class))
              .satisfies(
                e ->
                  assertThat(e.getCode()).isEqualTo(
                    BAD_REQUEST.getReasonPhrase()
                  ),
                e ->
                  assertThat(e.getMessage()).isEqualTo(
                    "OpenApi Criterion '%s' does not exist!",
                    criteriaName
                  )
              )
        );

      verifyNoMoreInteractions(qualityGateServiceMock);
      verifyNoInteractions(qualityGateConfigurationMapperMock);
    }
  }
}
