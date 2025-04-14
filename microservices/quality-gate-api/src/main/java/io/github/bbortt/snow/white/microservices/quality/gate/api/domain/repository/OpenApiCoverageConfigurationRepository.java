/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository;

import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.OpenApiCoverageConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OpenApiCoverageConfigurationRepository
  extends JpaRepository<OpenApiCoverageConfiguration, String> {}
