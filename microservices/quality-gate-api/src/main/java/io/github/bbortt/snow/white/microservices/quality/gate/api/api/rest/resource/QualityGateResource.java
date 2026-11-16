/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.resource;

import static io.github.bbortt.snow.white.commons.web.PaginationUtils.generatePaginationHttpHeaders;
import static io.github.bbortt.snow.white.commons.web.PaginationUtils.toPageable;
import static io.github.bbortt.snow.white.commons.web.QualityGateOwnership.OWNERSHIP_FILE_EMPTY;
import static io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.mapper.ObjectUtils.copyNonNullFields;
import static io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.resource.QualityGateSortDefinition.QUALITY_GATE_SORT;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.QualityGateApi;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.Error;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.mapper.OpenApiCriterionDoesNotExistException;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.mapper.QualityGateConfigurationMapper;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.GateOwnershipService;
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

  /**
   * Error code the mode's refusal carries, so a caller tells it apart from the {@code 409} that
   * means only "that name is taken" — which is why sharing the status costs them nothing.
   */
  static final String GATES_ARE_FILE_OWNED = "GATES_ARE_FILE_OWNED";

  /**
   * Appended to the {@code 404} of a read against an instance whose gate file defines no gates. The
   * gates are this service's to own, so the explanation why no name could have matched is this
   * service's to write; a caller surfaces it rather than reassembling it from the ownership header.
   */
  static final String GATE_FILE_DEFINES_NO_GATES =
    "This instance's Quality-Gates are defined by its gate file, and that file defines none: add the Quality-Gate to the file and restart this instance!";

  private final GateOwnershipService gateOwnershipService;
  private final QualityGateConfigurationMapper qualityGateConfigurationMapper;
  private final QualityGateService qualityGateService;

  private static ResponseEntity<@NonNull Error> newHttpConflictResponseGatesAreFileOwned() {
    return ResponseEntity.status(CONFLICT).body(
      Error.builder()
        .code(GATES_ARE_FILE_OWNED)
        .message(
          "This instance's Quality-Gates are defined by its gate file. Change the file and restart the instance; this API cannot write them!"
        )
        .build()
    );
  }

  private static ResponseEntity<@NonNull Error> newHttpBadRequestResponseQualityGateConfigUnmodifiable(
    String message
  ) {
    return ResponseEntity.status(BAD_REQUEST).body(
      Error.builder()
        .code(BAD_REQUEST.getReasonPhrase())
        .message(message)
        .build()
    );
  }

  private static ResponseEntity<@NonNull Error> newHttpConflictResponseQualityGateConfigNameAlreadyExists(
    String message
  ) {
    return ResponseEntity.status(CONFLICT).body(
      Error.builder().code(CONFLICT.getReasonPhrase()).message(message).build()
    );
  }

  private static ResponseEntity<@NonNull Error> newHttpNotFoundResponseQualityGateConfigDoesNotExist(
    String message
  ) {
    return ResponseEntity.status(NOT_FOUND).body(
      Error.builder().code(NOT_FOUND.getReasonPhrase()).message(message).build()
    );
  }

  /**
   * The create endpoint of the quality-gate CRUD contract: 201 with the created resource, or 409
   * on a duplicate name — and 409 outright while this instance's gates are file-owned.
   */
  @RealizesSw({
    SwTraceables.SW_009_QUALITY_GATE_CRUD_CONTRACT,
    SwTraceables.SW_051_MANAGED_INSTANCE_REJECTS_CRUD_WRITES_WITH_CONFLICT,
  })
  @Override
  public ResponseEntity createQualityGate(QualityGateConfig qualityGateConfig) {
    if (gateOwnershipService.isFileOwned()) {
      return newHttpConflictResponseGatesAreFileOwned();
    }

    try {
      var createdQualityGateConfiguration = qualityGateService.persist(
        qualityGateConfigurationMapper.toInitialEntityIgnoringRelationships(
          qualityGateConfig
        ),
        qualityGateConfig.getOpenApiCoverageCriteria()
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

  /**
   * The delete endpoint of the quality-gate CRUD contract: 204 on success, 404 for an unknown
   * name, 400 when the gate is predefined and therefore unmodifiable.
   *
   * <p>While the gates are file-owned the refusal comes first and reads nothing, so a delete of a
   * name that does not exist earns the mode's 409 rather than a 404 a caller would reasonably read
   * as "writes work here".
   */
  @RealizesSw({
    SwTraceables.SW_009_QUALITY_GATE_CRUD_CONTRACT,
    SwTraceables.SW_051_MANAGED_INSTANCE_REJECTS_CRUD_WRITES_WITH_CONFLICT,
  })
  @Override
  public ResponseEntity deleteQualityGate(String name) {
    if (gateOwnershipService.isFileOwned()) {
      return newHttpConflictResponseGatesAreFileOwned();
    }

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

  /**
   * The list endpoint of the quality-gate CRUD contract: paginated, with pagination headers on
   * the response.
   */
  @RealizesSw(SwTraceables.SW_009_QUALITY_GATE_CRUD_CONTRACT)
  @Override
  public ResponseEntity<@NonNull List<QualityGateConfig>> getAllQualityGates(
    Integer page,
    Integer size,
    String sort
  ) {
    var qualityGateConfigurations =
      qualityGateService.findAllQualityGateConfigurations(
        toPageable(page, size, sort, QUALITY_GATE_SORT)
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

  /**
   * The get-by-name endpoint of the quality-gate CRUD contract: 200 with the resource, or 404 for
   * an unknown name.
   *
   * <p>Where the instance's gate file defines no gates, that 404 says so, because no name could
   * have matched and a caller told only "it does not exist" goes looking for a typo it will not
   * find. The two have opposite fixes — put the gate in the file and restart, versus correct the
   * name. A file-owned instance that holds gates but not this one is answered exactly as an
   * unmanaged one is: the mode is not an excuse to blur a plain typo.
   */
  @RealizesSw({
    SwTraceables.SW_009_QUALITY_GATE_CRUD_CONTRACT,
    SwTraceables.SW_052_EMPTY_FILE_OWNED_INSTANCE_NAMES_ITS_FILE,
  })
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
        OWNERSHIP_FILE_EMPTY.equals(gateOwnershipService.currentOwnership())
          ? e.getMessage() + " " + GATE_FILE_DEFINES_NO_GATES
          : e.getMessage()
      );
    }
  }

  /**
   * The update endpoint of the quality-gate CRUD contract: 200 with the merge-patched resource,
   * 404 for an unknown name, 400 for an unknown criterion or a predefined (unmodifiable) gate —
   * and 409 outright while this instance's gates are file-owned.
   */
  @RealizesSw({
    SwTraceables.SW_009_QUALITY_GATE_CRUD_CONTRACT,
    SwTraceables.SW_051_MANAGED_INSTANCE_REJECTS_CRUD_WRITES_WITH_CONFLICT,
  })
  @Override
  public ResponseEntity updateQualityGate(
    String name,
    QualityGateConfig qualityGateConfig
  ) {
    if (gateOwnershipService.isFileOwned()) {
      return newHttpConflictResponseGatesAreFileOwned();
    }

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
