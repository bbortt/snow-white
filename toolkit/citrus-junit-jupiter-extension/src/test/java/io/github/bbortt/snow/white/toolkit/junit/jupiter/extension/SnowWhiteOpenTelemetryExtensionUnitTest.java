/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */
package io.github.bbortt.snow.white.toolkit.junit.jupiter.extension;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.WARN;
import static io.github.bbortt.snow.white.toolkit.junit.jupiter.extension.SnowWhiteTestIdentity.TEST_CASE_NAME;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesCon;
import clew.traceables.clew.annotation.VerifiesSw;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

class SnowWhiteOpenTelemetryExtensionUnitTest {

  @Test
  @VerifiesSw(SwTraceables.SW_047_ONE_CURRENT_SPAN_PER_TEST)
  void shouldRecordAPassingTestAsAnOkSpan() {
    execute(PassingFixture.class)
      .testEvents()
      .assertStatistics(stats -> stats.started(1).succeeded(1));

    var spans = PassingFixture.EXPORTER.getFinishedSpanItems();
    assertThat(spans).hasSize(1);

    var span = spans.getFirst();
    assertThat(span)
      .hasName(PassingFixture.class.getName() + "#shouldPass")
      .hasKind(INTERNAL)
      .hasStatusSatisfying(status -> status.hasCode(StatusCode.OK))
      .hasAttribute(
        AttributeKey.stringKey(TEST_CASE_NAME),
        PassingFixture.class.getName() + "#shouldPass"
      );
  }

  @Test
  @VerifiesSw(SwTraceables.SW_047_ONE_CURRENT_SPAN_PER_TEST)
  void shouldRecordAFailingTestAsAnErrorSpanWithItsException() {
    execute(FailingFixture.class)
      .testEvents()
      .assertStatistics(stats -> stats.started(1).failed(1));

    var spans = FailingFixture.EXPORTER.getFinishedSpanItems();
    assertThat(spans).hasSize(1);

    assertThat(spans.getFirst())
      .hasStatusSatisfying(status -> status.hasCode(StatusCode.ERROR))
      .hasException(new IllegalStateException("boom"));
  }

  @Test
  @VerifiesSw(SwTraceables.SW_047_ONE_CURRENT_SPAN_PER_TEST)
  void shouldRecordAnAbortedTestAsAnUnsetSpan() {
    execute(AbortedFixture.class)
      .testEvents()
      .assertStatistics(stats -> stats.started(1).aborted(1));

    var spans = AbortedFixture.EXPORTER.getFinishedSpanItems();
    assertThat(spans).hasSize(1);

    assertThat(spans.getFirst()).hasStatusSatisfying(status ->
      status.hasCode(StatusCode.UNSET)
    );
  }

  @Test
  @VerifiesSw(SwTraceables.SW_047_ONE_CURRENT_SPAN_PER_TEST)
  void shouldMakeTheTestSpanCurrentForNestedWork() {
    execute(ChildSpanFixture.class)
      .testEvents()
      .assertStatistics(stats -> stats.started(1).succeeded(1));

    var spans = ChildSpanFixture.EXPORTER.getFinishedSpanItems();
    assertThat(spans).hasSize(2);

    var testSpan = spans
      .stream()
      .filter(span ->
        span
          .getName()
          .equals(
            ChildSpanFixture.class.getName() + "#shouldNestUnderTheTestSpan"
          )
      )
      .findFirst()
      .orElseThrow();
    var childSpan = spans
      .stream()
      .filter(span -> span.getName().equals("child-span"))
      .findFirst()
      .orElseThrow();

    assertThat(childSpan).hasParentSpanId(testSpan.getSpanId());
  }

  @Test
  @VerifiesSw(SwTraceables.SW_046_TEST_CASE_NAME_IS_THE_QUALIFIED_INVOCATION)
  void shouldRecordADistinctSpanPerInvocation() {
    execute(ParameterizedFixture.class)
      .testEvents()
      .assertStatistics(stats -> stats.started(2).succeeded(2));

    var spans = ParameterizedFixture.EXPORTER.getFinishedSpanItems();

    assertThat(spans)
      .extracting(SpanData::getName)
      .containsExactlyInAnyOrder(
        ParameterizedFixture.class.getName() + "#aParameterizedTest[1]",
        ParameterizedFixture.class.getName() + "#aParameterizedTest[2]"
      );
  }

  @Test
  @VerifiesSw(SwTraceables.SW_047_ONE_CURRENT_SPAN_PER_TEST)
  void shouldKeepEachConcurrentTestsSpanIndependent() {
    var parallelConfig = Map.of(
      "junit.jupiter.execution.parallel.enabled",
      "true",
      "junit.jupiter.execution.parallel.config.strategy",
      "fixed",
      "junit.jupiter.execution.parallel.config.fixed.parallelism",
      "4"
    );

    execute(ConcurrentFixture.class, parallelConfig)
      .testEvents()
      .assertStatistics(stats -> stats.started(8).succeeded(8));

    var spans = ConcurrentFixture.EXPORTER.getFinishedSpanItems();
    assertThat(spans).hasSize(8);
    assertThat(spans).extracting(SpanData::getName).doesNotHaveDuplicates();
    assertThat(spans).allSatisfy(span ->
      assertThat(span).hasAttribute(
        AttributeKey.stringKey(TEST_CASE_NAME),
        span.getName()
      )
    );
  }

  @Test
  @VerifiesCon(ConTraceables.CON_013_EXTENSION_NEVER_CHANGES_A_TEST_VERDICT)
  void shouldNeverFailATestWhenRecordingItsSpanThrows() {
    try (var log = CapturedLog.of(SnowWhiteOpenTelemetryExtension.class)) {
      execute(ThrowingTracerFixture.class)
        .testEvents()
        .assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));

      assertThat(log.eventsAt(WARN)).hasSize(1);
      assertThat(log.eventsAt(WARN).getFirst().getFormattedMessage()).contains(
        "start a test span"
      );
    }
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class WithoutATestIdentityTest {

    @Mock
    private ExtensionContext context;

    @Mock
    private ExtensionContext.Store store;

    @Test
    @VerifiesSw(SwTraceables.SW_047_ONE_CURRENT_SPAN_PER_TEST)
    void shouldRecordNoSpanWhenNoIdentityCanBeDerived() {
      when(context.getTestClass()).thenReturn(Optional.empty());
      when(context.getTestMethod()).thenReturn(Optional.empty());
      when(context.getUniqueId()).thenReturn("[engine:junit-jupiter]");
      when(context.getStore(any())).thenReturn(store);

      var exporter = InMemorySpanExporter.create();
      var extension = extensionExporting(exporter);

      try (var log = CapturedLog.of(SnowWhiteOpenTelemetryExtension.class)) {
        extension.beforeEach(context);
        extension.afterEach(context);

        assertThat(log.eventsAt(DEBUG)).hasSize(1);
      }

      assertThat(exporter.getFinishedSpanItems()).isEmpty();
    }
  }

  static class PassingFixture {

    static final InMemorySpanExporter EXPORTER = InMemorySpanExporter.create();

    @RegisterExtension
    static final SnowWhiteOpenTelemetryExtension extension = extensionExporting(
      EXPORTER
    );

    @Test
    void shouldPass() {
      assertThat(SnowWhiteTestIdentity.current()).contains(
        PassingFixture.class.getName() + "#shouldPass"
      );
    }
  }

  static class FailingFixture {

    static final InMemorySpanExporter EXPORTER = InMemorySpanExporter.create();

    @RegisterExtension
    static final SnowWhiteOpenTelemetryExtension extension = extensionExporting(
      EXPORTER
    );

    @Test
    void shouldFail() {
      assertThat(SnowWhiteTestIdentity.current()).contains(
        FailingFixture.class.getName() + "#shouldFail"
      );

      throw new IllegalStateException("boom");
    }
  }

  static class AbortedFixture {

    static final InMemorySpanExporter EXPORTER = InMemorySpanExporter.create();

    @RegisterExtension
    static final SnowWhiteOpenTelemetryExtension extension = extensionExporting(
      EXPORTER
    );

    @Test
    void shouldAbort() {
      assertThat(SnowWhiteTestIdentity.current()).contains(
        AbortedFixture.class.getName() + "#shouldAbort"
      );

      Assumptions.assumeTrue(false, "deliberately aborted");
    }
  }

  static class ChildSpanFixture {

    static final InMemorySpanExporter EXPORTER = InMemorySpanExporter.create();
    static final OpenTelemetry OTEL = sdkExporting(EXPORTER);

    @RegisterExtension
    static final SnowWhiteOpenTelemetryExtension extension =
      new SnowWhiteOpenTelemetryExtension(new TestTracer(() -> OTEL));

    @Test
    void shouldNestUnderTheTestSpan() {
      assertThat(Span.current().isRecording()).isTrue();

      OTEL.getTracer("child").spanBuilder("child-span").startSpan().end();
    }
  }

  static class ParameterizedFixture {

    static final InMemorySpanExporter EXPORTER = InMemorySpanExporter.create();

    @RegisterExtension
    static final SnowWhiteOpenTelemetryExtension extension = extensionExporting(
      EXPORTER
    );

    @ParameterizedTest
    @ValueSource(strings = { "a", "b" })
    void aParameterizedTest(String value) {
      assertThat(value).isIn("a", "b");
      assertThat(SnowWhiteTestIdentity.current())
        .get()
        .asString()
        .startsWith(
          ParameterizedFixture.class.getName() + "#aParameterizedTest["
        );
    }
  }

  @Execution(CONCURRENT)
  static class ConcurrentFixture {

    static final InMemorySpanExporter EXPORTER = InMemorySpanExporter.create();

    @RegisterExtension
    static final SnowWhiteOpenTelemetryExtension extension = extensionExporting(
      EXPORTER
    );

    @RepeatedTest(8)
    void aRepeatedTest() {
      assertThat(SnowWhiteTestIdentity.current())
        .get()
        .asString()
        .startsWith(ConcurrentFixture.class.getName() + "#aRepeatedTest[");
    }
  }

  static class ThrowingTracerFixture {

    @RegisterExtension
    static final SnowWhiteOpenTelemetryExtension extension =
      new SnowWhiteOpenTelemetryExtension(
        new TestTracer(SnowWhiteOpenTelemetryExtensionUnitTest::brokenTracer)
      );

    @Test
    void shouldStillPass() {
      assertThat(SnowWhiteTestIdentity.current()).isEmpty();
    }
  }

  private static EngineExecutionResults execute(Class<?> testClass) {
    return execute(testClass, Map.of());
  }

  private static EngineExecutionResults execute(
    Class<?> testClass,
    Map<String, String> configurationParameters
  ) {
    return EngineTestKit.engine("junit-jupiter")
      .selectors(selectClass(testClass))
      .configurationParameters(configurationParameters)
      .execute();
  }

  private static SnowWhiteOpenTelemetryExtension extensionExporting(
    InMemorySpanExporter exporter
  ) {
    return new SnowWhiteOpenTelemetryExtension(
      new TestTracer(() -> sdkExporting(exporter))
    );
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

  private static OpenTelemetry brokenTracer() {
    var tracer = mock(Tracer.class);
    when(tracer.spanBuilder(any())).thenThrow(new RuntimeException("boom"));

    var openTelemetry = mock(OpenTelemetry.class);
    when(openTelemetry.getTracer(any(), any())).thenReturn(tracer);

    return openTelemetry;
  }
}
