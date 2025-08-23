/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.api.redis;

import io.github.bbortt.snow.white.commons.redis.ApiEndpointEntry;
import org.springframework.data.repository.CrudRepository;

public interface ApiEndpointRepository
  extends CrudRepository<ApiEndpointEntry, String> {}
