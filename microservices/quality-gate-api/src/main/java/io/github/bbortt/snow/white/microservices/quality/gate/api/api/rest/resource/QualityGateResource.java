/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.resource;

import static io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.mapper.ObjectUtils.copyNonNullFields;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.QualityGateApi;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.Error;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.mapper.OpenApiCriterionDoesNotExistException;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.mapper.QualityGateConfigurationMapper;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.QualityGateService;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.ConfigurationDoesNotExistException;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.ConfigurationNameAlreadyExistsException;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.UnmodifiableConfigurationException;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QualityGateResource implements QualityGateApi {

  private final QualityGateConfigurationMapper qualityGateConfigurationMapper;
  private final QualityGateService qualityGateService;

  private static ResponseEntity<
    Error
  > newHttpBadRequestResponseQualityGateConfigUnmodifiable(String message) {
    return ResponseEntity.status(BAD_REQUEST).body(
      Error.builder()
        .code(BAD_REQUEST.getReasonPhrase())
        .message(message)
        .build()
    );
  }

  private static ResponseEntity<
    Error
  > newHttpConflictResponseQualityGateConfigNameAlreadyExists(String message) {
    return ResponseEntity.status(CONFLICT).body(
      Error.builder().code(CONFLICT.getReasonPhrase()).message(message).build()
    );
  }

  private static ResponseEntity<
    Error
  > newHttpNotFoundResponseQualityGateConfigDoesNotExist(String message) {
    return ResponseEntity.status(NOT_FOUND).body(
      Error.builder().code(NOT_FOUND.getReasonPhrase()).message(message).build()
    );
  }

  @Override
  public ResponseEntity createQualityGate(QualityGateConfig qualityGateConfig) {
    try {
      var createdQualityGateConfiguration = qualityGateService.persist(
        qualityGateConfigurationMapper.toEntity(qualityGateConfig)
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
  public ResponseEntity<List<QualityGateConfig>> getAllQualityGates() {
    var qualityGateConfigurations =
      qualityGateService.getAllQualityGateConfigurations();

    return ResponseEntity.ok(
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
      var qualityGateConfiguration = qualityGateService.findByName(name);
      var updates = qualityGateConfigurationMapper.toEntity(qualityGateConfig);

      copyNonNullFields(updates, qualityGateConfiguration);

      var updatedQualityGateConfiguration = qualityGateService.update(
        qualityGateConfiguration
      );

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
}
