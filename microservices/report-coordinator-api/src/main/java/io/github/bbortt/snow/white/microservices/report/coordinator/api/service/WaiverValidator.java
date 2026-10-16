/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.Waiver.API_VERSION_WILDCARD;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.Waiver;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Everything about a submitted waiver that the trigger can answer before a calculation exists, and
 * therefore everything it must: an unanswerable waiver rejected here is a message the author can
 * still act on, where the same waiver accepted is a silent no-op nobody reads.
 * <p>
 * Every entry is judged and every rejection collected, the way the named APIs already are, so a
 * document with four bad entries is one round trip rather than four.
 */
@Component
public class WaiverValidator {

  /**
   * The rejections the submitted document earns, in document order - empty where it is acceptable.
   * {@code apiTests} are the report's resolved API tests, so a waiver is matched against the
   * versions the calculation will actually judge rather than the ones the request wrote.
   */
  @RealizesSw({
    SwTraceables.SW_013_CALCULATION_TRIGGER_IS_ALL_OR_NOTHING,
    SwTraceables.SW_040_WAIVER_ENTERS_ONLY_ON_THE_CALCULATION_REQUEST,
  })
  public List<String> validate(List<Waiver> waivers, Set<ApiTest> apiTests) {
    return waivers
      .stream()
      .flatMap(waiver -> rejectionsFor(waiver, apiTests))
      .toList();
  }

  private Stream<String> rejectionsFor(Waiver waiver, Set<ApiTest> apiTests) {
    return Stream.of(
      blankReason(waiver),
      severalTargetDiscriminators(waiver),
      misplacedWildcard(waiver),
      unknownCriterion(waiver),
      unknownApi(waiver, apiTests)
    ).flatMap(Optional::stream);
  }

  /**
   * A reason that is whitespace is a reason nobody can review, which is the one thing the field
   * exists for. An absent one never reaches here - the request shape rejects that.
   */
  private Optional<String> blankReason(Waiver waiver) {
    if (waiver.getReason().isBlank()) {
      return Optional.of(reject(waiver, "carries a blank reason"));
    }

    return Optional.empty();
  }

  /**
   * A criterion's target is addressed by at most one of the three narrow discriminators, so two at
   * once describes a target no criterion enumerates.
   */
  private Optional<String> severalTargetDiscriminators(Waiver waiver) {
    var discriminators = Stream.of(
      waiver.getResponseCode(),
      waiver.getParameterName(),
      waiver.getContentType()
    )
      .filter(discriminator -> nonNull(discriminator))
      .count();

    if (discriminators > 1) {
      return Optional.of(
        reject(
          waiver,
          "names several target discriminators - at most one of responseCode, parameterName and contentType"
        )
      );
    }

    return Optional.empty();
  }

  private Optional<String> misplacedWildcard(Waiver waiver) {
    var wildcarded = Stream.of(
      new NamedValue("serviceName", waiver.getServiceName()),
      new NamedValue("apiName", waiver.getApiName()),
      new NamedValue("path", waiver.getPath()),
      new NamedValue("method", waiver.getMethod()),
      new NamedValue("responseCode", waiver.getResponseCode()),
      new NamedValue("parameterName", waiver.getParameterName()),
      new NamedValue("contentType", waiver.getContentType()),
      new NamedValue("criterion", waiver.getCriterion()),
      new NamedValue("reason", waiver.getReason())
    )
      .filter(namedValue -> API_VERSION_WILDCARD.equals(namedValue.value()))
      .map(NamedValue::name)
      .collect(joining(", "));

    if (wildcarded.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(
      reject(
        waiver,
        "wildcards %s - '%s' is admitted on apiVersion only".formatted(
          wildcarded,
          API_VERSION_WILDCARD
        )
      )
    );
  }

  /**
   * A {@code criterion} this system does not declare names nothing any calculation can reach, and
   * nothing downstream can tell it apart from the one case that is legitimately absent: a criterion
   * the chosen quality gate excluded. The rule failing a calculation on a waiver that matched no
   * finding therefore has to excuse both, so a typo accepted here is an exemption that reads as
   * applied and never was.
   * <p>
   * The gate is not consulted - only the vocabulary. Which criteria run is a property of the gate
   * the pipeline names, and a waiver list written once for a repository is meant to outlive that
   * choice; it is the name that has to exist, not the run.
   */
  private Optional<String> unknownCriterion(Waiver waiver) {
    var criterion = waiver.getCriterion();

    if (
      isNull(criterion) ||
      // owned by misplacedWildcard, which says more about it than this would
      API_VERSION_WILDCARD.equals(criterion) ||
      OpenApiCoverageCriteria.byName(criterion).isPresent()
    ) {
      return Optional.empty();
    }

    return Optional.of(
      reject(waiver, "names an unknown criterion '%s'".formatted(criterion))
    );
  }

  /**
   * A waiver for an API this calculation does not judge is a typo the trigger already has the
   * information to name.
   */
  private Optional<String> unknownApi(Waiver waiver, Set<ApiTest> apiTests) {
    var matched = apiTests
      .stream()
      .anyMatch(apiTest ->
        waiver.appliesTo(
          apiTest.getServiceName(),
          apiTest.getApiName(),
          apiTest.getApiVersion()
        )
      );

    if (matched) {
      return Optional.empty();
    }

    return Optional.of(reject(waiver, "matches no API test of this report"));
  }

  private String reject(Waiver waiver, String complaint) {
    return "Waiver { serviceName='%s', apiName='%s', apiVersion='%s', path='%s', method='%s' } %s!".formatted(
      waiver.getServiceName(),
      waiver.getApiName(),
      waiver.getApiVersion(),
      waiver.getPath(),
      waiver.getMethod(),
      complaint
    );
  }

  private record NamedValue(String name, @Nullable String value) {}
}
