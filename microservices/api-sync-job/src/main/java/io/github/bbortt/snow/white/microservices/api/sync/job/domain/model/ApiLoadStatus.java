/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.domain.model;

public enum ApiLoadStatus {
  // Not yet loaded
  UNLOADED,
  // Could not be read from the source
  DOWNLOAD_FAILED,
  // Read, but not parseable as an OpenAPI document
  PARSE_FAILED,
  // Successfully loaded
  LOADED,
  // Load failed
  LOAD_FAILED,
  // One of service name, API name or version is missing
  MANDATORY_INFORMATION_MISSING,
  // No source url provided
  NO_SOURCE,
  // The index could not accept it - deferred to the next cycle
  PUBLISH_DEFERRED,
  // Published to API index service
  PUBLISHED,
}
