/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.health;

import static org.apache.kafka.streams.KafkaStreams.State.RUNNING;
import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import clew.traceables.clew.annotation.VerifiesSw;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;

/**
 * Proves the health verdict of a running topology is reachable from outside the process: served
 * on the management port and carried by both probe groups.
 * <p>
 * The verdict for each individual {@code KafkaStreams.State} is covered exhaustively by
 * {@code KafkaStreamsHealthIndicatorUnitTest}; this test is about the wiring. The down verdict
 * needs a context it is allowed to ruin, so it lives in
 * {@code KafkaStreamsHealthIndicatorClosedStreamIT}.
 */
class KafkaStreamsHealthIndicatorIT extends AbstractKafkaStreamsHealthIT {

  @Test
  @VerifiesSw(SwTraceables.SW_045_STREAM_STATE_DECIDES_THE_HEALTH_VERDICT)
  @VerifiesArch(
    ArchTraceables.ARCH_018_MANAGEMENT_SURFACE_WITHOUT_A_BUSINESS_ONE
  )
  void shouldReportUpAndNameTheState_onceTheTopologyIsRunning() {
    awaitStreamRunning();

    var health = readJson(managementBaseUrl(), HEALTH_PATH);

    assertThat(health.path("status").asString()).isEqualTo("UP");
    assertThat(
      health.path("components").path(INDICATOR_NAME).path("status").asString()
    ).isEqualTo("UP");
    assertThat(
      health
        .path("components")
        .path(INDICATOR_NAME)
        .path("details")
        .path("state")
        .asString()
    ).isEqualTo(RUNNING.name());
  }

  @Test
  @VerifiesSw(SwTraceables.SW_045_STREAM_STATE_DECIDES_THE_HEALTH_VERDICT)
  void shouldReportUpOnBothProbeGroups_onceTheTopologyIsRunning() {
    awaitStreamRunning();

    assertThat(
      readJson(managementBaseUrl(), LIVENESS_PATH).path("status").asString()
    ).isEqualTo("UP");
    assertThat(
      readJson(managementBaseUrl(), READINESS_PATH).path("status").asString()
    ).isEqualTo("UP");
  }

  @Test
  @VerifiesArch(
    ArchTraceables.ARCH_018_MANAGEMENT_SURFACE_WITHOUT_A_BUSINESS_ONE
  )
  void shouldServeNothingOnTheApplicationPort() {
    assertThat(statusOf(applicationBaseUrl(), HEALTH_PATH)).isEqualTo(
      HttpStatusCode.valueOf(404)
    );
    assertThat(statusOf(applicationBaseUrl(), "/")).isEqualTo(
      HttpStatusCode.valueOf(404)
    );
  }
}
