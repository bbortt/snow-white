/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event.dto;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/**
 * One target of one criterion declared inapplicable, as the triggering request named it.
 * <p>
 * The API the waiver applies to is not repeated here: the dispatch filters a calculation's waivers
 * per API test, so the record this travels on already names the one API it governs - and names it
 * by its resolved version, which a wildcard entry does not.
 */
public record Waiver(
  String path,
  String method,
  @Nullable String responseCode,
  @Nullable String parameterName,
  @Nullable String contentType,
  @Nullable String criterion,
  String reason,
  LocalDate expires
) {}
