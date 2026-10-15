/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static java.util.Locale.ROOT;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static lombok.AccessLevel.PRIVATE;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding;
import io.github.bbortt.snow.white.commons.event.dto.Waiver;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.WaiverContext;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Selects the findings a calculation's waivers address, out of the findings one criterion already
 * emitted.
 * <p>
 * Everything this class knows about a target is what the finding says about itself. It does not
 * read the OpenAPI document, re-derive a spec pointer, or implement path templating, {@code default}
 * response-code or parameter-token rules of its own - those live in the calculators, once, and a
 * waiver inherits them by selecting from what they produced.
 */
@NoArgsConstructor(access = PRIVATE)
@RealizesSw(SwTraceables.SW_041_WAIVER_RESOLVES_AGAINST_THE_EMITTED_FINDINGS)
final class WaiverResolver {

  /**
   * The findings as the waivers leave them: every finding a waiver in force addresses rewritten to
   * {@code WAIVED} with that waiver's justification, every other finding returned untouched.
   * <p>
   * A finding's own status is not consulted. A {@code COVERED} target is waived like any other,
   * because a waiver that only applied where the criterion was failing would be applied or skipped
   * according to which traces happened to land in the lookback window - the same request answering
   * differently, which is what {@code CON-001} exists to exclude. It also cannot flatter a ratio:
   * a covered target leaving the fraction can only lower it.
   */
  @RealizesSw(SwTraceables.SW_041_WAIVER_RESOLVES_AGAINST_THE_EMITTED_FINDINGS)
  static List<ApiTestFinding> applyWaivers(
    @NonNull OpenApiCoverageCriteria criterion,
    @NonNull List<ApiTestFinding> findings,
    @NonNull WaiverContext waiverContext
  ) {
    var inForce = waiverContext.inForce();

    if (inForce.isEmpty()) {
      return findings;
    }

    return findings
      .stream()
      .map(finding -> waive(criterion, finding, inForce))
      .toList();
  }

  /**
   * What the result says about the waivers that had already expired when the calculation was
   * triggered, or {@code null} where none of them addressed this criterion.
   * <p>
   * An expired waiver is reported rather than dropped: the point of a date is to force the question
   * back into view, and a criterion that simply starts failing with no mention of the waiver sends
   * its team debugging telemetry instead of reading their waiver file.
   */
  @RealizesSw(SwTraceables.SW_043_EXPIRY_JUDGED_AGAINST_THE_TRIGGERING_INSTANT)
  static @Nullable String describeExpiredWaivers(
    @NonNull OpenApiCoverageCriteria criterion,
    @NonNull List<ApiTestFinding> findings,
    @NonNull WaiverContext waiverContext
  ) {
    var description = waiverContext
      .expired()
      .stream()
      .filter(waiver -> addresses(criterion, findings, waiver))
      .map(WaiverResolver::describe)
      .collect(joining(" "));

    return description.isEmpty() ? null : description;
  }

  /**
   * This finding as the first waiver addressing it leaves it.
   * <p>
   * First rather than last, and one rather than all: two entries addressing one target are a
   * duplicate in the request, not a composition, and the earlier one is the one the author reading
   * their own document would expect to see quoted back.
   */
  private static ApiTestFinding waive(
    OpenApiCoverageCriteria criterion,
    ApiTestFinding finding,
    List<Waiver> waivers
  ) {
    return waivers
      .stream()
      .filter(waiver -> addresses(criterion, finding, waiver))
      .findFirst()
      .map(waiver -> finding.waivedBecause(waiver.reason(), waiver.expires()))
      .orElse(finding);
  }

  private static boolean addresses(
    OpenApiCoverageCriteria criterion,
    List<ApiTestFinding> findings,
    Waiver waiver
  ) {
    return findings
      .stream()
      .anyMatch(finding -> addresses(criterion, finding, waiver));
  }

  private static boolean addresses(
    OpenApiCoverageCriteria criterion,
    ApiTestFinding finding,
    Waiver waiver
  ) {
    return (
      coversCriterion(criterion, waiver) && addressesSameTarget(finding, waiver)
    );
  }

  /**
   * Whether the waiver's target is this finding's target.
   * <p>
   * Every discriminator is compared, including the ones the waiver omitted: an omitted
   * {@code responseCode} addresses a target that has none - the operation itself - rather than
   * standing for every response code under it. A waiver names one target, so widening an omission
   * into a wildcard would make the narrowest entry in the document the broadest one in effect.
   */
  private static boolean addressesSameTarget(
    ApiTestFinding finding,
    Waiver waiver
  ) {
    return (
      waiver.path().equals(finding.httpPath()) &&
      sameMethod(waiver.method(), finding.httpMethod()) &&
      Objects.equals(waiver.responseCode(), finding.responseCode()) &&
      Objects.equals(waiver.parameterName(), finding.parameterName()) &&
      Objects.equals(waiver.contentType(), finding.contentType())
    );
  }

  /**
   * Findings carry the method as the operation key spells it, which is upper case; a waiver carries
   * it as its author typed it, which is as often the lower case their specification uses. The one
   * normalisation here is on a scalar - it is not a matching rule, and it is the only place this
   * class deviates from comparing a finding's addressing verbatim.
   */
  private static boolean sameMethod(String waived, @Nullable String emitted) {
    return nonNull(emitted) && waived.toUpperCase(ROOT).equals(emitted);
  }

  /**
   * Whether the waiver reaches this criterion: an omitted {@code criterion} waives the target for
   * every criterion that judged it, and a named one waives it for that criterion and for every
   * criterion containing it.
   * <p>
   * Upward only. Conceding that an operation cannot be made to succeed concedes the weaker
   * statement that it cannot be called, but not the reverse - a narrower criterion is a stricter
   * claim, and a waiver never widens itself into one.
   */
  @RealizesSw(
    SwTraceables.SW_042_CRITERION_NARROWING_REACHES_THE_CONTAINING_CRITERIA
  )
  private static boolean coversCriterion(
    OpenApiCoverageCriteria criterion,
    Waiver waiver
  ) {
    var named = waiver.criterion();

    if (isNull(named)) {
      return true;
    }

    return toCriterion(named)
      .map(
        namedCriterion ->
          namedCriterion.equals(criterion) ||
          namedCriterion.getContainingCriteria().contains(criterion)
      )
      .orElse(false);
  }

  /**
   * A name no criterion of this system carries addresses nothing here, rather than failing the
   * calculation from inside a calculator: the request that carried it is where a typo can still be
   * answered usefully, and {@code CON-011} is what answers an entry that addressed nothing
   * anywhere.
   */
  private static Optional<OpenApiCoverageCriteria> toCriterion(String name) {
    try {
      return Optional.of(
        OpenApiCoverageCriteria.valueOf(name.toUpperCase(ROOT))
      );
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  private static String describe(Waiver waiver) {
    return "Waiver on '%s %s'%s expired on %s and was not applied: %s".formatted(
      waiver.method().toUpperCase(ROOT),
      waiver.path(),
      discriminator(waiver),
      waiver.expires(),
      waiver.reason()
    );
  }

  private static String discriminator(Waiver waiver) {
    return Stream.of(
      waiver.responseCode(),
      waiver.parameterName(),
      waiver.contentType()
    )
      .filter(Objects::nonNull)
      .findFirst()
      .map(" (%s)"::formatted)
      .orElse("");
  }
}
