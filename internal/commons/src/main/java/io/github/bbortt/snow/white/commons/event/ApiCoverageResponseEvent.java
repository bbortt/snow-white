/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event;

import io.github.bbortt.snow.white.commons.quality.gate.ApiType;

public interface ApiCoverageResponseEvent {
  ApiType getApiType();
}
