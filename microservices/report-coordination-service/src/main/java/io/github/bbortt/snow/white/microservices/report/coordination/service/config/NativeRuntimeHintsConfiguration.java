/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.config;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.client.qualitygateapi.dto.Error;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.client.qualitygateapi.dto.OpenApiCriterion;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.client.qualitygateapi.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.report.coordination.service.junit.Failure;
import io.github.bbortt.snow.white.microservices.report.coordination.service.junit.Property;
import io.github.bbortt.snow.white.microservices.report.coordination.service.junit.Skipped;
import io.github.bbortt.snow.white.microservices.report.coordination.service.junit.TestCase;
import io.github.bbortt.snow.white.microservices.report.coordination.service.junit.TestSuite;
import io.github.bbortt.snow.white.microservices.report.coordination.service.junit.TestSuites;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RegisterReflectionForBinding(
  {
    // For Quality-Gate API Requests
    Error.class,
    OpenApiCriterion.class,
    QualityGateConfig.class,
    // For Coverage Requests
    QualityGateCalculationRequestEvent.class,
    // For Coverage Responses
    OpenApiCoverageResponseEvent.class,
    OpenApiTestResult.class,
    // For JUnit Report Generation
    Failure.class,
    Property.class,
    Skipped.class,
    TestCase.class,
    TestSuite.class,
    TestSuites.class,
  }
)
public class NativeRuntimeHintsConfiguration {}
