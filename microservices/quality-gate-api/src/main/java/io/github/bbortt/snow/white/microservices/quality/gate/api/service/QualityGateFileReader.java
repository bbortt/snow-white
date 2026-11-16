/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.service;

import static java.lang.Boolean.FALSE;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.quality.gate.api.config.QualityGateApiProperties;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.QualityGateFileException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads this instance's gate file, if it has one. Parsing only — the gate set is written by {@link
 * QualityGateService#applyQualityGateFile(List)}, which is the one writer a file-owned instance
 * has.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QualityGateFileReader {

  private static final TypeReference<
    List<QualityGateConfig>
  > QUALITY_GATE_LIST = new TypeReference<>() {};

  /**
   * What a gate's threshold is when the file does not state one: the schema's own default, and the
   * strictest value there is, so an omission can never quietly lower a bar.
   */
  private static final int UNSTATED_MIN_COVERAGE_PERCENTAGE = 100;

  /**
   * Strict on purpose, and deliberately not the application's response mapper: a misspelt key in a
   * gate file must fail the startup rather than silently leave the field at its default, which for
   * the criteria list would be a gate that enforces nothing.
   */
  private static final ObjectMapper QUALITY_GATE_FILE_MAPPER =
    JsonMapper.builder().enable(FAIL_ON_UNKNOWN_PROPERTIES).build();

  private final QualityGateApiProperties qualityGateApiProperties;
  private final ResourceLoader resourceLoader;
  private final Validator validator;

  /**
   * The gate definitions the file states, as either JSON or YAML — YAML is a superset of JSON, so
   * one parser accepts both and neither costs a dependency.
   *
   * <p>A configured file that is absent or unreadable is refused rather than interpreted: read as
   * an empty gate set, a volume that failed to mount would delete every gate the instance has. A
   * file that is present and empty is a different thing — a statement that this instance defines no
   * gates — and is returned as such.
   */
  @RealizesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
  public List<@NonNull QualityGateConfig> readQualityGateFile() {
    var location = requireNonNull(
      qualityGateApiProperties.getGateFile(),
      "Quality-Gate file location must be set to read it"
    );
    var resource = resourceLoader.getResource(location);

    if (!resource.exists()) {
      throw new QualityGateFileException(location, "it does not exist");
    }

    Object document;
    try (var inputStream = resource.getInputStream()) {
      document = new Yaml(new SafeConstructor(new LoaderOptions())).load(
        inputStream
      );
    } catch (IOException | YAMLException e) {
      throw new QualityGateFileException(
        location,
        "it could not be parsed as JSON or YAML",
        e
      );
    }

    if (isNull(document)) {
      logger.info(
        "Quality-Gate file '{}' is empty: this instance defines no Quality-Gates",
        location
      );
      return emptyList();
    }

    List<@NonNull QualityGateConfig> qualityGateConfigs;
    try {
      qualityGateConfigs = QUALITY_GATE_FILE_MAPPER.convertValue(
        document,
        QUALITY_GATE_LIST
      );
    } catch (DatabindException | IllegalArgumentException e) {
      throw new QualityGateFileException(
        location,
        "it is not a list of Quality-Gate definitions",
        e
      );
    }

    qualityGateConfigs.forEach(QualityGateFileReader::clearWhatTheFileOmits);
    assertEveryStatedValueIsValid(location, qualityGateConfigs);

    return qualityGateConfigs;
  }

  /**
   * A field the file omits is cleared rather than preserved, so each omission takes the value that
   * says "the file states nothing here": no criteria means none, and an unstated threshold takes
   * the schema's own default instead of reaching a non-null column as a startup {@code
   * NullPointerException}.
   *
   * <p>{@code isPredefined} is set rather than read, because a file-defined gate is never
   * predefined whatever the file says — requiring the operator to write a value that is then
   * overwritten would be asking for nothing.
   */
  @RealizesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
  private static void clearWhatTheFileOmits(
    QualityGateConfig qualityGateConfig
  ) {
    qualityGateConfig.setIsPredefined(FALSE);

    if (isNull(qualityGateConfig.getOpenApiCoverageCriteria())) {
      qualityGateConfig.setOpenApiCoverageCriteria(emptyList());
    }

    if (isNull(qualityGateConfig.getMinCoveragePercentage())) {
      qualityGateConfig.setMinCoveragePercentage(
        UNSTATED_MIN_COVERAGE_PERCENTAGE
      );
    }
  }

  /**
   * What the file does state answers to the same constraints the create endpoint applies to the
   * same payload — refused here, naming the gate and the field, rather than landing a threshold or
   * a name the API would never have accepted.
   */
  @RealizesSw(SwTraceables.SW_050_STARTUP_MAKES_THE_GATE_SET_EXACTLY_THE_FILE)
  private void assertEveryStatedValueIsValid(
    String location,
    List<QualityGateConfig> qualityGateConfigs
  ) {
    var violations = qualityGateConfigs
      .stream()
      .flatMap(qualityGateConfig ->
        validator.validate(qualityGateConfig).stream()
      )
      .map(QualityGateFileReader::describe)
      .sorted()
      .toList();

    if (!violations.isEmpty()) {
      throw new QualityGateFileException(
        location,
        "it defines invalid Quality-Gates: " + String.join("; ", violations)
      );
    }
  }

  private static String describe(
    ConstraintViolation<QualityGateConfig> violation
  ) {
    return format(
      "Quality-Gate '%s' %s %s",
      violation.getRootBean().getName(),
      violation.getPropertyPath(),
      violation.getMessage()
    );
  }
}
