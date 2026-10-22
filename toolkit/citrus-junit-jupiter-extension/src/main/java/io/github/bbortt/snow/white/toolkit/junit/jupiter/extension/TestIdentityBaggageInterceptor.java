/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */
package io.github.bbortt.snow.white.toolkit.junit.jupiter.extension;

import static io.github.bbortt.snow.white.toolkit.junit.jupiter.extension.SnowWhiteTestIdentity.BAGGAGE_HEADER;
import static io.github.bbortt.snow.white.toolkit.junit.jupiter.extension.SnowWhiteTestIdentity.currentBaggageHeader;
import static java.util.Objects.isNull;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Adds the running test's identity to an outgoing request as a W3C {@code baggage} header, so the
 * service under test can copy it onto its own server span.
 * <p>
 * This is the attaching half of the publishing architecture. Publishing a value and leaving the
 * attaching to whoever owns the client reads like clean separation and is really the same defect a
 * hand-written header has: with no component owning the step, every stage works and nothing happens
 * end to end.
 * <p>
 * It is expressed at Spring's {@link ClientHttpRequestInterceptor}, which is why one small class
 * reaches every application test in this repository. Citrus's {@code HttpEndpointConfiguration}
 * takes a {@code List<ClientHttpRequestInterceptor>} and applies it to the {@code RestTemplate} it
 * builds, so a Citrus endpoint and a plain {@code RestTemplate} are wired identically, and
 * Citrus's OpenAPI-generated actions ride the same endpoint and need nothing of their own. Nothing
 * here names Citrus.
 * <p>
 * Wire it once where the client is constructed:
 * <pre>{@code
 * var endpointConfiguration = new HttpEndpointConfiguration();
 * endpointConfiguration.setClientInterceptors(List.of(new TestIdentityBaggageInterceptor()));
 * }</pre>
 * It adds nothing when no test identity is current, so it is safe to wire unconditionally -
 * including on a client used outside a test.
 */
@NullMarked
@RealizesArch(ArchTraceables.ARCH_020_TEST_IDENTITY_PUBLISHED_NOT_PROPAGATED)
public class TestIdentityBaggageInterceptor
  implements ClientHttpRequestInterceptor
{

  @Override
  public ClientHttpResponse intercept(
    HttpRequest request,
    byte[] body,
    ClientHttpRequestExecution execution
  ) throws IOException {
    currentBaggageHeader().ifPresent(identity ->
      request.getHeaders().set(BAGGAGE_HEADER, merged(request, identity))
    );

    return execution.execute(request, body);
  }

  /**
   * Appends to a {@code baggage} header the request already carries rather than replacing it: the
   * caller's own baggage is not this interceptor's to discard, and W3C baggage is a comma-separated
   * list precisely so several concerns can share the header.
   */
  private static String merged(HttpRequest request, String testIdentity) {
    var existing = request.getHeaders().getFirst(BAGGAGE_HEADER);

    return isNull(existing) || existing.isBlank()
      ? testIdentity
      : existing + "," + testIdentity;
  }
}
