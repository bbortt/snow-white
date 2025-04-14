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

@Target(TYPE)
@Retention(RUNTIME)
@SpringBootTest(
  classes = { Main.class },
  properties = {
    "io.github.bbortt.snow.white.microservices.api.sync.job.service-interface.base-url=${wiremock.server.baseUrl}",
    "io.github.bbortt.snow.white.microservices.api.sync.job.service-interface.index-uri=/sir/index",
  }
)
@ActiveProfiles("test")
public @interface IntegrationTest {
}
