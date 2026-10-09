/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.spring.web.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.Span;
import org.junit.jupiter.api.Test;

class SpanProviderUnitTest {

  private final SpanProvider fixture = new SpanProvider();

  @Test
  void getCurrentSpanDelegatesToOpenTelemetryApi() {
    assertThat(fixture.getCurrentSpan()).isEqualTo(Span.getInvalid());
  }
}
