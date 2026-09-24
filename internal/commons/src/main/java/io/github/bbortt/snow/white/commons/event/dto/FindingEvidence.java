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
) {}
