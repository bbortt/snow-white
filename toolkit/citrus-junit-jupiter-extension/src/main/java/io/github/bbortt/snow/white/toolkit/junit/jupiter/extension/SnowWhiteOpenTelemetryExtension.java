/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */
package io.github.bbortt.snow.white.toolkit.junit.jupiter.extension;

import static io.github.bbortt.snow.white.toolkit.junit.jupiter.extension.SnowWhiteTestIdentity.TEST_CASE_NAME;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.StatusCode.*;
import static java.util.Objects.isNull;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.create;
import static org.slf4j.LoggerFactory.getLogger;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesCon;
import clew.traceables.clew.annotation.RealizesSw;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.opentest4j.TestAbortedException;
import org.slf4j.Logger;

/**
 * Names the test behind every request a JUnit-driven suite makes, and records one span per test.
 * <p>
 * For the duration of a test, the test's identity is an entry in the current OpenTelemetry
 * baggage, where an instrumented in-process caller picks it up for free and
 * {@link SnowWhiteTestIdentity} reads it for a caller that is not instrumented. The same test also
 * gets one span, current for its duration, carrying its identity and its verdict.
 * <p>
 * <strong>It never changes a test's verdict.</strong> It is registered for every
 * test in a module - through {@code junit.jupiter.extensions.autodetection.enabled} or
 * {@code @ExtendWith} - including the many that have no OpenTelemetry anywhere, so an absent SDK,
 * an unconfigured exporter, an unreachable collector or a bug in this class costs telemetry and
 * nothing else.
 * <p>
 * Per-test state lives in the {@link ExtensionContext.Store} JUnit already scopes to the test,
 * never in a field or a {@code ThreadLocal} of this class. That is what makes two tests in flight
 * on two threads unable to see each other's span, and what makes this class unit-testable at all.
 * <p>
 * The verdict is read from {@link ExtensionContext#getExecutionException()} in
 * {@link #afterEach(ExtensionContext)} rather than through {@code TestWatcher}. {@code TestWatcher}
 * reports only {@code @Test} and {@code @TestTemplate} methods, is documented as running after the
 * context's closeable resources have been closed, and promises nothing about which thread it runs
 * on - which matters, because an OpenTelemetry {@link Scope} must be closed on the thread that
 * opened it.
 */
@NullMarked
@RealizesSw(SwTraceables.SW_047_ONE_CURRENT_SPAN_PER_TEST)
public class SnowWhiteOpenTelemetryExtension
  implements BeforeEachCallback, AfterEachCallback
{

  private static final Logger logger = getLogger(
    SnowWhiteOpenTelemetryExtension.class
  );

  private static final Namespace NAMESPACE = create(
    SnowWhiteOpenTelemetryExtension.class
  );
  private static final String TEST_SPAN_KEY = "testSpan";

  private final TestTracer tracer;

  public SnowWhiteOpenTelemetryExtension() {
    this(new TestTracer());
  }

  SnowWhiteOpenTelemetryExtension(TestTracer tracer) {
    this.tracer = tracer;
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    withoutFailingTheTest(
      () -> start(context),
      "start a test span for " + context.getUniqueId()
    );
  }

  @Override
  public void afterEach(ExtensionContext context) {
    withoutFailingTheTest(
      () -> finish(context),
      "finish the test span for " + context.getUniqueId()
    );
  }

  private void start(ExtensionContext context) {
    var testCaseName = TestCaseName.from(context);

    if (testCaseName.isEmpty()) {
      logger.debug(
        "No test identity could be derived for '{}'; recording no test span.",
        context.getUniqueId()
      );
      return;
    }

    context
      .getStore(NAMESPACE)
      .put(TEST_SPAN_KEY, startTestSpan(testCaseName.get()));
  }

  /**
   * Opens one {@link Scope} carrying both the span and the baggage entry, so there is a single
   * close to pair with it, and makes the span current - which is the whole point of creating it. A
   * span that is never current parents nothing, so every request the test issues would open its own
   * root trace and the span would be one orphan per test.
   */
  private TestSpan startTestSpan(String testCaseName) {
    var span = tracer
      .get()
      .spanBuilder(testCaseName)
      .setSpanKind(INTERNAL)
      .setAttribute(TEST_CASE_NAME, testCaseName)
      .startSpan();

    var scope = Context.current()
      .with(span)
      .with(
        Baggage.current().toBuilder().put(TEST_CASE_NAME, testCaseName).build()
      )
      .makeCurrent();

    return new TestSpan(span, scope);
  }

  /**
   * Closes the scope and ends the span, the second in a {@code finally} so that a failure to
   * release the context never leaks a span that is already past its test.
   */
  private void finish(ExtensionContext context) {
    var testSpan = context
      .getStore(NAMESPACE)
      .remove(TEST_SPAN_KEY, TestSpan.class);

    if (isNull(testSpan)) {
      return;
    }

    try {
      testSpan.scope().close();
    } finally {
      end(testSpan.span(), context.getExecutionException());
    }
  }

  /**
   * An aborted test is {@code UNSET}, not {@code ERROR}: a failed assumption means the test
   * declined to judge, and recording that as an error would make the span claim the service
   * misbehaved when the suite merely skipped the case.
   */
  private static void end(Span span, Optional<Throwable> executionException) {
    executionException.ifPresentOrElse(
      cause -> {
        if (cause instanceof TestAbortedException) {
          span.setStatus(UNSET);
        } else {
          span.setStatus(ERROR, cause.toString()).recordException(cause);
        }
      },
      () -> span.setStatus(OK)
    );

    span.end();
  }

  /**
   * {@code Exception} and {@code LinkageError} cover a misconfiguration and an absent optional
   * dependency; an {@code Error} that is not a {@code LinkageError} is not this class's business
   * and propagates.
   */
  @RealizesCon(ConTraceables.CON_013_EXTENSION_NEVER_CHANGES_A_TEST_VERDICT)
  private static void withoutFailingTheTest(
    Runnable action,
    String description
  ) {
    try {
      action.run();
    } catch (Exception | LinkageError e) {
      logger.warn("Failed to {}; continuing without it.", description, e);
    }
  }

  private record TestSpan(Span span, Scope scope) {}
}
