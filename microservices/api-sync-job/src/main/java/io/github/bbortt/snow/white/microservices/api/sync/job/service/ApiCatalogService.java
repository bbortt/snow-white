/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import java.util.List;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public interface ApiCatalogService {
  List<Supplier<@Nullable ApiInformation>> getApiSpecificationLoaders();
}
