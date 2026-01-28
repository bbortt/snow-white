/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.EnableWireMock;

@Target(TYPE)
@Retention(RUNTIME)
@EnableWireMock
@SpringBootTest(
  classes = { Main.class },
  properties = {
    "snow.white.api.sync.job.api-index.base-url=http://localhost:8085",
    "snow.white.api.sync.job.artifactory.base-url=http://localhost:3000",
    "snow.white.api.sync.job.artifactory.access-token=private-token",
    "snow.white.api.sync.job.artifactory.repository=snow-white-generic-local",
  }
)
@ActiveProfiles("test")
public @interface IntegrationTest {}
