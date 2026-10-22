/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event.dto;

import static java.util.Locale.ROOT;
import static java.util.Objects.nonNull;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One target of one criterion declared inapplicable, as the triggering request named it.
 * <p>
 * The API the waiver applies to is not repeated here: the dispatch filters a calculation's waivers
 * per API test, so the record this travels on already names the one API it governs - and names it
 * by its resolved version, which a wildcard entry does not.
 */
@RealizesSw(SwTraceables.SW_040_WAIVER_ENTERS_ONLY_ON_THE_CALCULATION_REQUEST)
public record Waiver(
  String path,
  String method,
  @Nullable String responseCode,
  @Nullable String parameterName,
  @Nullable String contentType,
  @Nullable String criterion,
  String reason,
  LocalDate expires
) {
  /**
   * Whether this waiver's target is the finding's target.
   * <p>
   * Every discriminator is compared, including the ones the waiver omitted: an omitted
   * {@code responseCode} addresses a target that has none - the operation itself - rather than
   * standing for every response code under it. A waiver names one target, so widening an omission
   * into a wildcard would make the narrowest entry in the document the broadest one in effect.
   * <p>
   * Nothing here is derived from the specification. What a target is, is whatever the finding says
   * about itself - path templating, {@code default} response codes and parameter tokens were already
   * resolved by the calculator that emitted it, and a waiver inherits those rules by comparing
   * against their output rather than re-deriving them. This sits beside {@link ApiTestFinding}
   * because every reader of a waiver asks the same question of it, and two readers asking it in two
   * places would be two answers to what a waiver addresses.
   */
  @RealizesSw(SwTraceables.SW_041_WAIVER_RESOLVES_AGAINST_THE_EMITTED_FINDINGS)
  public boolean addresses(@NonNull ApiTestFinding finding) {
    return (
      path.equals(finding.httpPath()) &&
      sameMethod(finding.httpMethod()) &&
      Objects.equals(responseCode, finding.responseCode()) &&
      Objects.equals(parameterName, finding.parameterName()) &&
      Objects.equals(contentType, finding.contentType())
    );
  }

  /**
   * This waiver's target as a message names it back to its author: {@code GET /pets/{petId} (404)}.
   */
  public String target() {
    return "%s %s%s".formatted(method.toUpperCase(ROOT), path, discriminator());
  }

  /**
   * Findings carry the method as the operation key spells it, which is upper case; a waiver carries
   * it as its author typed it, which is as often the lower case their specification uses. The one
   * normalisation here is on a scalar - it is not a matching rule, and it is the only place this
   * record deviates from comparing a finding's addressing verbatim.
   */
  private boolean sameMethod(@Nullable String emitted) {
    return nonNull(emitted) && method.toUpperCase(ROOT).equals(emitted);
  }

  /**
   * The one discriminator a waiver carries beyond its operation, parenthesised, or empty where it
   * names the operation itself. Trigger-time validation already rejected an entry carrying several,
   * so the first one present is the only one present.
   */
  private String discriminator() {
    return Stream.of(responseCode, parameterName, contentType)
      .filter(Objects::nonNull)
      .findFirst()
      .map(" (%s)"::formatted)
      .orElse("");
  }
}
