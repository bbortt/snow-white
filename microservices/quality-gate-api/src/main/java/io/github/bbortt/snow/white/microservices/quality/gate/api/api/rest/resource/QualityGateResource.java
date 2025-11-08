/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.resource;

import static io.github.bbortt.snow.white.commons.web.PaginationUtils.generatePaginationHttpHeaders;
import static io.github.bbortt.snow.white.commons.web.PaginationUtils.toPageable;
import static io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.mapper.ObjectUtils.copyNonNullFields;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.QualityGateApi;
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
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QualityGateResource implements QualityGateApi {

  private final QualityGateConfigurationMapper qualityGateConfigurationMapper;
  private final QualityGateService qualityGateService;

  private static ResponseEntity<
    @NonNull Error
  > newHttpBadRequestResponseQualityGateConfigUnmodifiable(String message) {
    return ResponseEntity.status(BAD_REQUEST).body(
      Error.builder()
        .code(BAD_REQUEST.getReasonPhrase())
        .message(message)
        .build()
    );
  }

  private static ResponseEntity<
    @NonNull Error
  > newHttpConflictResponseQualityGateConfigNameAlreadyExists(String message) {
    return ResponseEntity.status(CONFLICT).body(
      Error.builder().code(CONFLICT.getReasonPhrase()).message(message).build()
    );
  }

  private static ResponseEntity<
    @NonNull Error
  > newHttpNotFoundResponseQualityGateConfigDoesNotExist(String message) {
    return ResponseEntity.status(NOT_FOUND).body(
      Error.builder().code(NOT_FOUND.getReasonPhrase()).message(message).build()
    );
  }

  @Override
  public ResponseEntity createQualityGate(QualityGateConfig qualityGateConfig) {
    try {
      var createdQualityGateConfiguration = qualityGateService.persist(
        qualityGateConfigurationMapper.toInitialEntityIgnoringRelationships(
          qualityGateConfig
        ),
        qualityGateConfig.getOpenApiCriteria()
      );

      return ResponseEntity.created(
        URI.create(
          format("/api/rest/v1/quality-gates/%s", qualityGateConfig.getName())
        )
      ).body(
        qualityGateConfigurationMapper.toDto(createdQualityGateConfiguration)
      );
    } catch (ConfigurationNameAlreadyExistsException e) {
      return newHttpConflictResponseQualityGateConfigNameAlreadyExists(
        e.getMessage()
      );
    }
  }

  @Override
  public ResponseEntity deleteQualityGate(String name) {
    try {
      qualityGateService.deleteByName(name);
    } catch (ConfigurationDoesNotExistException e) {
      return newHttpNotFoundResponseQualityGateConfigDoesNotExist(
        e.getMessage()
      );
    } catch (UnmodifiableConfigurationException e) {
      return newHttpBadRequestResponseQualityGateConfigUnmodifiable(
        e.getMessage()
      );
    }

    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<@NonNull List<QualityGateConfig>> getAllQualityGates(
    Integer page,
    Integer size,
    String sort
  ) {
    var qualityGateConfigurations =
      qualityGateService.findAllQualityGateConfigurations(
        toPageable(page, size, sort)
      );

    return ResponseEntity.ok()
      .headers(generatePaginationHttpHeaders(qualityGateConfigurations))
      .body(
        qualityGateConfigurations
          .stream()
          .map(qualityGateConfigurationMapper::toDto)
          .toList()
      );
  }

  @Override
  public ResponseEntity getQualityGateByName(String name) {
    try {
      return ResponseEntity.ok(
        qualityGateConfigurationMapper.toDto(
          qualityGateService.findByName(name)
        )
      );
    } catch (ConfigurationDoesNotExistException e) {
      return newHttpNotFoundResponseQualityGateConfigDoesNotExist(
        e.getMessage()
      );
    }
  }

  @Override
  public ResponseEntity updateQualityGate(
    String name,
    QualityGateConfig qualityGateConfig
  ) {
    try {
      var updatedQualityGateConfiguration =
        fetchAndUpdateExistingQualityGateConfig(name, qualityGateConfig);

      return ResponseEntity.ok(
        qualityGateConfigurationMapper.toDto(updatedQualityGateConfiguration)
      );
    } catch (ConfigurationDoesNotExistException e) {
      return newHttpNotFoundResponseQualityGateConfigDoesNotExist(
        e.getMessage()
      );
    } catch (OpenApiCriterionDoesNotExistException e) {
      return ResponseEntity.status(BAD_REQUEST).body(
        Error.builder()
          .code(BAD_REQUEST.getReasonPhrase())
          .message(e.getMessage())
          .build()
      );
    } catch (UnmodifiableConfigurationException e) {
      return newHttpBadRequestResponseQualityGateConfigUnmodifiable(
        e.getMessage()
      );
    }
  }

  private QualityGateConfiguration fetchAndUpdateExistingQualityGateConfig(
    String name,
    QualityGateConfig qualityGateConfig
  )
    throws ConfigurationDoesNotExistException, UnmodifiableConfigurationException {
    var qualityGateConfiguration = qualityGateService.findByName(name);
    var updates = qualityGateConfigurationMapper.toEntityForUpdate(
      qualityGateConfig,
      qualityGateConfiguration
    );

    copyNonNullFields(updates, qualityGateConfiguration);

    return qualityGateService.update(qualityGateConfiguration);
  }
}
