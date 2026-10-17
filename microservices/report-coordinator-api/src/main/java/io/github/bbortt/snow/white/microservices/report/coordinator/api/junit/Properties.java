/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.junit;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class Properties {

  static final String CALCULATION_ID = "calculationId";
  static final String MIN_COVERAGE_PERCENTAGE = "minCoveragePercentage";

  static final String SERVICE_NAME = "serviceName";
  static final String API_NAME = "apiName";
  static final String API_VERSION = "apiVersion";

  static final String DESCRIPTION = "description";

  /**
   * The two numbers the JSON reads publish beside a coverage ratio, spelled here exactly as they
   * are spelled there: a reader comparing the document against the report it came from should not
   * have to translate a name.
   * On the {@code testsuites} root, {@code waivedTargetCount} is the document's total.
   */
  static final String JUDGED_TARGET_COUNT = "judgedTargetCount";
  static final String WAIVED_TARGET_COUNT = "waivedTargetCount";

  /**
   * The waived targets themselves, for a reader that parses rather than reads - the same targets
   * the case's {@code system-out} names in prose, where the reasons and expiries are.
   */
  static final String WAIVED_TARGETS = "waivedTargets";
}
