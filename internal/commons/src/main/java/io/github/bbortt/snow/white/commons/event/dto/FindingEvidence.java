/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event.dto;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record FindingEvidence(
  @NonNull String traceId,
  @Nullable String testCaseName
) {
  /**
   * The largest test identity Snow-White stores, in UTF-8 bytes.
   * <p>
   * The upstream convention bounds the value not at all, so something here has to, and the
   * consequence of not bounding it is severe: {@code finding_evidence.test_case_name} is
   * {@code VARCHAR(1024)} and part of a unique constraint, so an over-long name fails the insert,
   * the listener exhausts its retries, and a whole report is discarded over one pathological test
   * name.
   * <p>
   * The bound is in bytes rather than characters because it has to satisfy two limits at once: the
   * column's 1024 <em>characters</em>, and the 2704-byte row limit of the constraint's btree, which
   * 1024 characters would exceed at CJK's 3 bytes each, let alone an emoji's 4. Since no character
   * encodes to less than one byte, 1024 bytes is inside both. That makes it deliberately strict for
   * multi-byte names - a 600-character CJK name the column would hold is still dropped - which is
   * the trade for one rule that cannot fail an insert.
   * <p>
   * Truncating the value is forbidden, so a name past this bound is no identity at all rather than
   * a shortened one: the evidence keeps its trace id and the report survives.
   * <p>
   * It lives here, on the event contract both sides speak, because the service that applies it
   * (openapi-coverage-stream, on the way into the evidence) and the service that has to hold what
   * it admits (report-coordinator-api, in its column and its published schema) are deployed
   * separately. A test on each side asserts its own storage still admits this many bytes.
   */
  public static final int MAX_TEST_CASE_NAME_BYTES = 1024;
}
