/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.web;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

/**
 * The response header every Quality-Gate read publishes gate ownership on, and its values. Here
 * rather than inside the service that writes it, because it is wire contract a client reads off the
 * read it has already made — same reason {@code X-Total-Count}'s handling lives beside it.
 */
@NoArgsConstructor(access = PRIVATE)
public final class QualityGateOwnership {

  public static final String HEADER_X_QUALITY_GATE_OWNERSHIP =
    "X-Quality-Gate-Ownership";

  /**
   * Gates are owned through the Quality-Gate API — the behaviour of an instance deployed without a
   * gate file.
   */
  public static final String OWNERSHIP_DATABASE = "database";

  /** Gates come from the instance's gate file, so every write on a gate path is refused. */
  public static final String OWNERSHIP_FILE = "file";

  /**
   * Gates come from the instance's gate file and it defines none. A third value of this header
   * rather than a second header, because the three states are one fact about the instance and a
   * client reads them with one lookup; and it is published rather than left to be inferred from an
   * empty page, because "no gates here" and "gates, none of them yours to write" are what a screen
   * has to tell apart. Why a {@code 404} for a gate name could not have matched is stated in that
   * response's own body by the service that owns the gates, not reassembled from this header.
   */
  public static final String OWNERSHIP_FILE_EMPTY = "file-empty";
}
