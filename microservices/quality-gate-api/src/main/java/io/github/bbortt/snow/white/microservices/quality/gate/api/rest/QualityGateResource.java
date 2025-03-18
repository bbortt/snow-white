package io.github.bbortt.snow.white.microservices.quality.gate.api.rest;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import io.github.bbortt.snow.white.commons.testing.VisibleForTesting;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.CreateQualityGate201Response;
import io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.Error;
import io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.QualityGateService;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QualityGateResource implements QualityGateApi {

  @VisibleForTesting
  static final String QUALITY_GATE_CREATED_MESSAGE =
    "Quality-Gate configuration created successfully";

  private final ConversionService conversionService;
  private final QualityGateService qualityGateService;

  @Override
  public ResponseEntity createQualityGate(QualityGateConfig qualityGateConfig) {
    try {
      qualityGateService.persist(
        requireNonNull(
          conversionService.convert(
            qualityGateConfig,
            QualityGateConfiguration.class
          )
        )
      );
    } catch (QualityGateService.ConfigurationNameAlreadyExistsException e) {
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

    return ResponseEntity.created(
      URI.create(format("/v1/quality-gates/%s", qualityGateConfig.getName()))
    ).body(
      CreateQualityGate201Response.builder()
        .name(qualityGateConfig.getName())
        .message(QUALITY_GATE_CREATED_MESSAGE)
        .build()
    );
  }

  @Override
  public ResponseEntity getQualityGateByName(String name) {
    try {
      return ResponseEntity.ok(
        conversionService.convert(
          qualityGateService.findByName(name),
          QualityGateConfig.class
        )
      );
    } catch (QualityGateService.ConfigurationDoesNotExistException e) {
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
}
