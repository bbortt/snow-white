/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */
package io.github.bbortt.snow.white.toolkit.junit.jupiter.extension;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.WARN;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.VerifiesCon;
import io.opentelemetry.api.OpenTelemetry;
import java.util.Collection;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AutoConfiguredOpenTelemetrySourceUnitTest {

  private static AutoConfiguredOpenTelemetrySource sourceSeeing(
    Collection<String> systemPropertyNames,
    Collection<String> environmentVariableNames
  ) {
    return new AutoConfiguredOpenTelemetrySource(
      () -> systemPropertyNames,
      () -> environmentVariableNames
    );
  }

  @Nested
  class IsOpenTelemetryConfiguredTest {

    @Test
    @VerifiesCon(ConTraceables.CON_013_EXTENSION_NEVER_CHANGES_A_TEST_VERDICT)
    void shouldSeeNoConfigurationInAnEmptyEnvironment() {
      assertThat(
        sourceSeeing(emptySet(), emptySet()).isOpenTelemetryConfigured()
      ).isFalse();
    }

    @ParameterizedTest
    @ValueSource(
      strings = {
        "otel.traces.exporter",
        "otel.exporter.otlp.endpoint",
        "otel.sdk.disabled",
      }
    )
    @VerifiesCon(ConTraceables.CON_013_EXTENSION_NEVER_CHANGES_A_TEST_VERDICT)
    void shouldSeeConfigurationInASystemProperty(String propertyName) {
      assertThat(
        sourceSeeing(
          Set.of(propertyName),
          emptySet()
        ).isOpenTelemetryConfigured()
      ).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
      strings = { "OTEL_TRACES_EXPORTER", "OTEL_EXPORTER_OTLP_ENDPOINT" }
    )
    @VerifiesCon(ConTraceables.CON_013_EXTENSION_NEVER_CHANGES_A_TEST_VERDICT)
    void shouldSeeConfigurationInAnEnvironmentVariable(String variableName) {
      assertThat(
        sourceSeeing(
          emptySet(),
          Set.of(variableName)
        ).isOpenTelemetryConfigured()
      ).isTrue();
    }

    /**
     * The two prefixes are not interchangeable: a lower-case {@code otel.} name is a system
     * property and an upper-case {@code OTEL_} name is an environment variable, and treating either
     * as both would make an unrelated name look like OpenTelemetry configuration.
     */
    @Test
    @VerifiesCon(ConTraceables.CON_013_EXTENSION_NEVER_CHANGES_A_TEST_VERDICT)
    void shouldNotReadAnEnvironmentStylePrefixAsASystemProperty() {
      assertThat(
        sourceSeeing(
          Set.of("OTEL_TRACES_EXPORTER"),
          emptySet()
        ).isOpenTelemetryConfigured()
      ).isFalse();
    }

    @Test
    @VerifiesCon(ConTraceables.CON_013_EXTENSION_NEVER_CHANGES_A_TEST_VERDICT)
    void shouldNotReadASystemPropertyStylePrefixAsAnEnvironmentVariable() {
      assertThat(
        sourceSeeing(
          emptySet(),
          Set.of("otel.traces.exporter")
        ).isOpenTelemetryConfigured()
      ).isFalse();
    }

    @ParameterizedTest
    @ValueSource(
      strings = {
        "hotel.booking",
        "my.otel.thing",
        "SOME_OTEL_FLAG",
        "java.home",
      }
    )
    @VerifiesCon(ConTraceables.CON_013_EXTENSION_NEVER_CHANGES_A_TEST_VERDICT)
    void shouldOnlyMatchAtTheStartOfAName(String unrelatedName) {
      assertThat(
        sourceSeeing(
          Set.of(unrelatedName),
          Set.of(unrelatedName)
        ).isOpenTelemetryConfigured()
      ).isFalse();
    }
  }

  @Nested
  class GetTest {

    /**
     * The green path and the important one: with nothing configured, no SDK is started at all.
     * Calling {@code AutoConfiguredOpenTelemetrySdk.initialize()} here is exactly what failed every
     * application test in five modules, because it defaults every exporter to {@code otlp}.
     */
    @Test
    @VerifiesCon(ConTraceables.CON_013_EXTENSION_NEVER_CHANGES_A_TEST_VERDICT)
    void shouldReturnNoOpWithoutStartingAnSdkWhenNothingIsConfigured() {
      assertThat(sourceSeeing(emptySet(), emptySet()).get()).isSameAs(
        OpenTelemetry.noop()
      );
    }

    @Test
    @VerifiesCon(ConTraceables.CON_013_EXTENSION_NEVER_CHANGES_A_TEST_VERDICT)
    void shouldReportAnUnconfiguredEnvironmentAtDebugRatherThanWarn() {
      try (var log = CapturedLog.of(AutoConfiguredOpenTelemetrySource.class)) {
        sourceSeeing(emptySet(), emptySet()).get();

        assertThat(log.eventsAt(DEBUG)).hasSize(1);
        assertThat(log.eventsAt(WARN)).isEmpty();
        assertThat(
          log.eventsAt(DEBUG).getFirst().getFormattedMessage()
        ).contains("No OpenTelemetry configuration found", "needs no SDK");
      }
    }
  }
}
