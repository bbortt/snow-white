/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */
package io.github.bbortt.snow.white.toolkit.junit.jupiter.extension;

import static ch.qos.logback.classic.Level.WARN;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.VerifiesCon;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class TestTracerUnitTest {

  @Test
  @VerifiesCon(ConTraceables.CON_013_EXTENSION_NEVER_CHANGES_A_TEST_VERDICT)
  void shouldReturnATracerFromTheSuppliedOpenTelemetry() {
    var exporter = InMemorySpanExporter.create();

    var tracer = new TestTracer(() -> sdkExporting(exporter)).get();
    tracer.spanBuilder("a-span").startSpan().end();

    assertThat(exporter.getFinishedSpanItems()).hasSize(1);
  }

  /**
   * The condition that actually broke: configuration naming an exporter whose implementation is
   * absent. A {@code ConfigurationException} must become a no-op tracer, never a test outcome.
   */
  @Test
  @VerifiesCon(ConTraceables.CON_013_EXTENSION_NEVER_CHANGES_A_TEST_VERDICT)
  void shouldFallBackToANoOpTracerWhenAcquisitionThrows() {
    var testTracer = new TestTracer(() -> {
      throw new ConfigurationException(
        "otel.metrics.exporter set to \"otlp\" but opentelemetry-exporter-otlp not found"
      );
    });

    Tracer tracer = assertThatNoThrow(testTracer);

    assertThat(
      tracer.spanBuilder("a-span").startSpan().isRecording()
    ).isFalse();
  }

  /**
   * An absent optional dependency arrives as a {@code NoClassDefFoundError}, which is an
   * {@code Error} and would otherwise sail straight past a {@code catch (Exception)}.
   */
  @Test
  @VerifiesCon(ConTraceables.CON_013_EXTENSION_NEVER_CHANGES_A_TEST_VERDICT)
  void shouldFallBackToANoOpTracerWhenTheSdkIsNotOnTheClasspath() {
    var testTracer = new TestTracer(() -> {
      throw new NoClassDefFoundError(
        "io/opentelemetry/sdk/autoconfigure/AutoConfiguredOpenTelemetrySdk"
      );
    });

    assertThat(
      assertThatNoThrow(testTracer)
        .spanBuilder("a-span")
        .startSpan()
        .isRecording()
    ).isFalse();
  }

  /**
   * An {@code Error} that is not a {@code LinkageError} is not this class's business. Swallowing an
   * {@code OutOfMemoryError} to protect a test's verdict would be the wrong trade.
   */
  @Test
  @VerifiesCon(ConTraceables.CON_013_EXTENSION_NEVER_CHANGES_A_TEST_VERDICT)
  void shouldNotSwallowAnUnrecoverableError() {
    var testTracer = new TestTracer(() -> {
      throw new OutOfMemoryError("not ours to absorb");
    });

    assertThatCode(testTracer::get).isInstanceOf(OutOfMemoryError.class);
  }

  @Test
  @VerifiesCon(ConTraceables.CON_013_EXTENSION_NEVER_CHANGES_A_TEST_VERDICT)
  void shouldReportAFailedAcquisitionOnceAtWarn() {
    try (var log = CapturedLog.of(TestTracer.class)) {
      var testTracer = new TestTracer(() -> {
        throw new ConfigurationException("broken");
      });

      testTracer.get();
      testTracer.get();
      testTracer.get();

      assertThat(log.eventsAt(WARN)).hasSize(1);
      assertThat(log.eventsAt(WARN).getFirst().getFormattedMessage()).contains(
        "could not be initialized",
        "broken"
      );
    }
  }

  @Test
  @VerifiesCon(ConTraceables.CON_013_EXTENSION_NEVER_CHANGES_A_TEST_VERDICT)
  void shouldAcquireAtMostOnceAcrossManyCalls() {
    var attempts = new AtomicInteger();
    var testTracer = new TestTracer(counting(attempts, OpenTelemetry::noop));

    testTracer.get();
    testTracer.get();
    testTracer.get();

    assertThat(attempts).hasValue(1);
  }

  /** A failure is memoized too, so it is never retried per test. */
  @Test
  @VerifiesCon(ConTraceables.CON_013_EXTENSION_NEVER_CHANGES_A_TEST_VERDICT)
  void shouldNeverRetryAfterAFailedAcquisition() {
    var attempts = new AtomicInteger();
    var testTracer = new TestTracer(
      counting(attempts, () -> {
        throw new ConfigurationException("broken");
      })
    );

    testTracer.get();
    testTracer.get();

    assertThat(attempts).hasValue(1);
  }

  /**
   * The one-attempt guarantee has to be a property of this code rather than of tests happening to
   * run sequentially, because Surefire is configured for parallel execution in this repository.
   */
  @Test
  @VerifiesCon(ConTraceables.CON_013_EXTENSION_NEVER_CHANGES_A_TEST_VERDICT)
  void shouldAcquireOnceWhenManyThreadsArriveAtTheSameTime()
    throws InterruptedException {
    var threads = 16;
    var attempts = new AtomicInteger();
    var testTracer = new TestTracer(counting(attempts, OpenTelemetry::noop));

    var startLine = new CountDownLatch(1);
    var finished = new CountDownLatch(threads);

    try (var executor = Executors.newFixedThreadPool(threads)) {
      for (var i = 0; i < threads; i++) {
        executor.execute(() -> {
          try {
            startLine.await();
            testTracer.get();
          } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
          } finally {
            finished.countDown();
          }
        });
      }

      startLine.countDown();
      assertThat(finished.await(10, SECONDS)).isTrue();
    }

    assertThat(attempts).hasValue(1);
  }

  private static Tracer assertThatNoThrow(TestTracer testTracer) {
    try (var _ = CapturedLog.of(TestTracer.class)) {
      return testTracer.get();
    }
  }

  private static Supplier<OpenTelemetry> counting(
    AtomicInteger attempts,
    Supplier<OpenTelemetry> delegate
  ) {
    return () -> {
      attempts.incrementAndGet();
      return delegate.get();
    };
  }

  private static OpenTelemetry sdkExporting(InMemorySpanExporter exporter) {
    return OpenTelemetrySdk.builder()
      .setTracerProvider(
        SdkTracerProvider.builder()
          .addSpanProcessor(SimpleSpanProcessor.create(exporter))
          .build()
      )
      .build();
  }
}
