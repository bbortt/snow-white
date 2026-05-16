/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.service;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.CONTENT_TYPE_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.ERROR_RESPONSE_CODE_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.HTTP_METHOD_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.NO_UNDOCUMENTED_ERROR_RESPONSE_CODES;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.NO_UNDOCUMENTED_POSITIVE_RESPONSE_CODES;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.NO_UNDOCUMENTED_RESPONSE_CODES;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.OPERATION_SUCCESS_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.OPTIONAL_PARAMETER_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.PARAMETER_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.POSITIVE_RESPONSE_CODE_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.REQUIRED_ERROR_FIELDS_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.REQUIRED_PARAMETER_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.RESPONSE_CODE_COVERAGE;
import static java.lang.Boolean.TRUE;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateOpenApiCoverageMapping;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.OpenApiCoverageConfigurationRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@NullMarked
@RequiredArgsConstructor
public final class DefaultOpenApiQualityGates {

  private final OpenApiCoverageConfigurationRepository openApiCoverageConfigurationRepository;

  public Set<
    QualityGateConfiguration
  > getDefaultOpenApiCoverageConfigurations() {
    // LinkedHashSet preserves insertion order for deterministic iteration
    var gates = new LinkedHashSet<QualityGateConfiguration>();
    gates.add(getBasicCoverage());
    gates.add(getFullFeature());
    gates.add(getMinimal());
    gates.add(getDryRun());
    return gates;
  }

  private QualityGateConfiguration getBasicCoverage() {
    var qualityGateConfiguration = QualityGateConfiguration.builder()
      .name("basic-coverage")
      .description(
        "A pragmatic balance of common expectations without requiring deep error validation."
      )
      .isPredefined(TRUE)
      .minCoveragePercentage(80)
      .build();

    addAllOpenApiCoverageCriteria(
      qualityGateConfiguration,
      Stream.of(
        PATH_COVERAGE,
        HTTP_METHOD_COVERAGE,
        OPERATION_SUCCESS_COVERAGE,
        POSITIVE_RESPONSE_CODE_COVERAGE,
        REQUIRED_PARAMETER_COVERAGE,
        NO_UNDOCUMENTED_POSITIVE_RESPONSE_CODES
      )
    );

    return qualityGateConfiguration;
  }

  private QualityGateConfiguration getFullFeature() {
    var qualityGateConfiguration = QualityGateConfiguration.builder()
      .name("full-feature")
      .description(
        "The most complete and strict configuration, useful for production-readiness or auditing."
      )
      .isPredefined(TRUE)
      .build();

    addAllOpenApiCoverageCriteria(
      qualityGateConfiguration,
      Stream.of(
        PATH_COVERAGE,
        HTTP_METHOD_COVERAGE,
        OPERATION_SUCCESS_COVERAGE,
        RESPONSE_CODE_COVERAGE,
        ERROR_RESPONSE_CODE_COVERAGE,
        POSITIVE_RESPONSE_CODE_COVERAGE,
        REQUIRED_PARAMETER_COVERAGE,
        OPTIONAL_PARAMETER_COVERAGE,
        PARAMETER_COVERAGE,
        CONTENT_TYPE_COVERAGE,
        REQUIRED_ERROR_FIELDS_COVERAGE,
        NO_UNDOCUMENTED_RESPONSE_CODES,
        NO_UNDOCUMENTED_ERROR_RESPONSE_CODES,
        NO_UNDOCUMENTED_POSITIVE_RESPONSE_CODES
      )
    );

    return qualityGateConfiguration;
  }

  private QualityGateConfiguration getMinimal() {
    var qualityGateConfiguration = QualityGateConfiguration.builder()
      .name("minimal")
      .description(
        "Just enough to ensure the API is reachable at all expected endpoints."
      )
      .isPredefined(TRUE)
      .minCoveragePercentage(80)
      .build();

    addAllOpenApiCoverageCriteria(
      qualityGateConfiguration,
      Stream.of(PATH_COVERAGE)
    );

    return qualityGateConfiguration;
  }

  private static QualityGateConfiguration getDryRun() {
    return QualityGateConfiguration.builder()
      .name("dry-run")
      .description(
        "Doesn't enforce any rules, but may be used to generate reports or test tooling."
      )
      .isPredefined(TRUE)
      .build();
  }

  private void addAllOpenApiCoverageCriteria(
    QualityGateConfiguration qualityGateConfiguration,
    Stream<OpenApiCoverageCriteria> openApiCriteria
  ) {
    openApiCriteria
      .map(openApiCriterion ->
        openApiCoverageConfigurationRepository
          .findByName(openApiCriterion.name())
          .orElseThrow(IllegalStateException::new)
      )
      .map(openApiCoverageConfiguration ->
        QualityGateOpenApiCoverageMapping.builder()
          .openApiCoverageConfiguration(openApiCoverageConfiguration)
          .qualityGateConfiguration(qualityGateConfiguration)
          .build()
      )
      .forEach(mapping ->
        qualityGateConfiguration.getOpenApiCoverageConfigurations().add(mapping)
      );
  }
}
