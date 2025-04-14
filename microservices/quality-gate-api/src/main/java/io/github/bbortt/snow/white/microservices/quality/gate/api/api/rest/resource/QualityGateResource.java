package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.resource;

import static io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.mapper.ObjectUtils.copyNonNullFields;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.QualityGateApi;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.Error;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.GetAllQualityGates200Response;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.mapper.OpenApiCriterionDoesNotExistException;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.mapper.QualityGateConfigurationMapper;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.QualityGateService;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.ConfigurationDoesNotExistException;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.ConfigurationNameAlreadyExistsException;
import java.net.URI;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QualityGateResource implements QualityGateApi {

  private final QualityGateConfigurationMapper qualityGateConfigurationMapper;
  private final QualityGateService qualityGateService;

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
        qualityGateConfig
      );
    }
  }

  private static ResponseEntity<
    Error
  > newHttpConflictResponseQualityGateConfigNameAlreadyExists(
    QualityGateConfig qualityGateConfig
  ) {
    return ResponseEntity.status(CONFLICT).body(
      Error.builder()
        .code(CONFLICT.getReasonPhrase())
        .message(
          format(
            "Quality-Gate configuration with name '%s' already exists",
            qualityGateConfig.getName()
          )
        )
        .build()
    );
  }

  @Override
  public ResponseEntity deleteQualityGate(String name) {
    try {
      qualityGateService.deleteByName(name);
    } catch (ConfigurationDoesNotExistException e) {
      return newHttpNotFoundResponseQualityGateConfigDoesNotExist(name);
    }

    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<GetAllQualityGates200Response> getAllQualityGates() {
    var qualityGateConfigNames =
      qualityGateService.getAllQualityGateConfigNames();

    return ResponseEntity.ok(
      GetAllQualityGates200Response.builder()
        .names(new ArrayList<>(qualityGateConfigNames))
        .build()
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
      return newHttpNotFoundResponseQualityGateConfigDoesNotExist(name);
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
      return newHttpNotFoundResponseQualityGateConfigDoesNotExist(name);
    } catch (OpenApiCriterionDoesNotExistException e) {
      return ResponseEntity.status(BAD_REQUEST).body(
        Error.builder()
          .code(BAD_REQUEST.getReasonPhrase())
          .message(e.getMessage())
          .build()
      );
    }
  }

  private static ResponseEntity<
    Error
  > newHttpNotFoundResponseQualityGateConfigDoesNotExist(String name) {
    return ResponseEntity.status(NOT_FOUND).body(
      Error.builder()
        .code(NOT_FOUND.getReasonPhrase())
        .message(
          format(
            "Quality-Gate configuration with name '%s' does not exist",
            name
          )
        )
        .build()
    );
  }
}
