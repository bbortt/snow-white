/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;

public interface CachingService {
  boolean apiInformationIndexed(ApiInformation apiInformation);

  /**
   * @return whether the index holds the API information afterwards. A deferred
   *   publish - one the index could not accept before the retries ran out -
   *   answers {@code false} rather than raising, so the caller can count it
   *   truthfully without having to fail the cycle over it.
   */
  boolean publishApiInformation(ApiInformation apiInformation);
}
