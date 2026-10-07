/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */
package io.github.bbortt.snow.white.toolkit.junit.jupiter.extension;

import static io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator.getInstance;
import static java.util.HashMap.newHashMap;
import static java.util.Objects.isNull;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Context;
import java.util.HashMap;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The identity of the test currently running, readable by anything that needs to name it.
 * <p>
 * This is the second of the two channels the extension publishes, and it is <em>derived</em>
 * from the first rather than stored beside it: the value is whatever {@value #TEST_CASE_NAME} the
 * current OpenTelemetry baggage holds. There is therefore one source of truth, the accessor is
 * correct on any thread that inherited the context, and a caller that sets the entry itself -
 * without {@link SnowWhiteOpenTelemetryExtension} registered at all - is served identically.
 * <p>
 * Nothing here needs an OpenTelemetry SDK. {@link Baggage} is part of {@code opentelemetry-api}, so
 * a suite gets named requests with no SDK, no exporter and no agent on its test classpath.
 *
 * @see TestIdentityBaggageInterceptor the interceptor that attaches this to an outgoing request
 */
@NullMarked
@RealizesArch(ArchTraceables.ARCH_020_TEST_IDENTITY_PUBLISHED_NOT_PROPAGATED)
public final class SnowWhiteTestIdentity {

  /**
   * The OpenTelemetry semantic-convention key, used both as the baggage entry and as the span
   * attribute. Snow-White adopts it rather than defining it - see {@code semantic-convention/test.md}.
   */
  public static final String TEST_CASE_NAME = "test.case.name";

  /**
   * The W3C header the identity travels in, and the one
   * {@code otel.java.experimental.span-attributes.copy-from-baggage.include} reads on the other
   * side.
   */
  public static final String BAGGAGE_HEADER = "baggage";

  private SnowWhiteTestIdentity() {
    // utility class
  }

  /**
   * The identity of the running test, or empty where there is none.
   * <p>
   * Blank is treated as absent, matching how the reading side already treats it: a suite that does
   * not name its tests and a suite that names one badly produce the same, already specified
   * outcome rather than two.
   */
  public static Optional<String> current() {
    return of(Baggage.current().getEntryValue(TEST_CASE_NAME));
  }

  /**
   * The identity of the running test as a ready-to-send {@value #BAGGAGE_HEADER} header value, or
   * empty where there is none.
   * <p>
   * Encoding is delegated to OpenTelemetry's own {@code W3CBaggagePropagator} rather than
   * hand-rolled, so a class named in a non-ASCII script travels correctly rather than producing a
   * header the receiving end silently drops.
   * <p>
   * That propagator is conservative: it percent-encodes {@code #} to {@code %23}, so the header
   * reads {@code test.case.name=com.example.ExampleIT%23shouldDoThing} even though a bare
   * {@code #} would also be legal. Do not "fix" this - the receiving propagator decodes it back,
   * and the span carries the unescaped name.
   */
  public static Optional<String> currentBaggageHeader() {
    return current().map(SnowWhiteTestIdentity::asBaggageHeader);
  }

  static Optional<String> of(@Nullable String identity) {
    return isNull(identity) || identity.isBlank()
      ? Optional.empty()
      : Optional.of(identity);
  }

  /**
   * Builds the header from a baggage holding only this entry, so the result is a single member and
   * never leaks whatever else the ambient context happens to carry.
   */
  private static String asBaggageHeader(String identity) {
    HashMap<String, String> carrier = newHashMap(1);

    getInstance().inject(
      Context.root().with(
        Baggage.builder().put(TEST_CASE_NAME, identity).build()
      ),
      carrier,
      HashMap::put
    );

    return carrier.get(BAGGAGE_HEADER);
  }
}
