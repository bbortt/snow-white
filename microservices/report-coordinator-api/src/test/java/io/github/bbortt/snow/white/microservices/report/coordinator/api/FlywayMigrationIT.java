/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FlywayMigrationIT extends AbstractReportCoordinationServiceIT {

  @Autowired
  private Flyway flyway;

  @Test
  void shouldHaveAppliedFlywayMigrations() {
    assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
  }
}
