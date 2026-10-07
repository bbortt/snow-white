/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */
package io.github.bbortt.snow.white.toolkit.junit.jupiter.extension;

import static io.github.bbortt.snow.white.toolkit.junit.jupiter.extension.SnowWhiteTestIdentity.*;
import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.context.Context;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SnowWhiteTestIdentityUnitTest {

  private static final String AN_IDENTITY =
    "com.example.ExampleIT#shouldDoThing";

  /**
   * Runs the supplier with a baggage entry current, which is the only state these accessors read.
   * The scope is closed on the same thread that opened it, as an OpenTelemetry scope requires.
   */
  private static <T> T withTestCaseNameInBaggage(
    String value,
    Supplier<T> action
  ) {
    var baggage = Baggage.builder().put(TEST_CASE_NAME, value).build();

    try (var _ = baggage.storeInContext(Context.current()).makeCurrent()) {
      return action.get();
    }
  }

  @Nested
  class CurrentTest {

    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_020_TEST_IDENTITY_PUBLISHED_NOT_PROPAGATED
    )
    void shouldReadTheIdentityFromCurrentBaggage() {
      assertThat(
        withTestCaseNameInBaggage(AN_IDENTITY, SnowWhiteTestIdentity::current)
      ).contains(AN_IDENTITY);
    }

    /**
     * "Derived, not stored" stated observably: nothing set this entry but the test itself, with no
     * extension registered, and the accessor serves it identically.
     */
    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_020_TEST_IDENTITY_PUBLISHED_NOT_PROPAGATED
    )
    void shouldReadAnIdentityACallerSetItselfWithoutTheExtension() {
      var fromACallerThatNeverSawTheExtension = withTestCaseNameInBaggage(
        "set-by-hand",
        SnowWhiteTestIdentity::current
      );

      assertThat(fromACallerThatNeverSawTheExtension).contains("set-by-hand");
    }

    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_020_TEST_IDENTITY_PUBLISHED_NOT_PROPAGATED
    )
    void shouldBeEmptyWithoutAnyBaggageEntry() {
      assertThat(current()).isEmpty();
    }

    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_020_TEST_IDENTITY_PUBLISHED_NOT_PROPAGATED
    )
    void shouldBeEmptyOnceTheScopeHasClosed() {
      withTestCaseNameInBaggage(AN_IDENTITY, SnowWhiteTestIdentity::current);

      assertThat(current()).isEmpty();
    }

    /**
     * Blank is absent, matching how the reading side already treats it, so a suite that names a
     * test badly produces the same already-specified outcome as one that names none.
     */
    @ParameterizedTest
    @ValueSource(strings = { "", " ", "\t", "\n", "   " })
    @VerifiesArch(
      ArchTraceables.ARCH_020_TEST_IDENTITY_PUBLISHED_NOT_PROPAGATED
    )
    void shouldTreatABlankIdentityAsAbsent(String blank) {
      assertThat(
        withTestCaseNameInBaggage(blank, SnowWhiteTestIdentity::current)
      ).isEmpty();
    }
  }

  @Nested
  class CurrentBaggageHeaderTest {

    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_020_TEST_IDENTITY_PUBLISHED_NOT_PROPAGATED
    )
    void shouldBeEmptyWithoutAnIdentity() {
      assertThat(currentBaggageHeader()).isEmpty();
    }

    /**
     * The two channels must describe one identity. Asserted through a decode rather than a literal
     * match, because the propagator percent-encodes {@code #} to {@code %23} - the value is the
     * same, the spelling on the wire is not.
     */
    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_020_TEST_IDENTITY_PUBLISHED_NOT_PROPAGATED
    )
    void shouldDescribeTheSameIdentityTheValueAccessorDoes() {
      var identity = withTestCaseNameInBaggage(
        AN_IDENTITY,
        SnowWhiteTestIdentity::current
      );
      var header = withTestCaseNameInBaggage(
        AN_IDENTITY,
        SnowWhiteTestIdentity::currentBaggageHeader
      );

      assertThat(header)
        .get()
        .asString()
        .startsWith(TEST_CASE_NAME + "=");
      assertThat(
        extractBaggage(header.orElseThrow()).getEntryValue(TEST_CASE_NAME)
      ).isEqualTo(identity.orElseThrow());
    }

    /**
     * The fragment has to parse as a W3C baggage member, so it is round-tripped through
     * OpenTelemetry's own propagator rather than merely string-matched: that is what proves the
     * receiving end would read it.
     */
    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_020_TEST_IDENTITY_PUBLISHED_NOT_PROPAGATED
    )
    void shouldProduceAHeaderThatParsesBackToTheSameSingleEntry() {
      var header = withTestCaseNameInBaggage(
        AN_IDENTITY,
        SnowWhiteTestIdentity::currentBaggageHeader
      ).orElseThrow();

      var extracted = extractBaggage(header);

      assertThat(extracted.getEntryValue(TEST_CASE_NAME)).isEqualTo(
        AN_IDENTITY
      );
      assertThat(extracted.size()).isOne();
    }

    /**
     * A non-ASCII class name is why encoding is delegated upstream instead of hand-rolled: such
     * characters are outside the W3C baggage-octet range, and an unescaped header would be dropped
     * by the receiving end rather than rejected loudly.
     */
    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_020_TEST_IDENTITY_PUBLISHED_NOT_PROPAGATED
    )
    void shouldEncodeAnIdentityThatIsNotPlainAscii() {
      var nonAscii = "com.example.中文IT#shouldDoThing";

      var header = withTestCaseNameInBaggage(
        nonAscii,
        SnowWhiteTestIdentity::currentBaggageHeader
      ).orElseThrow();

      assertThat(header).isEqualTo(
        TEST_CASE_NAME + "=com.example.%E4%B8%AD%E6%96%87IT%23shouldDoThing"
      );
      assertThat(
        extractBaggage(header).getEntryValue(TEST_CASE_NAME)
      ).isEqualTo(nonAscii);
    }

    /**
     * Only the test identity travels, never whatever else the ambient context happens to carry -
     * that is a property of the header being built from a baggage of one entry.
     */
    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_020_TEST_IDENTITY_PUBLISHED_NOT_PROPAGATED
    )
    void shouldNotLeakUnrelatedBaggageIntoTheHeader() {
      var baggage = Baggage.builder()
        .put(TEST_CASE_NAME, AN_IDENTITY)
        .put("team", "platform")
        .build();

      Optional<String> header;
      try (var _ = baggage.storeInContext(Context.current()).makeCurrent()) {
        header = currentBaggageHeader();
      }

      assertThat(header).get().asString().doesNotContain("team", "platform");
    }

    private static Baggage extractBaggage(String header) {
      return Baggage.fromContext(
        W3CBaggagePropagator.getInstance().extract(
          Context.root(),
          new HashMap<>(Map.of(SnowWhiteTestIdentity.BAGGAGE_HEADER, header)),
          new MapGetter()
        )
      );
    }
  }
}
