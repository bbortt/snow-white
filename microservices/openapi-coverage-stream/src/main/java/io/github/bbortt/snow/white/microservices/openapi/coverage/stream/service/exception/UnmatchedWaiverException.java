/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.joining;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.RealizesCon;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.Waiver;
import java.util.List;

/**
 * A calculation stopped because a waiver dispatched with it addressed no target any criterion
 * judged.
 * <p>
 * Checked, and declared all the way out to the stream processor, because the alternative is a
 * runtime exception caught by a general handler - and then the one guarantee this failure exists to
 * give, that no ratio is published beside an input that was ignored, would hold by accident of
 * which catch block happened to be widest.
 */
@RealizesCon(
  ConTraceables.CON_011_WAIVER_MATCHING_NO_FINDING_FAILS_THE_CALCULATION
)
public class UnmatchedWaiverException extends Exception {

  public UnmatchedWaiverException(
    ApiInformation apiInformation,
    List<Waiver> unmatchedWaivers
  ) {
    super(
      "%d waiver(s) matched no finding of any criterion of { \"serviceName\": \"%s\", \"apiName\": \"%s\", \"apiVersion\": \"%s\" }: %s. A waiver that matches nothing is either misspelled or names a target the specification no longer declares; no coverage is reported for this API test while one of them is unplaced.".formatted(
        unmatchedWaivers.size(),
        apiInformation.getServiceName(),
        apiInformation.getApiName(),
        apiInformation.getApiVersion(),
        describe(unmatchedWaivers)
      )
    );
  }

  private static String describe(List<Waiver> unmatchedWaivers) {
    return unmatchedWaivers
      .stream()
      .map(UnmatchedWaiverException::describe)
      .collect(joining(", "));
  }

  /**
   * The criterion is named only where the waiver named one. Printing {@code null} for the common
   * entry would read as a criterion called null, and the omission is itself the useful fact: it
   * says the entry was meant to reach every criterion judging the target.
   */
  private static String describe(Waiver waiver) {
    return isNull(waiver.criterion())
      ? "'%s'".formatted(waiver.target())
      : "'%s' naming criterion '%s'".formatted(
          waiver.target(),
          waiver.criterion()
        );
  }
}
