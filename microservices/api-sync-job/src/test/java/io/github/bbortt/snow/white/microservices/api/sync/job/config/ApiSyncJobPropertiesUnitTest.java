/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode.GRACEFUL;
import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.NfTraceables;
import clew.traceables.clew.annotation.VerifiesNf;
import org.junit.jupiter.api.Test;

class ApiSyncJobPropertiesUnitTest {

  private final ApiSyncJobProperties fixture = new ApiSyncJobProperties();

  @Test
  void shouldDefaultToABoundedFanOut() {
    assertThat(fixture).satisfies(
      properties ->
        assertThat(properties.getMaxParallelSyncTasks()).isEqualTo(3),
      properties -> assertThat(properties.getWorkQueueCapacity()).isEqualTo(30)
    );
  }

  @Test
  void shouldDefaultToGracefulParsing() {
    assertThat(fixture.getArtifactory().getParsingMode()).isEqualTo(GRACEFUL);
  }

  @Test
  @VerifiesNf(NfTraceables.NF_010_REFERENCE_RESOLUTION_IS_OFF_BY_DEFAULT)
  void shouldDefaultToNotResolvingReferences() {
    assertThat(fixture.getArtifactory().getResolveReferences()).isFalse();
  }
}
