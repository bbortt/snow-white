/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event.dto;

public enum FindingStatus {
  COVERED,
  UNCOVERED,
  NOT_APPLICABLE,

  /**
   * The target was removed from the criterion's judgement by a waiver the calculation was triggered
   * with.
   * <p>
   * It sits outside both sides of the coverage fraction, like {@code NOT_APPLICABLE}, and for the
   * same arithmetic reason - but it says something the other statuses cannot: this target could have
   * been judged, and somebody said not to. That is why a waived finding carries the reason and the
   * expiry it was waived under rather than standing on the status alone.
   */
  WAIVED,
}
