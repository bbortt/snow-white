/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
  classes = { Main.class },
  properties = {
    "io.github.bbortt.snow.white.microservices.report.coordination.service.calculation-request-topic=snow-white-coverage-request",
    "io.github.bbortt.snow.white.microservices.report.coordination.service.openapi-calculation-response.topic=snow-white-openapi-calculation-response",
    "io.github.bbortt.snow.white.microservices.report.coordination.service.public-api-gateway-url=http://localhost:9080",
    "io.github.bbortt.snow.white.microservices.report.coordination.service.quality-gate-api-url=http://localhost:8081",
  }
)
public @interface IntegrationTest {
}
