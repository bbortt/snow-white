/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */
package io.github.bbortt.snow.white.toolkit.junit.jupiter.extension;

import static java.util.Objects.isNull;
import static org.slf4j.LoggerFactory.getLogger;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.RealizesCon;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Supplies the {@link Tracer} test spans are recorded against, at most once per instance and
 * without ever failing.
 * <p>
 * The extension is auto-registered for every test in a module, including the many that have no
 * OpenTelemetry anywhere and want none. So acquisition is:
 * <ul>
 *   <li><strong>lazy</strong> - a suite with nothing configured never pays SDK start-up;</li>
 *   <li><strong>at most once</strong> - the result is memoized, a no-op result included, so a
 *       failure is never retried per test;</li>
 *   <li><strong>total</strong> - any failure becomes a no-op tracer and a single {@code WARN},
 *       never a test outcome.</li>
 * </ul>
 * The {@code WARN} is deliberate and so is its level: reaching this class's catch means something
 * <em>was</em> configured and did not work, which is what somebody wiring up a suite needs to see.
 * The "nothing configured" case is reported at {@code DEBUG} by
 * {@link AutoConfiguredOpenTelemetrySource}, because it is the normal state of most tests here and
 * would be pure noise.
 */
@NullMarked
final class TestTracer {

  private static final Logger logger = getLogger(TestTracer.class);

  /**
   * The instrumentation scope reported on every test span. The version is read from this
   * artifact's own jar manifest, and is {@code null} when the classes are loaded from a build
   * directory rather than a jar - which the OpenTelemetry API accepts.
   */
  private static final String INSTRUMENTATION_SCOPE_NAME =
    TestTracer.class.getPackageName();

  private final Supplier<OpenTelemetry> openTelemetry;

  private @Nullable Tracer tracer;

  TestTracer() {
    this(new AutoConfiguredOpenTelemetrySource());
  }

  TestTracer(Supplier<OpenTelemetry> openTelemetry) {
    this.openTelemetry = openTelemetry;
  }

  /**
   * The tracer, acquiring it on first use.
   * <p>
   * Synchronized rather than merely memoized: the one-attempt guarantee has to be a property of
   * this code and not of tests happening to run sequentially, because Surefire is configured for
   * parallel execution in this repository.
   */
  synchronized Tracer get() {
    if (isNull(tracer)) {
      tracer = resolve().getTracer(
        INSTRUMENTATION_SCOPE_NAME,
        TestTracer.class.getPackage().getImplementationVersion()
      );
    }

    return tracer;
  }

  /**
   * {@code Exception} and {@code LinkageError} are both expected here - a misconfiguration throws
   * {@code ConfigurationException}, and an absent optional dependency throws
   * {@code NoClassDefFoundError}. Neither is allowed out. An {@code Error} that is not a
   * {@code LinkageError} - {@code OutOfMemoryError}, say - is not this class's business and
   * propagates.
   */
  @RealizesCon(ConTraceables.CON_013_EXTENSION_NEVER_CHANGES_A_TEST_VERDICT)
  private OpenTelemetry resolve() {
    try {
      return openTelemetry.get();
    } catch (Exception | LinkageError e) {
      logger.warn(
        "OpenTelemetry is configured but could not be initialized; test spans will not be " +
          "recorded. Test identity is unaffected - it travels as baggage and needs no SDK. Cause: {}",
        e.toString()
      );
      return OpenTelemetry.noop();
    }
  }
}
