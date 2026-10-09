/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.health;

import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;

/**
 * Proves a stopped topology turns the verdict down on both probe groups, read over HTTP rather
 * than from a startup snapshot.
 * <p>
 * Closing the stream cannot be undone, so this verdict owns a context of its own rather than
 * ordering itself after the up-verdict tests in {@code KafkaStreamsHealthIndicatorIT}.
 */
class KafkaStreamsHealthIndicatorClosedStreamIT
  extends AbstractKafkaStreamsHealthIT
{

  @Test
  @VerifiesSw(SwTraceables.SW_045_STREAM_STATE_DECIDES_THE_HEALTH_VERDICT)
  void shouldReportDownOnBothProbeGroups_afterTheStreamIsClosed() {
    awaitStreamRunning();

    closeStream();

    assertThat(statusOf(managementBaseUrl(), HEALTH_PATH)).isEqualTo(
      HttpStatusCode.valueOf(503)
    );
    assertThat(statusOf(managementBaseUrl(), LIVENESS_PATH)).isEqualTo(
      HttpStatusCode.valueOf(503)
    );
    assertThat(statusOf(managementBaseUrl(), READINESS_PATH)).isEqualTo(
      HttpStatusCode.valueOf(503)
    );
  }
}
