/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.junit;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.JUnitReportResource.FILENAME;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JUnitReportResourceTest {

  @Nested
  class GetFilename {

    @Test
    void returnsFilename() {
      var fixture = new JUnitReportResource(new byte[0]);

      assertThat(fixture.getFilename()).isEqualTo(FILENAME);
    }
  }
}
