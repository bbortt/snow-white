/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.config;

import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.Error;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.OpenApiCriterion;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.QualityGateConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RegisterReflectionForBinding(
  {
    // For Quality-Gate API Requests
    Error.class, OpenApiCriterion.class, QualityGateConfig.class,
  }
)
public class NativeRuntimeHintsConfiguration {}
