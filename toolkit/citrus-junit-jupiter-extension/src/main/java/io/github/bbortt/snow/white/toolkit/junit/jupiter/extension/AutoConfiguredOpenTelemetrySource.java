/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */
package io.github.bbortt.snow.white.toolkit.junit.jupiter.extension;

import static org.slf4j.LoggerFactory.getLogger;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.RealizesCon;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;

/**
 * Decides which {@link OpenTelemetry} the extension records test spans against, without ever
 * starting an SDK a suite did not ask for.
 * <p>
 * Three outcomes, in order:
 * <ol>
 *   <li>An instance already registered globally - something else configured OpenTelemetry, so use
 *       it.</li>
 *   <li>Nothing configured at all - return no-op. This is the ordinary state of most tests in this
 *       repository, and it costs nothing: the test identity travels as baggage, which is part of
 *       {@code opentelemetry-api} and needs no SDK.</li>
 *   <li>Something configured - autoconfigure an SDK from it.</li>
 * </ol>
 * Checking for configuration before autoconfiguring is what keeps the second case free. Calling
 * {@code AutoConfiguredOpenTelemetrySdk.initialize()} unconditionally would default every exporter
 * to {@code otlp} and throw on a classpath with no OTLP exporter - which is exactly how the first
 * cut of this extension failed every application test in five modules.
 * <p>
 * This supplier may throw. Never changing a test's verdict is {@link TestTracer}'s job, which is
 * where every failure here is absorbed.
 */
@NullMarked
final class AutoConfiguredOpenTelemetrySource
  implements Supplier<OpenTelemetry>
{

  private static final Logger logger = getLogger(
    AutoConfiguredOpenTelemetrySource.class
  );

  private static final String SYSTEM_PROPERTY_PREFIX = "otel.";
  private static final String ENVIRONMENT_VARIABLE_PREFIX = "OTEL_";

  private final Supplier<Collection<String>> systemPropertyNames;
  private final Supplier<Collection<String>> environmentVariableNames;

  AutoConfiguredOpenTelemetrySource() {
    this(
      () -> Set.copyOf(System.getProperties().stringPropertyNames()),
      () -> Set.copyOf(System.getenv().keySet())
    );
  }

  AutoConfiguredOpenTelemetrySource(
    Supplier<Collection<String>> systemPropertyNames,
    Supplier<Collection<String>> environmentVariableNames
  ) {
    this.systemPropertyNames = systemPropertyNames;
    this.environmentVariableNames = environmentVariableNames;
  }

  @Override
  @RealizesCon(ConTraceables.CON_013_EXTENSION_NEVER_CHANGES_A_TEST_VERDICT)
  public OpenTelemetry get() {
    if (GlobalOpenTelemetry.isSet()) {
      logger.debug(
        "Recording test spans against the globally registered OpenTelemetry instance."
      );
      return GlobalOpenTelemetry.get();
    }

    if (!isOpenTelemetryConfigured()) {
      logger.debug(
        "No OpenTelemetry configuration found ('{}*' system property or '{}*' environment " +
          "variable); test spans will not be recorded. Test identity is unaffected - it travels as " +
          "baggage and needs no SDK.",
        SYSTEM_PROPERTY_PREFIX,
        ENVIRONMENT_VARIABLE_PREFIX
      );
      return OpenTelemetry.noop();
    }

    return Sdk.autoConfigure();
  }

  boolean isOpenTelemetryConfigured() {
    return (
      containsPrefixed(systemPropertyNames.get(), SYSTEM_PROPERTY_PREFIX) ||
      containsPrefixed(
        environmentVariableNames.get(),
        ENVIRONMENT_VARIABLE_PREFIX
      )
    );
  }

  private static boolean containsPrefixed(
    Collection<String> names,
    String prefix
  ) {
    return names.stream().anyMatch(name -> name.startsWith(prefix));
  }

  /**
   * Holds the only reference to the autoconfigure SDK, so the class is resolved when a configured
   * suite first needs it and never on a classpath that does not carry it -
   * {@code opentelemetry-sdk-extension-autoconfigure} is an optional dependency of this artifact.
   */
  private static final class Sdk {

    private Sdk() {
      // utility class
    }

    static OpenTelemetry autoConfigure() {
      return AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();
    }
  }
}
