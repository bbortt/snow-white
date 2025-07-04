/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.domain.model;

public enum ApiLoadStatus {
  // Not yet loaded
  UNLOADED,
  // Successfully loaded
  LOADED,
  // Load failed
  LOAD_FAILED,
  // No source url provided
  NO_SOURCE,
}
