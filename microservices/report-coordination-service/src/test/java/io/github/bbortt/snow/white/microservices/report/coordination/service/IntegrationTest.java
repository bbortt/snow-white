/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service;

import static io.github.bbortt.snow.white.microservices.report.coordination.service.IntegrationTest.CALCULATION_REQUEST_TOPIC;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.IntegrationTest.OPENAPI_CALCULATION_RESPONSE_TOPIC;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
  classes = { Main.class },
  properties = {
    "io.github.bbortt.snow.white.microservices.report.coordination.service.calculation-request-topic=" +
    CALCULATION_REQUEST_TOPIC,
    "io.github.bbortt.snow.white.microservices.report.coordination.service.init-topics=true",
    "io.github.bbortt.snow.white.microservices.report.coordination.service.openapi-calculation-response.topic=" +
    OPENAPI_CALCULATION_RESPONSE_TOPIC,
    "io.github.bbortt.snow.white.microservices.report.coordination.service.public-api-gateway-url=http://localhost:9080",
    "io.github.bbortt.snow.white.microservices.report.coordination.service.quality-gate-api-url=${wiremock.server.baseUrl:http://localhost:8081}",
  }
)
@ActiveProfiles("test")
public @interface IntegrationTest {
  String CALCULATION_REQUEST_TOPIC = "snow-white-coverage-request";

  String OPENAPI_CALCULATION_RESPONSE_TOPIC =
    "snow-white-openapi-calculation-response";
}
