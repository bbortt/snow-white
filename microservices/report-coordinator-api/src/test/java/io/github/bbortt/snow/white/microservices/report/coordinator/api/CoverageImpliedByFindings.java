/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.FindingStatus.NOT_APPLICABLE;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.FindingStatus.findingStatus;
import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.HALF_UP;
import static lombok.AccessLevel.PRIVATE;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.NoArgsConstructor;

/**
 * The ratio a set of persisted findings implies, spelled out independently of the service that
 * derived it: a test asserting the two agree has to recompute the number rather than read it back
 * from the same code path that wrote it.
 */
@NoArgsConstructor(access = PRIVATE)
public final class CoverageImpliedByFindings {

  private static final BigDecimal LARGEST_VALUE_BELOW_ONE = new BigDecimal(
    "0.99"
  );

  /**
   * @param findings rows of {@code api_test_finding}, each carrying at least its {@code status}
   *                 column.
   */
  public static BigDecimal ratioImpliedBy(List<Map<String, Object>> findings) {
    var judged = findings
      .stream()
      .map(finding -> findingStatus((Short) finding.get("status")))
      .filter(status -> !NOT_APPLICABLE.equals(status))
      .toList();

    if (judged.isEmpty()) {
      return ONE.setScale(2, HALF_UP);
    }

    var covered = judged.stream().filter(COVERED::equals).count();
    var ratio = BigDecimal.valueOf(covered).divide(
      BigDecimal.valueOf(judged.size()),
      2,
      HALF_UP
    );

    return covered != judged.size() && ratio.compareTo(ONE) == 0
      ? LARGEST_VALUE_BELOW_ONE
      : ratio;
  }
}
